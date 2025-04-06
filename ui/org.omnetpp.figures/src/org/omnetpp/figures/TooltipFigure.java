/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.ParagraphTextLayout;
import org.eclipse.draw2d.text.TextFlow;

/**
 * Figure used to ad tooltip information to other figures.
 *
 * @author rhornig
 */
//FIXME obsolete class, change usages to ITooltipTextProvider, and remove!!!
public class TooltipFigure extends FlowPage {

    private ITooltipTextProvider textProvider;
    private String text;
    private final TextFlow textFlow;

    public TooltipFigure() {
        setBorder(new MarginBorder(2));
        setBackgroundColor(ColorConstants.tooltipBackground);
        setForegroundColor(ColorConstants.tooltipForeground);

        textFlow = new TextFlow();
        // XXX hack because otherwise it throws exception in BiDi implementation
        textFlow.setText(" ");
        textFlow.setLayoutManager(new ParagraphTextLayout(textFlow, ParagraphTextLayout.WORD_WRAP_SOFT));
        setMaximumSize(new Dimension(120,60));
        add(textFlow);
        setHorizontalAligment(PositionConstants.LEFT);
    }

    /**
     * Returns the text inside the TextFlow.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text of the TextFlow to the given value.
     */
    public void setText(String newText) {
        text = newText;
    }

    /**
     * Sets the text provider callback. It is used only if no text is directly set on the figure.
     */
    public void setTextProvider(ITooltipTextProvider textProvider) {
        this.textProvider = textProvider;
    }

    @Override
    public void repaint() {
        // set the textFlow content from the text field or from textProvider if no text was directly provided
        // FIXME check if getTooltipText(-1,-1) correct
        if (textFlow != null)
            textFlow.setText(text != null ? text : (textProvider != null ? textProvider.getTooltipText(-1,-1) : null));
        super.repaint();
    }

    @Override
    public Dimension getPreferredSize(int w, int h) {
        Dimension d = super.getPreferredSize(-1, -1);
        if (d.width > 400)
            d = super.getPreferredSize(400, -1);
        return d;
    }
}