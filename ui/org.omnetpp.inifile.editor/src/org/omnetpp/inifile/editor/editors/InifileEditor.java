/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.inifile.editor.editors;

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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.omnetpp.common.IConstants;
import org.omnetpp.common.ui.SelectionProvider;
import org.omnetpp.common.util.DelayedJob;
import org.omnetpp.common.util.DetailedPartInitException;
import org.omnetpp.inifile.editor.IGotoInifile;
import org.omnetpp.inifile.editor.InifileEditorPlugin;
import org.omnetpp.inifile.editor.actions.ToggleAnalysisAction;
import org.omnetpp.inifile.editor.form.InifileFormEditor;
import org.omnetpp.inifile.editor.model.IInifileChangeListener;
import org.omnetpp.inifile.editor.model.IInifileDocument;
import org.omnetpp.inifile.editor.model.IReadonlyInifileDocument.LineInfo;
import org.omnetpp.inifile.editor.model.InifileAnalyzer;
import org.omnetpp.inifile.editor.model.InifileDocument;
import org.omnetpp.inifile.editor.text.InifileTextEditor;
import org.omnetpp.inifile.editor.views.InifileContentOutlinePage;

/**
 * Editor for omnetpp.ini files.
 */
//FIXME File|Revert is always disabled
//FIXME crashes if file gets renamed or moved
//TODO for units, tooltip should display "seconds" not only "s"
public class InifileEditor extends MultiPageEditorPart implements IGotoMarker, IGotoInifile, IShowInSource, IShowInTargetList {
    public static final String ID = "org.omnetpp.inifile.editor";

    private static final String PREF_ACTIVE_PAGE = "ActivePage";

    // various paramresolution timeouts, all in milliseconds
    public static final int CONTENTASSIST_TIMEOUT = 1000;
    public static final int HOVER_TIMEOUT = 1000;
    public static final int HYPERLINKDETECTOR_TIMEOUT = 500;

    /* editor pages */
    private InifileTextEditor textEditor;
    private InifileFormEditor formEditor;
    public static final int FORMEDITOR_PAGEINDEX = 0;
    public static final int TEXTEDITOR_PAGEINDEX = 1;

    private InifileEditorData editorData = new InifileEditorData();
    private ResourceTracker resourceTracker = new ResourceTracker();
    private InifileContentOutlinePage outlinePage;
    private DelayedJob postSelectionChangedJob;

    private ToggleAnalysisAction toggleAnalysisAction = new ToggleAnalysisAction();

    /**
     * Creates the ini file editor.
     */
    public InifileEditor() {
        super();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceTracker);
    }

    public InifileEditorData getEditorData() {
        return editorData;
    }

    public InifileTextEditor getTextEditor() {
        return textEditor;
    }

    public InifileFormEditor getFormEditor() {
        return formEditor;
    }

    public IAction getToggleAnalysisAction() {
        return toggleAnalysisAction;
    }

    /**
     * Creates the text editor page of the multi-page editor.
     */
    void createTextEditorPage() {
        try {
            textEditor = new InifileTextEditor(this);
            int index = addPage(textEditor, getEditorInput());
            setPageText(index, "Source");
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
        }
    }

    /**
     * Creates the pages of the multi-page editor.
     */
    @Override
    protected void createPages() {
        // create form page
        formEditor = new InifileFormEditor(getContainer(), this);
        addEditorPage(formEditor, "Form");

        // create text editor
        createTextEditorPage();

        // assert page constants are OK
        Assert.isTrue(getControl(FORMEDITOR_PAGEINDEX)==formEditor && getEditor(TEXTEDITOR_PAGEINDEX)==textEditor);

        int pageIndex = readActivePageFromPreferences();
        if (0 <= pageIndex && pageIndex < getPageCount())
            setActivePage(pageIndex);

        // set up editorData (the InifileDocument)
        IFile file = ((IFileEditorInput)getEditorInput()).getFile();
        IDocument document = textEditor.getDocumentProvider().getDocument(getEditorInput());
        IInifileDocument inifileDocument = new InifileDocument(document, file);
        editorData.initialize(this, inifileDocument, new InifileAnalyzer(inifileDocument));

        // setTargetEditor can be called after editorData is initialized
        toggleAnalysisAction.setTargetEditor(this);

        // replace original MultiPageSelectionProvider with our own, as we want to
        // publish our own selection (with InifileSelectionItem) for both pages.
        getSite().setSelectionProvider(new SelectionProvider());

        // propagate property changes (esp. PROP_DIRTY) from our text editor
        textEditor.addPropertyListener(new IPropertyListener() {
            public void propertyChanged(Object source, int propertyId) {
                firePropertyChange(propertyId);
            }
        });

//      //XXX experimental
//      // see registration of InformationDispatchAction in AbstractTextEditor
//      IAction action = new Action("F2!!!") {
//          public void run() {
//              Debug.println("F2 pressed!");
//          }
//      };
//      action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_INFORMATION);
////        IKeyBindingService keyBindingService = getEditorSite().getKeyBindingService();
////        keyBindingService.registerAction(action);
//
//      //IWorkbench workbench = PlatformUI.getWorkbench();
//      //IHandlerService handlerService = (IHandlerService)workbench.getAdapter(IHandlerService.class);
//      //IHandlerService handlerService = (IHandlerService)getEditorSite().getService(IHandlerService.class);
//      IHandlerService handlerService = (IHandlerService)textEditor.getEditorSite().getService(IHandlerService.class);
//      IHandler actionHandler = new ActionHandler(action);
//      handlerService.activateHandler(ITextEditorActionDefinitionIds.SHOW_INFORMATION, actionHandler);

        // this DelayedJob will, after a delay, publish a new editor selection towards the workbench
        postSelectionChangedJob = new DelayedJob(600) {
            public void run() {
                updateSelection();
            }
        };

        // we want to update the selection whenever the document changes, or the cursor position in the text editor changes
        editorData.getInifileDocument().addInifileChangeListener(new IInifileChangeListener() {
            public void modelChanged() {
                postSelectionChangedJob.restartTimer();
            }
        });
        textEditor.setPostCursorPositionChangeJob(new Runnable() {
            public void run() {
                postSelectionChangedJob.restartTimer();
            }
        });

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                // schedule initial analysis of the inifile
                InifileDocument doc = (InifileDocument) editorData.getInifileDocument();
                InifileAnalyzer analyzer = editorData.getInifileAnalyzer();
                doc.parseIfChanged();
                analyzer.startAnalysisIfChanged();

                // open the "Module Parameters" view
                try {
                    IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    if (workbenchPage != null) { // note: may be null during platform startup...
                        workbenchPage.showView(IConstants.MODULEPARAMETERS_VIEW_ID);
                        workbenchPage.activate(InifileEditor.this); // otherwise editor loses focus
                    }
                } catch (PartInitException e) {
                    InifileEditorPlugin.logError(e);
                }

                // publish an initial selection (select first section)
                String[] sectionNames = doc.getSectionNames();
                if (sectionNames.length > 0)
                    setSelection(sectionNames[0], null);
            }

        });
    }

    private int readActivePageFromPreferences() {
        IPreferenceStore store = InifileEditorPlugin.getDefault().getPreferenceStore();
        String pageName = store.getString(PREF_ACTIVE_PAGE);
        return "Form".equals(pageName) ? FORMEDITOR_PAGEINDEX :
                "Source".equals(pageName) ? TEXTEDITOR_PAGEINDEX : -1;
    }

    private void storeActivePageInPreferences(int pageIndex) {
        IPreferenceStore store = InifileEditorPlugin.getDefault().getPreferenceStore();
        String pageName = null;
        switch (pageIndex) {
        case FORMEDITOR_PAGEINDEX: pageName = "Form"; break;
        case TEXTEDITOR_PAGEINDEX: pageName = "Source"; break;
        }
        store.setValue(PREF_ACTIVE_PAGE, pageName);
    }

    protected void updateSelection() {
        int cursorLine = textEditor.getCursorLine();
        String section = getEditorData().getInifileDocument().getSectionForLine(cursorLine);
        String key = getEditorData().getInifileDocument().getKeyForLine(cursorLine);
        setSelection(section, key);
    }

    /**
     * Sets the editor's selection to an InifileSelectionItem containing
     * the given section and key.
     */
    public void setSelection(String section, String key) {
        ISelection selection = new StructuredSelection(new InifileSelectionItem(getEditorData().getInifileDocument(), getEditorData().getInifileAnalyzer(), section, key));
        ISelectionProvider selectionProvider = getSite().getSelectionProvider();
        selectionProvider.setSelection(selection);
    }

    /**
     * Adds an editor page at the last position.
     */
    protected int addEditorPage(Control page, String label) {
        int index = addPage(page);
        setPageText(index, label);
        return index;
    }

    /**
     * The <code>MultiPageEditorPart</code> implementation of this
     * <code>IWorkbenchPart</code> method disposes all nested editors.
     * Subclasses may extend.
     */
    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceTracker);
        if (outlinePage != null)
            outlinePage.setInput(null); //XXX ?
        editorData.getInifileAnalyzer().dispose();
        editorData.getInifileDocument().dispose();
        super.dispose();
    }

    /**
     * Saves the multi-page editor's document.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        textEditor.doSave(monitor);
    }

    /**
     * Saves the multi-page editor's document as another file.
     * Also updates the text for page 0's tab, and updates this multi-page editor's input
     * to correspond to the nested editor's.
     */
    @Override
    public void doSaveAs() {
        textEditor.doSaveAs();
        setInput(textEditor.getEditorInput());
    }

    /**
     * The <code>MultiPageEditorExample</code> implementation of this method
     * checks that the input is an instance of <code>IFileEditorInput</code>.
     */
    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput))
            throw new DetailedPartInitException("Invalid input, it must be a file in the workspace: " + editorInput.getName(),
                "Please make sure the project is open before trying to open a file in it.");

        super.init(site, editorInput);

        setPartName(editorInput.getName());
    }

    /* (non-Javadoc)
     * Method declared on IEditorPart.
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Notification about page change.
     */
    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        if (getControl(newPageIndex) == formEditor) {
            formEditor.pageSelected();
        }
        else {
            formEditor.pageDeselected();
        }
        storeActivePageInPreferences(newPageIndex);
    }

    /**
     * Returns true if the editor is currently switched to the form editor.
     */
    public boolean isFormPageDisplayed() {
        return getActivePage()==FORMEDITOR_PAGEINDEX;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object getAdapter(Class required) {
        if (IContentOutlinePage.class.equals(required)) {
            if (outlinePage == null) {
                outlinePage = new InifileContentOutlinePage(this);
                outlinePage.setInput(getEditorData().getInifileDocument());
            }
            return outlinePage;
        }
        return super.getAdapter(required);
    }

    /**
     * This class listens to changes to the file system in the workspace, and
     * makes changes accordingly.
     * 1) An open, saved file gets deleted -> close the editor
     * 2) An open file gets renamed or moved -> change the editor's input accordingly
     */
    class ResourceTracker implements IResourceChangeListener, IResourceDeltaVisitor {
        public void resourceChanged(IResourceChangeEvent event) {
            // close editor on project close
            if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
                final IEditorPart thisEditor = InifileEditor.this;
                final IResource resource = event.getResource();
                Display.getDefault().asyncExec(new Runnable(){
                    public void run(){
                        if (((FileEditorInput)thisEditor.getEditorInput()).getFile().getProject().equals(resource)) {
                            thisEditor.getSite().getPage().closeEditor(thisEditor, true);
                        }
                    }
                });
            }
            // visit all changed resources and check if we have changed/deleted
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
            if (delta.getKind() == IResourceDelta.REMOVED) {
                if ((IResourceDelta.MOVED_TO & delta.getFlags()) == 0) {
                    // if the file was deleted
                    display.asyncExec(new Runnable() {
                        public void run() {
                            inputFileDeletedFromDisk();
                        }
                    });
                }
                else {
                    // else if it was moved or renamed
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
                    display.asyncExec(new Runnable() {
                        public void run() {
                            inputFileMovedOrRenamedOnDisk(newFile);
                        }
                    });
                }
            }
            else if (delta.getKind() == IResourceDelta.CHANGED) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        inputFileModifiedOnDisk();
                    }
                });
            }
            return false;
        }
    }

    protected void inputFileDeletedFromDisk() {
        closeEditor(false);
    }

    protected void inputFileMovedOrRenamedOnDisk(IFile newFile) {
        closeEditor(false);
    }

    protected void inputFileModifiedOnDisk() {
        // TODO ask the user to keep/throw away change
    }

    /**
     * Closes the editor and optionally saves it.
     */
    protected void closeEditor(boolean save) {
        getSite().getPage().closeEditor(this, save);
    }

    /* (non-Javadoc)
     * Method declared on IGotoMarker
     */
    public void gotoMarker(IMarker marker) {
        setActivePage(TEXTEDITOR_PAGEINDEX);
        IDE.gotoMarker(textEditor, marker);
    }

    /* (non-Javadoc)
     * Method declared on IGotoInifile
     */
    public void gotoSection(String section, Mode mode) {
        gotoSectionOrEntry(section, null, mode);
    }

    /* (non-Javadoc)
     * Method declared on IGotoInifile
     */
    public void gotoEntry(String section, String key, Mode mode) {
        gotoSectionOrEntry(section, key, mode);
    }

    private void gotoSectionOrEntry(String section, String key, Mode mode) {
        // switch to the requested page. If mode==AUTO, stay where we are.
        // Note: setActivePage() gives focus to the editor, so don't call it with AUTO mode.
        if (mode==IGotoInifile.Mode.FORM)
            setActivePage(FORMEDITOR_PAGEINDEX);
        else if (mode==IGotoInifile.Mode.FORM)
            setActivePage(TEXTEDITOR_PAGEINDEX);

        // perform "go to" on whichever page is displayed
        if (getActivePage()==FORMEDITOR_PAGEINDEX) {
            // form editor
            if (section != null) {
                if (key==null)
                    formEditor.gotoSection(section);
                else
                    formEditor.gotoEntry(section, key);
            }
        }
        else {
            // text editor
            LineInfo line = section==null ? null : key==null ?
                    editorData.getInifileDocument().getSectionLineDetails(section) :
                    editorData.getInifileDocument().getEntryLineDetails(section, key);
            highlightLineInTextEditor(line); //XXX highlight the whole section
        }
    }

    protected void highlightLineInTextEditor(LineInfo line) {
        if (line==null) {
            textEditor.resetHighlightRange();
            return;
        }
        //XXX check IFile matches!!!!
        try {
            IDocument docu = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            int startOffset = docu.getLineOffset(line.getLineNumber()-1);
            int endOffset = docu.getLineOffset(line.getLineNumber())-2;
            textEditor.setHighlightRange(endOffset, 0, true); // move cursor to end of selected line
            textEditor.setHighlightRange(startOffset, endOffset-startOffset, false); // set range without moving cursor
        } catch (BadLocationException e) {
        }
        catch (IllegalArgumentException x) {
            textEditor.resetHighlightRange();
        }
    }

    /* (non-Javadoc)
     * Method declared on IShowInSource
     */
    public ShowInContext getShowInContext() {
        return new ShowInContext(getEditorInput(), getSite().getSelectionProvider().getSelection());
    }

    /* (non-Javadoc)
     * Method declared on IShowInTargetList
     */
    public String[] getShowInTargetIds() {
        // contents of the "Show In..." context menu
        return new String[] {
                IConstants.MODULEHIERARCHY_VIEW_ID,
                IConstants.MODULEPARAMETERS_VIEW_ID,
                IPageLayout.ID_OUTLINE,
                IPageLayout.ID_PROBLEM_VIEW,
                IPageLayout.ID_PROJECT_EXPLORER,
                };
    }
}
