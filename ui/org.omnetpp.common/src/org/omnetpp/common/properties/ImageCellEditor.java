/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.properties;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.omnetpp.common.contentassist.ContentProposalProviderBase;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.common.image.ImageSelectionDialog;

public class ImageCellEditor extends TextCellEditorEx {

    public static class ImageContentProposalProvider extends ContentProposalProviderBase {
        private IProject project;

        public ImageContentProposalProvider(IProject project) {
            this.project = project;
        }

        @Override
        public IContentProposal[] getProposals(String contents, int position) {
            String prefix = contents.substring(0, position);
            List<String> stringList = prefix.contains("/") ? ImageFactory.of(project).getImageNameList() : ImageFactory.of(project).getCategories();
            List<IContentProposal> candidates = sort(toProposals(stringList.toArray(new String[0])));
            return filterAndWrapProposals(candidates, prefix, true, position);
        }
    }

    final IProject project; // scope of the image search

    public ImageCellEditor(IProject project) {
        super();
        this.project = project;
    }

    public ImageCellEditor(Composite parent, int style, IProject project) {
        super(parent, style);
        this.project = project;
    }

    protected ImageCellEditor(Composite parent, IProject project) {
        super(parent);
        this.project = project;
    }

    @Override
    protected Control createControl(Composite parent) {
        Control result = super.createControl(parent);
        IContentProposalProvider proposalProvider = new ImageContentProposalProvider(project);
        new ContentAssistCommandAdapter(text, new TextContentAdapter(), proposalProvider,
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, "/".toCharArray(), true);

        return result;
    }

    @Override
    protected Object openDialogBox(Control cellEditorWindow) {
        ImageSelectionDialog cellDialog =
            new ImageSelectionDialog(cellEditorWindow.getShell(), (String)getValue(), project);

        if (cellDialog.open() == Dialog.OK)
            return cellDialog.getImageId();

        // dialog cancelled
        return null;
    }

}
