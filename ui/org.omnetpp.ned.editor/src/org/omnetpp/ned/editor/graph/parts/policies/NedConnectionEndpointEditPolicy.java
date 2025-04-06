/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.parts.policies;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.swt.graphics.Color;
import org.omnetpp.figures.ConnectionFigure;
import org.omnetpp.ned.editor.graph.GraphicalNedEditor;
import org.omnetpp.ned.editor.graph.misc.NedConnectionEndHandle;
import org.omnetpp.ned.editor.graph.misc.NedConnectionStartHandle;
import org.omnetpp.ned.editor.graph.parts.EditPartUtil;

/**
 * Defines selection handle creation/removal
 *
 * @author rhornig
 */
public class NedConnectionEndpointEditPolicy extends ConnectionEndpointEditPolicy {

    /* (non-Javadoc)
     * @see org.eclipse.gef.editpolicies.SelectionHandlesEditPolicy#addSelectionHandles()
     * Mark the selected connection with a bolder, highlighted line
     */
    @Override
    protected void addSelectionHandles() {
        super.addSelectionHandles();

        getConnectionFigure().setLineWidth(getConnectionFigure().getLocalLineWidth() + 1);
        Color color = EditPartUtil.isEditable(getHost()) ?
                GraphicalNedEditor.HIGHLIGHT_COLOR : GraphicalNedEditor.LOWLIGHT_COLOR;
        getConnectionFigure().setForegroundColor(color);
    }

    /* (non-Javadoc)
     * @see org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy#createSelectionHandles()
     * Redefine to create special connection handles that return NedConnectionEndpointTracker
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected List createSelectionHandles() {
        List list = new ArrayList();
        list.add(new NedConnectionEndHandle((ConnectionEditPart)getHost()));
        list.add(new NedConnectionStartHandle((ConnectionEditPart)getHost()));
        return list;
    }

    /* (non-Javadoc)
     * @see org.eclipse.gef.editpolicies.SelectionHandlesEditPolicy#removeSelectionHandles()
     * Return the connection to the original state (before selection)
     */
    @Override
    protected void removeSelectionHandles() {
        super.removeSelectionHandles();
        getConnectionFigure().setLineWidth(getConnectionFigure().getLocalLineWidth());
        getConnectionFigure().setLineStyle(getConnectionFigure().getLocalLineStyle());
        getConnectionFigure().setForegroundColor(getConnectionFigure().getLocalLineColor());
    }

    protected ConnectionFigure getConnectionFigure() {
        return (ConnectionFigure) getConnection();
    }

}