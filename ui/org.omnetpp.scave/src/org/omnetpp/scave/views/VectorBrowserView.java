/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.views;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.INullSelectionListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.omnetpp.common.engine.BigDecimal;
import org.omnetpp.common.ui.TimeTriggeredProgressMonitorDialog2;
import org.omnetpp.common.ui.ViewWithMessagePart;
import org.omnetpp.common.virtualtable.VirtualTable;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.editors.IDListSelection;
import org.omnetpp.scave.editors.ResultFileException;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.datatable.VectorResultContentProvider;
import org.omnetpp.scave.editors.datatable.VectorResultRowRenderer;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.VectorDatum;
import org.omnetpp.scave.engine.VectorResult;
import org.omnetpp.scave.model2.ChartDataPoint;
import org.omnetpp.scave.model2.ChartLine;
import org.omnetpp.scave.model2.ResultItemRef;

/**
 * View for vector data.
 *
 * @author tomi
 */
public class VectorBrowserView extends ViewWithMessagePart {
    public static final String ID = "org.omnetpp.scave.VectorBrowserView";

    protected TableColumn eventNumberColumn;
    protected VirtualTable<VectorDatum> viewer;
    protected ISelectionListener selectionChangedListener;
    protected IPartListener partListener;
    protected IWorkbenchPart activePart;
    protected VectorResultContentProvider contentProvider;

    protected Action copyToClipboardAction;
    protected GotoAction gotoLineAction;
    protected GotoAction gotoEventAction;
    protected GotoAction gotoTimeAction;

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        createContextMenu();
        hookListeners();
        setViewerInput(getSite().getPage().getSelection());
    }

    @Override
    protected Control createViewControl(final Composite parent) {
        contentProvider = new VectorResultContentProvider();
        viewer = new VirtualTable<VectorDatum>(parent, SWT.NONE) {
            @Override
            public void handleRuntimeException(RuntimeException e) {
                VectorBrowserView.this.handleRuntimeException(e);
            }
        };
        viewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer.setContentProvider(contentProvider);
        viewer.setRowRenderer(new VectorResultRowRenderer());
        viewer.setRunnableContextForLongRunningOperations(new IRunnableContext() {
            @Override
            public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
                TimeTriggeredProgressMonitorDialog2 dialog = new TimeTriggeredProgressMonitorDialog2(parent.getShell(), 1000);
                dialog.setCancelable(cancelable);
                dialog.run(fork, cancelable, runnable);
            }
        });

        TableColumn tableColumn = viewer.createColumn();
        tableColumn.setWidth(60);
        tableColumn.setText("Item#");
        tableColumn = viewer.createColumn();
        tableColumn.setWidth(140);
        tableColumn.setText("Time");
        tableColumn = viewer.createColumn();
        tableColumn.setWidth(140);
        tableColumn.setText("Value");
        return viewer;
    }

    private void createContextMenu() {
        copyToClipboardAction = new CopyToClipboardAction(viewer);
        gotoLineAction = new GotoAction(this, GotoTarget.Line);
        gotoEventAction = new GotoAction(this, GotoTarget.Event);
        gotoTimeAction = new GotoAction(this, GotoTarget.Time);

        Menu menu = new Menu(viewer.getCanvas());
        addMenuItem(menu, copyToClipboardAction);
        new MenuItem(menu, SWT.SEPARATOR);
        addMenuItem(menu, gotoLineAction);
        addMenuItem(menu, gotoEventAction);
        addMenuItem(menu, gotoTimeAction);
        viewer.getCanvas().setMenu(menu);
    }

    private void addMenuItem(Menu menu, final IAction action) {
        MenuItem item = new MenuItem(menu, SWT.NONE);
        item.setText(action.getText());
        item.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { action.run(); }
        });
    }

    public void gotoLine(int lineNumber) {
        VectorDatum entry = contentProvider.getElementBySerial(lineNumber);
        if (entry != null)
            viewer.gotoElement(entry);
    }

    public void gotoEvent(long eventNumber) {
        VectorDatum entry = contentProvider.getElementByEventNumber(eventNumber, true);
        if (entry != null)
            viewer.gotoElement(entry);
    }

    public void gotoTime(BigDecimal time) {
        VectorDatum entry = contentProvider.getElementBySimulationTime(time, true);
        if (entry != null)
            viewer.gotoElement(entry);
    }

    @Override
    public void dispose() {
        unhookListeners();
        super.dispose();
    }

    private void hookListeners() {
        selectionChangedListener = new INullSelectionListener() {
            @Override
            public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
                if (part == activePart)
                    setViewerInput(selection);
            }
        };
        getSite().getPage().addPostSelectionListener(selectionChangedListener);

        partListener = new IPartListener() {

            @Override
            public void partActivated(IWorkbenchPart part) {
                if (part instanceof ScaveEditor)
                    activePart = part;
            }

            @Override
            public void partClosed(IWorkbenchPart part) {
                if (part == activePart) {
                    activePart = null;
                    setViewerInput((ISelection)null);
                }
            }

            @Override
            public void partOpened(IWorkbenchPart part) {}
            @Override
            public void partBroughtToTop(IWorkbenchPart part) {}
            @Override
            public void partDeactivated(IWorkbenchPart part) {}
        };
        getSite().getPage().addPartListener(partListener);
    }

    private void unhookListeners() {
        if (selectionChangedListener != null) {
            getSite().getPage().removePostSelectionListener(selectionChangedListener);
            selectionChangedListener = null;
        }
        if (partListener != null) {
            getSite().getPage().removePartListener(partListener);
            partListener = null;
        }
    }

    @Override
    public void setFocus() {
        viewer.setFocus();
    }

    public void setViewerInput(ISelection selection) {
        ResultItemRef selectedVector = null;
        int dataPointIndex = -1;

        if (selection instanceof IDListSelection) {
            IDListSelection idlistSelection = (IDListSelection)selection;
            if (!idlistSelection.isEmpty()) {
                long id = idlistSelection.getIDList().get(0);
                if (ResultFileManager.getTypeOf(id) == ResultFileManager.VECTOR)
                    selectedVector = new ResultItemRef(id, idlistSelection.getResultFileManager());
            }
        }
        else if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection)selection).getFirstElement();
            if (selectedObject instanceof ChartLine) {  // TODO this branch is dead, currently no one sends ChartLine as selection
                ChartLine selectedLine = (ChartLine)selectedObject;
                ResultItemRef item = selectedLine.getResultItemRef();
                if (item != null && ResultFileManager.getTypeOf(item.getID()) == ResultFileManager.VECTOR) {
                    selectedVector = item;
                    if (selectedObject instanceof ChartDataPoint)
                        dataPointIndex = ((ChartDataPoint)selectedObject).getIndex();
                }
            }
        }

        if (selectedVector == null) {
            setContentDescription("");
            viewer.setInput(null);
        }
        else {
            final ResultItemRef selectedVector_ = selectedVector;
            final int dataPointIndex_ = dataPointIndex;
            ResultFileManager.runWithReadLock(selectedVector.getResultFileManager(), () -> {
                doSetViewerInput(selectedVector_, dataPointIndex_);
            });
        }

    }

    protected void doSetViewerInput(ResultItemRef selectedVector, int dataPointIndex) {
        Assert.isTrue(selectedVector.resolve() instanceof VectorResult);
        selectedVector.getResultFileManager().checkReadLock();

        if (!ObjectUtils.equals(selectedVector, viewer.getInput())) {
            try {
                viewer.setInput(selectedVector); // note: all error checking regarding the underlying vec/vci files is here (in content provider's inputChanged() method)
            }
            catch (RuntimeException ex) {
                handleRuntimeException(ex);
                return;
            }

            VectorResult vector = (VectorResult)selectedVector.resolve();
            hideMessage();
            boolean hasEventNumbers = vector.getColumns().indexOf('E') >= 0;
            setEventNumberColumnVisible(hasEventNumbers);
            gotoEventAction.setEnabled(hasEventNumbers);
            setEventNumberColumnVisible(hasEventNumbers);
            setContentDescription(String.format("'%s.%s' vectorID=%d in run %s from file %s",
                    vector.getModuleName(), vector.getName(), vector.getVectorId(),
                    vector.getFileRun().getRun().getRunName(),
                    vector.getFile().getFilePath()));
            viewer.redraw();
        }

        if (dataPointIndex >= 0)
            gotoLine(dataPointIndex);
    }

    protected void handleRuntimeException(RuntimeException e) {
        String message;
        if (e instanceof ResultFileException)
            message = e.getMessage() + " -- try reloading result files in the editor";
        else {
            message = "An error occurred during refreshing.";
            ScavePlugin.logError(e);
        }
        setContentDescription(message);
        viewer.setInput(null);
    }

    // Order: Item#, Event#, Time, Value
    private static final int[] ColumnOrder = new int[] {0,3,1,2};

    private void setEventNumberColumnVisible(boolean visible) {
        if (eventNumberColumn != null && !visible) {
            eventNumberColumn.dispose();
            eventNumberColumn = null;
        }
        else if (eventNumberColumn == null && visible) {
            eventNumberColumn = viewer.createColumn();
            eventNumberColumn.setWidth(60);
            eventNumberColumn.setText("Event#");
            viewer.setColumnOrder(ColumnOrder);
        }
    }

    static class CopyToClipboardAction extends Action {
        VirtualTable<?> table;

        public CopyToClipboardAction(VirtualTable<?> table) {
            this.table = table;
            setText("Copy to clipboard");
        }

        @Override
        public boolean isEnabled() {
            return !table.getSelection().isEmpty();
        }

        @Override
        public void run() {
            table.copySelectionToClipboard();
        }
    }

    enum GotoTarget {
        Line,
        Event,
        Time,
    }

    static class GotoAction extends Action
    {
        VectorBrowserView view;
        GotoTarget target;
        String prompt;

        public GotoAction(VectorBrowserView view, GotoTarget target) {
            this.view = view;
            this.target = target;
            switch (target) {
            case Line: setText("Go to line..."); prompt = "Line number:"; break;
            case Event: setText("Go to event..."); prompt = "Event number:"; break;
            case Time: setText("Go to time..."); prompt = "Time:"; break;
            }
        }

        @Override
        public void run() {
            IInputValidator validator = new IInputValidator() {
                @Override
                public String isValid(String text) {
                    if (text == null || text.length() == 0)
                        return " ";
                    else if (parseTarget(text) == null)
                        return "Enter a" + (target == GotoTarget.Time ? "" : "n integer") + " number";
                    else
                        return null; // ok
                }
            };
            InputDialog dialog = new InputDialog(view.getSite().getShell(), "Go to", prompt, "", validator);
            if (dialog.open() == Window.OK) {
                Object targetAddr = parseTarget(dialog.getValue());
                if (targetAddr != null) {
                    switch (target) {
                    case Line: view.gotoLine((Integer)targetAddr); break;
                    case Event: view.gotoEvent((Long)targetAddr); break;
                    case Time: view.gotoTime((BigDecimal)targetAddr); break;
                    }
                }
            }
        }

        public Object parseTarget(String str) {
            try
            {
                switch (target) {
                case Time: return BigDecimal.parse(str);
                case Line: return Integer.parseInt(str);
                case Event: return Long.parseLong(str);
                default: return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }
}
