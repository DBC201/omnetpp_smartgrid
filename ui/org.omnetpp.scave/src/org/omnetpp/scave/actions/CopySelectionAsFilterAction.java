/*--------------------------------------------------------------*
  Copyright (C) 2006-2020 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.scave.ScaveImages;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.editors.IDListSelection;
import org.omnetpp.scave.editors.ScaveEditor;
import org.omnetpp.scave.model2.ResultSelectionFilterGenerator;

public class CopySelectionAsFilterAction extends AbstractScaveAction {

    public CopySelectionAsFilterAction() {
        setText("Copy Selection As Filter Expression");
        setToolTipText("Generate filter expression that matches the selected items");
        setImageDescriptor(ScavePlugin.getImageDescriptor(ScaveImages.IMG_ETOOL16_COPY));
    }

    @Override
    protected void doRun(ScaveEditor editor, ISelection selection) throws CoreException {
        IDListSelection idListSelection = (IDListSelection)selection;
        String filter = ResultSelectionFilterGenerator.makeFilterForIDListSelection(idListSelection);
        int types = idListSelection.getSource().getType().getItemTypes();
        editor.getFilterCache().putFilterResult(types, filter, editor.getBrowseDataPage().getScalarsPanel().getShowFieldsAsScalars(), idListSelection.getIDList());
        new Clipboard(Display.getCurrent()).setContents(new Object[] {filter}, new Transfer[] {TextTransfer.getInstance()});
    }

    @Override
    protected boolean isApplicable(ScaveEditor editor, ISelection selection) {
        return selection instanceof IDListSelection && !selection.isEmpty();
    }
}
