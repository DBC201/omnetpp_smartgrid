/*--------------------------------------------------------------*
  Copyright (C) 2006-2020 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.actions.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.omnetpp.common.Debug;
import org.omnetpp.scave.ScaveImages;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.actions.AbstractScaveAction;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.editors.datatable.DataTable;
import org.omnetpp.scave.editors.datatable.FilteredDataPanel;
import org.omnetpp.scave.editors.ui.BrowseDataPage;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.engine.ResultItem;
import org.omnetpp.scave.model2.AndFilter;

/**
 * Sets the filter of a filtered data panel.
 * The filter is determined by the selected cell of the panel's control.
 *
 * @author andras
 */
public class SetFilterBySelectedCellAction extends AbstractScaveAction
{
    public SetFilterBySelectedCellAction() {
        setText("Filter Rows by Cell Value");
        setDescription("Sets the panel filter according to the clicked cell.");
        setImageDescriptor(ScavePlugin.getImageDescriptor(ScaveImages.IMG_ETOOL16_SETFILTER));
    }

    @Override
    protected void doRun(ScaveEditor scaveEditor, ISelection selection) throws CoreException {
        BrowseDataPage browseDataPage = scaveEditor.getBrowseDataPage();
        if (browseDataPage != scaveEditor.getActiveEditorPage())
            scaveEditor.showPage(browseDataPage);

        FilteredDataPanel panel = browseDataPage.getActivePanel();
        if (!(panel.getDataControl() instanceof DataTable))
            return;
        DataTable table = (DataTable)panel.getDataControl();
        long focusedID = table.getFocusedID();
        String selectedField = table.getSelectedField();
        if (focusedID == -1 || selectedField == null)
            return;
        ResultFileManager manager = panel.getResultFileManager();
        ResultFileManager.runWithReadLock(manager, () -> {
            Debug.println("selectedField: " + selectedField);
            ResultItem item = manager.getItem(focusedID);
            String fieldValue = item.getProperty(selectedField);

            AndFilter filter = new AndFilter();
            filter.setFieldValue(selectedField, fieldValue);
            panel.setFilter(filter.getFilterPattern());
        });
    }

    @Override
    protected boolean isApplicable(ScaveEditor editor, ISelection selection) {
        return editor.getBrowseDataPage().getActivePanel().getDataControl().getItemCount() != 0;
    }
}
