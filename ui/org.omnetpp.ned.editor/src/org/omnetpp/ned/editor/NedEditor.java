/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditor;
import org.omnetpp.common.Debug;
import org.omnetpp.common.IConstants;
import org.omnetpp.common.util.DetailedPartInitException;
import org.omnetpp.common.util.DisplayUtils;
import org.omnetpp.common.util.UIUtils;
import org.omnetpp.ned.core.IGotoNedElement;
import org.omnetpp.ned.core.NedResourcesPlugin;
import org.omnetpp.ned.editor.graph.GraphicalNedEditor;
import org.omnetpp.ned.editor.text.TextualNedEditor;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.interfaces.INedModelProvider;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.ISubmoduleOrConnection;
import org.omnetpp.ned.model.notification.INedChangeListener;
import org.omnetpp.ned.model.notification.NedFileRemovedEvent;
import org.omnetpp.ned.model.notification.NedModelEvent;

/**
 * Multi-page NED editor.
 * MultiPageNedEditor binds the two separate NED-based editors together. Both the text and the graphical
 * editor maintain their own models independent of each other.
 * Saving is done by delegation to the text editor's save method (i.e. the graphical
 * editor itself cannot save its model, the multipage editor must first obtain
 * the model from the graphical editor, convert to text and pass it to the Text based editor and then
 * call the Text editor to save its content. When setting the input of the multipage editor both
 * embedded editor should be notified (i.e. setInput must be delegated)
 *
 * @author rhornig
 */
public class NedEditor
    extends MultiPageEditorPart
    implements IGotoNedElement, IGotoMarker, IShowInTargetList, IShowInSource
{
    public static final String ID = "org.omnetpp.ned.editor";

    // If a NED file is outside NED folders (or is excluded), we cannot edit it as NED,
    // so we'll just instantiate a barebones plain text editor instead.
    private boolean usePlainTextEditor;
    private TextEditor plainTextEditor;

    // the "normal" nested editors
    private GraphicalNedEditor graphicalEditor;
    private TextualNedEditor textEditor;
    private final ResourceTracker resourceListener = new ResourceTracker();

    // we should store the currentPage so we can figure out whether text or the graphical editor is the active one
    private int currentPageIndex;
    private int graphPageIndex;
    private int textPageIndex;

    private static Mode mostRecentlyUsedMode = Mode.GRAPHICAL;  // TEXT or GRAPHICAL

    protected IPartListener partListener = new IPartListener() {
        public void partOpened(IWorkbenchPart part) {
        }

        public void partClosed(IWorkbenchPart part) {
        }

        public void partActivated(IWorkbenchPart part) {
            Debug.println(part.toString()+" activated");
            if (part == NedEditor.this) {
                mostRecentlyUsedMode = (currentPageIndex == graphPageIndex) ? Mode.GRAPHICAL : Mode.TEXT;

                if (getEditorInput() != null && NedResourcesPlugin.getNedResources().containsNedFileElement(getFile())) {
                    // when switching from another MultiPageNedEditor to this one for the same file
                    // we need to immediately pull the changes, because editing in this editor
                    // can be done correctly only if it is synchronized with NEDResources
                    // synchronization is normally done in a delayed job and here we enforce to happen it right now
                    NedFileElementEx nedFileElement = getModel();
                    if (getActivePage() == textPageIndex && graphicalEditor.hasContentChanged() &&
                        !nedFileElement.isReadOnly() && !nedFileElement.hasSyntaxError())
                        textEditor.pullChangesFromNedResourcesWhenPending();
                }

                if (getControl(getActivePage()).isVisible())
                    graphicalEditor.refresh();
            }
        }

        public void partDeactivated(IWorkbenchPart part) {
            if (part == NedEditor.this) {
                // when switching from one MultiPageNedEditor to another for the same file
                // we need to immediately push the changes, because editing in the other editor
                // can be done correctly only if it is synchronized with the one just being deactivated
                // synchronization is normally done in a delayed job and here we enforce to happen it right now
                if (getActivePage() == textPageIndex &&
                        NedResourcesPlugin.getNedResources().containsNedFileElement(getFile()))
                    textEditor.pushChangesIntoNedResources();
            }
        }

        public void partBroughtToTop(IWorkbenchPart part) {
        }
    };

    protected INedChangeListener nedModelListener = new INedChangeListener() {
        public void modelChanged(NedModelEvent event) {
            if (event instanceof NedFileRemovedEvent && ((NedFileRemovedEvent)event).getFile().equals(getFile())) {
                // if file was deleted, close editor; or if it was moved out of NED folders or excluded, close this editor and reopen in plain text editor
                final boolean dirty = isDirty(); // this must be called before closeEditor
                // remember edited content -- if editor was dirty, we must put it back into the newly opened editor
                final String oldContent = getTextEditor().getText();
                final IFile file = getFile();
                // the problem is that workspace changes don't happen in the UI thread
                // so we switch to it and call close from there
                DisplayUtils.runNowOrAsyncInUIThread(() -> {
                    closeEditor(false);
                    if (file.isAccessible() && dirty) {
                        IWorkbench workbench = PlatformUI.getWorkbench();
                        IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
                        IWorkbenchPage page = workbenchWindow.getActivePage();
                        try {
                            ITextEditor editor = (ITextEditor)IDE.openEditor(page, file, EditorsUI.DEFAULT_TEXT_EDITOR_ID);  //TODO use new instance of NedEditor instead (see code in setInput())
                            editor.getDocumentProvider().getDocument(editor.getEditorInput()).set(oldContent);
                        } catch (PartInitException e) {
                            NedEditorPlugin.logError(e);
                        }
                    }
                });
            }
        }
    };

    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput))
            throw new DetailedPartInitException("Invalid input, it must be a file in the workspace: " + editorInput.getName(),
                "Please make sure the project is open before trying to open a file in it.");

        IFile file = ((FileEditorInput)editorInput).getFile();
        if (!file.exists()) {
            IStatus status = new Status(IStatus.WARNING, NedEditorPlugin.PLUGIN_ID, 0, "File "+file.getFullPath()+" does not exist", null);
            throw new PartInitException(status);
        }

        // To properly open it, file must be inside a NED source folder, and must not be in an excluded package
        usePlainTextEditor = !NedResourcesPlugin.getNedResources().isNedFile(file);

        super.init(site, editorInput);  // note: involves setInput()

        if (!usePlainTextEditor) {
            ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);
            NedResourcesPlugin.getNedResources().addNedModelChangeListener(nedModelListener);
            getSite().getPage().addPartListener(partListener);
        }
    }

    @Override
    public void dispose() {
        if (usePlainTextEditor) {
            super.dispose();
            return;
        }

        // detach the editor file from the core plugin and do not set a new file
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
        if (getSite() != null && getSite().getPage() != null)
            getSite().getPage().removePartListener(partListener);
        NedResourcesPlugin.getNedResources().removeNedModelChangeListener(nedModelListener);

        // super must be called before disconnect to let the child editors remove their listeners
        super.dispose();

        // disconnect the editor from the ned resources plugin
        if (getEditorInput() != null)
            NedResourcesPlugin.getNedResources().disconnect(getFile());
    }

    @Override
    protected void setInput(IEditorInput newInput) {
        Assert.isNotNull(newInput, "input should not be null");

        IEditorInput oldInput = getEditorInput();
        if (ObjectUtils.equals(oldInput, newInput))
            return; // no change

        if (usePlainTextEditor) {
            if (plainTextEditor != null)
                plainTextEditor.setInput(newInput);
            super.setInput(newInput);
            setPartName(getFile().getName());
            if (NedResourcesPlugin.getNedResources().isNedFile(getFile())) {
                // File is now part of a NED source folder, re-open editor for full NED editing capabilities.
                IWorkbenchPage page = getSite().getPage();
                Display.getCurrent().asyncExec(() -> {
                    UIUtils.reopenEditor(page, newInput, (IFileEditorInput)newInput, false);
                });
                return;
            }
            return;
        }

        // new file must STILL be under a NED folder, as ensured by doSaveAs()+TextualNedEditor.doSetInput()
        IFile newFile = ((IFileEditorInput) newInput).getFile();
        Assert.isTrue(NedResourcesPlugin.getNedResources().isNedFile(newFile));

        // disconnect from the old file (if there was any)
        if (oldInput != null)
            NedResourcesPlugin.getNedResources().disconnect(getFile());

        // check if the given file is in sync with the filesystem. If not
        // synchronize it otherwise the text editor loads only an empty file
        if (!newFile.isSynchronized(IResource.DEPTH_ZERO))
            try {
                newFile.refreshLocal(IResource.DEPTH_ZERO, null);
            } catch (CoreException e) {
                NedEditorPlugin.logError("Cannot refresh file", e);
            }

        // connect() must take place *before* setInput()
        NedResourcesPlugin.getNedResources().connect(newFile);

        // set the new input
        super.setInput(newInput);
        if (graphicalEditor != null)
            graphicalEditor.setInput(newInput);
        if (textEditor != null)
            textEditor.setInput(newInput);

        setPartName(getFile().getName());
    }

    @Override
    protected void createPages() {
        try {
            if (usePlainTextEditor) {
                plainTextEditor = new TextEditor();
                addPage(plainTextEditor, getEditorInput());
                setPageText(0, "Source");
                return;
            }

            graphicalEditor = new GraphicalNedEditor();
            textEditor = new TextualNedEditor();

            // setup graphical editor
            graphPageIndex = addPage(graphicalEditor, getEditorInput());
            graphicalEditor.markContent();
            setPageText(graphPageIndex, "Design");

            // setup text editor
            // we don't have to set the content because it's set
            // automatically by the text editor (from the FileEditorInput)
            textPageIndex = addPage(textEditor, getEditorInput());
            setPageText(textPageIndex,"Source");

            // switch to graphics mode initially if there's no error in the file
            setActivePage(maySwitchToGraphicalEditor() && mostRecentlyUsedMode==Mode.GRAPHICAL ? graphPageIndex : textPageIndex);

        }
        catch (PartInitException e) {
            NedEditorPlugin.logError(e);
        }

    }

    /**
     * Returns true if the editor is allowed to switch to the graphical editor page.
     * By default, the criteria is that there should be no parse error or basic validation error.
     * (Consistency errors are allowed).
     */
    protected boolean maySwitchToGraphicalEditor() {
        Assert.isTrue(!usePlainTextEditor);
        return getModel().getSyntaxProblemMaxCumulatedSeverity() < IMarker.SEVERITY_ERROR;
    }

    @Override
    protected void setActivePage(int pageIndex) {
        // check if there was a change at all (this prevents possible recursive calling)
        if (pageIndex == getActivePage())
            return;

        super.setActivePage(pageIndex);
        // store the current active page. we should not rely on getActivePage because it accesses
        // SWT widgets so it cannot be called from NON GUI threads.
        currentPageIndex = pageIndex;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorPart#pageChange(int)
     * Responsible of synchronizing the two editor's model with each other and the NEDResources plugin
     */
    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        if (usePlainTextEditor)
            return;

        mostRecentlyUsedMode = (newPageIndex == graphPageIndex) ? Mode.GRAPHICAL : Mode.TEXT;

        // XXX Kludge: currently the workbench do not send a partActivated/deactivated messages
        // for embedded editors in a MultiPageEditor. (This is a missing unimplemented feature,
        // it works with MultiEditor though).
        // To make the NED editor Outline page active, we need to send activate/deactivate directly.
        // We find the Outline View directly, and send the notification by hand.
        // Once the platform MultiPageEditor class handles this correctly, this code can be removed.
        // On each page change we emulate a close/open cycle of the multi-page editor, this removes
        // the associated Outline page, so the Outline View will re-request the multi-page editor
        // for a ContentOutlinePage (via getAdapter). The current implementation of
        // MultipageEditorPart.getAdapter delegates this request to the active embedded editor.
        //
//        ContentOutline contentOutline = (ContentOutline)getEditorSite().getPage().findView(IPageLayout.ID_OUTLINE);
//        if (contentOutline != null) {
//            // notify from the old closed editor
//          // TODO: after switching to text page and back to graphical page the dragging will be broken in the outline view
//          //       look at somewhere near hookControl() and refreshDragSourceAdapter() in AbstractEditPartView
//          //       somehow the getDragSource() method returns a non null value and listeners are not correctly hooked up
//          //       and that's why dragging will not work AFAICT now. KLUDGE: this has been worked around in NedOutlinePage
//            contentOutline.partClosed(this);
//            contentOutline.partActivated(this);
//        }
        // end of kludge

        // switch from graphics to text:
        if (newPageIndex == textPageIndex) {
            // generate text representation from the model NOW
            NedFileElementEx nedFileElement = getModel();
            if (graphicalEditor.hasContentChanged() && !nedFileElement.isReadOnly() && !nedFileElement.hasSyntaxError()) {
                textEditor.pullChangesFromNedResourcesWhenPending();
                // NOTE: the following line fixes http://dev.omnetpp.org/bugs/view.php?id=235
                // we have to update the line information of the tree to be able to put the error markers on the right lines
                // this requires to parse the text right after the tree changes has been applied to it
                // luckily the tree will be only modified where it needs to thanks to the clever tree differencer
                // still it will end up doing another round of pushing changes from the tree towards text but now without action... no infinite loop
                textEditor.pushChangesIntoNedResources();
            }

            // keep the current selection between the two editors
            INedElement currentNEDElementSelection = null;
            Object object = ((IStructuredSelection)graphicalEditor.getSite()
                    .getSelectionProvider().getSelection()).getFirstElement();
            if (object != null)
                currentNEDElementSelection = ((INedModelProvider)object).getModel();

            if (currentNEDElementSelection != null)
                showInEditor(currentNEDElementSelection, Mode.TEXT);
        }
        else if (newPageIndex == graphPageIndex) {
            textEditor.pushChangesIntoNedResources();
            graphicalEditor.markContent();
            // earlier ned changes may not caused a refresh (because of optimizations)
            // in the graphical editor (ie. the editor was not visible) so we must do it now
            graphicalEditor.refresh();

            // keep the current selection between the two editors
            INedElement currentNEDElementSelection = null;
            if (textEditor.getSite().getSelectionProvider().getSelection() instanceof IStructuredSelection) {
                Object object = ((IStructuredSelection)textEditor.getSite()
                        .getSelectionProvider().getSelection()).getFirstElement();
                if (object != null)
                    currentNEDElementSelection = ((INedModelProvider)object).getModel();
            }
            if (currentNEDElementSelection!=null)
                showInEditor(currentNEDElementSelection, Mode.GRAPHICAL);
        }
        else
            throw new RuntimeException("Unknown page index");

        currentPageIndex = newPageIndex;
    }

    @Override
    public boolean isDirty() {
        if (usePlainTextEditor)
            return plainTextEditor.isDirty();

        // The default behavior is wrong when undoing changes in both editors.
        // This way at least the text editor dirtiness flag will be good.
        return textEditor.isDirty();
    }

    /**
     * Prepares the content of the text editor before save.
     * If we are in a graphical mode it generates the text version and puts it into the text editor.
     */
    private void prepareForSave() {
        Assert.isTrue(!usePlainTextEditor);
        NedFileElementEx nedFileElement = getModel();

        if (getActivePage() == graphPageIndex && !nedFileElement.isReadOnly() && !nedFileElement.hasSyntaxError()) {
            textEditor.pullChangesFromNedResourcesWhenPending();
            graphicalEditor.markSaved();
        }

        if (getActivePage() == textPageIndex)
            textEditor.pushChangesIntoNedResources();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (usePlainTextEditor) {
            plainTextEditor.doSave(monitor);
            return;
        }

        prepareForSave();
        // delegate the save task to the TextEditor's save method
        textEditor.doSave(monitor);
        graphicalEditor.markSaved();
    }

    @Override
    public void doSaveAs() {
        if (usePlainTextEditor) {
            plainTextEditor.doSaveAs();
            setInput(plainTextEditor.getEditorInput());
            return;
        }

        prepareForSave();
        textEditor.doSaveAs();
        graphicalEditor.markSaved();
        setInput(textEditor.getEditorInput());
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Closes the editor and optionally saves it.
     */
    protected void closeEditor(boolean save) {
        getSite().getPage().closeEditor(this, save);
    }

    /**
     * This class listens to changes to the file system in the workspace, and
     * makes changes accordingly.
     * 1) An open, saved file gets deleted -> close the editor
     * 2) An open file gets renamed or moved -> change the editor's input accordingly
     */
    class ResourceTracker implements IResourceChangeListener, IResourceDeltaVisitor {
        public void resourceChanged(IResourceChangeEvent event) {
            IResourceDelta delta = event.getDelta();
            try {
                if (delta != null) delta.accept(this);
            }
            catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean visit(IResourceDelta delta) {
            if (delta == null || !delta.getResource().equals(((IFileEditorInput) getEditorInput()).getFile()))
                return true;

            Display display = getSite().getShell().getDisplay();
            if (delta.getKind() == IResourceDelta.CHANGED) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        inputFileModifiedOnDisk();
                    }
                });
            }
            return false;
        }
    }

    protected void inputFileModifiedOnDisk() {
        // the file was overwritten somehow (could have been
        // replaced by another version in the repository)
        // TODO ask the user and reload the file
        if (isDirty()) {
            ; //FIXME ask user!
        }
    }

    public void gotoMarker(IMarker marker) {
        if (usePlainTextEditor) {
            IGotoMarker gm = (IGotoMarker)plainTextEditor.getAdapter(IGotoMarker.class);
            if (gm != null)
                gm.gotoMarker(marker);
            return;
        }

        // switch to text page and delegate to it
        setActivePage(textPageIndex);
        IGotoMarker gm = (IGotoMarker)textEditor.getAdapter(IGotoMarker.class);
        if (gm != null)
            gm.gotoMarker(marker);
    }

    public void showInEditor(INedElement model, Mode mode) {
        Assert.isTrue(!usePlainTextEditor);

        Assert.isTrue(mostRecentlyUsedMode != Mode.AUTOMATIC);

        if (mode == Mode.AUTOMATIC) {
            if (model instanceof INedTypeElement || model instanceof ISubmoduleOrConnection) // has graphical representation
                mode = mostRecentlyUsedMode;
            else
                mode = Mode.TEXT;
        }

        if (mode == Mode.GRAPHICAL) {
            setActivePage(graphPageIndex);
            graphicalEditor.reveal(model);
        }
        else {
            setActivePage(textPageIndex);
            IDocument document = textEditor.getDocumentProvider().getDocument(getEditorInput());
            if (model.getSourceRegion() != null)
                try {
                    int startLine = model.getSourceRegion().getStartLine();
                    int endLine = model.getSourceRegion().getEndLine();
                    int startOffset = document.getLineOffset(startLine-1)+model.getSourceRegion().getStartColumn();
                    int endOffset = document.getLineOffset(endLine-1)+model.getSourceRegion().getEndColumn();
                    textEditor.setHighlightRange(startOffset, endOffset - startOffset, true);

                } catch (Exception e) {
                }
        }
    }

    // provides show in view options
    public String[] getShowInTargetIds() {
        return new String[] {IPageLayout.ID_OUTLINE,
                             IConstants.MODULEHIERARCHY_VIEW_ID,
                             IConstants.MODULEPARAMETERS_VIEW_ID,
                             IConstants.NEDINHERITANCE_VIEW_ID};
    }

    public ShowInContext getShowInContext() {
        return new ShowInContext(getEditorInput(), getSite().getSelectionProvider().getSelection());
    }

    public GraphicalNedEditor getGraphEditor() {
        return graphicalEditor;
    }

    public TextualNedEditor getTextEditor() {
        return textEditor;
    }

    public IFile getFile() {
        return ((FileEditorInput)getEditorInput()).getFile();
    }

    public NedFileElementEx getModel() {
        Assert.isTrue(!usePlainTextEditor);
        return NedResourcesPlugin.getNedResources().getNedFileElement(getFile());
    }

    // NOTE: this method is called from NON UI thread too (notifications) so
    // you should not call any SWT only methods like getActiveEditor etc.
    // instead we store the current editorIndex and do the compare by hand
    public boolean isActiveEditor(IEditorPart editorPart) {
        //Assert.isTrue(!usePlainTextEditor);
        if (editorPart == textEditor && currentPageIndex == textPageIndex)
            return true;
        if (editorPart == graphicalEditor && currentPageIndex == graphPageIndex)
            return true;
        return false;
    }
}
