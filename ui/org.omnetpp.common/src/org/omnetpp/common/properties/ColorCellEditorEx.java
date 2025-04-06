/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.properties;

import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.contentassist.ContentProposalProviderBase;

/**
 * A cell editor that manages a color field. Uses ColorFactory.
 * Supports content assist function, and direct text editing of the color name.
 * <p>
 * This is a copied/modified version of the platform ColorCellEditor.
 * </p>
 */
public class ColorCellEditorEx extends TextCellEditorEx {

    public static class ColorContentProposalProvider extends ContentProposalProviderBase {
        @Override
        public IContentProposal[] getProposals(String contents, int position) {
            String prefix = contents.substring(0, position);
            List<IContentProposal> candidates = sort(toProposals(ColorFactory.getColorNames()));
            return filterAndWrapProposals(candidates, prefix, true, position);
        }
    }

    /**
     * Creates a new color cell editor with the given control as parent.
     * The cell editor value is black (<code>RGB(0,0,0)</code>) initially, and has no
     * validator.
     *
     * @param parent the parent control
     */
    public ColorCellEditorEx(Composite parent) {
        this(parent, SWT.NONE);
    }

    /**
     * Creates a new color cell editor with the given control as parent.
     * The cell editor value is black (<code>RGB(0,0,0)</code>) initially, and has no
     * validator.
     *
     * @param parent the parent control
     * @param style the style bits
     * @since 2.1
     */
    public ColorCellEditorEx(Composite parent, int style) {
        super(parent, style);
        doSetValue("");
    }

    @Override
    protected Control createControl(Composite parent) {
        Control result = super.createControl(parent);
        IContentProposalProvider proposalProvider = new ColorContentProposalProvider();
        new ContentAssistCommandAdapter(text, new TextContentAdapter(), proposalProvider,
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, null, true);

        return result;
    }

    /* (non-Javadoc)
     * Method declared on DialogCellEditor.
     */
    protected Object openDialogBox(Control cellEditorWindow) {
        ColorDialog dialog = new ColorDialog(cellEditorWindow.getShell());
        RGB value = ColorFactory.asRGB((String)getValue());
        if (value != null) {
            dialog.setRGB(value);
        }
        value =  dialog.open();
        return (value == null) ? null : ColorFactory.asString(value);
    }

}
