/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.misc;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.omnetpp.ned.editor.graph.GraphicalNedEditor;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.interfaces.INedModelProvider;

/**
 * Special synchronizer that syncs any selected editpart to the first editpart which is found among
 * the ancestors. Returns null if no suitable editpart found in the viewer
 *
 * @author rhornig
 */
public class NedSelectionSynchronizer extends SelectionSynchronizer {

    private boolean linkWithEditor = false;

    /**
     * Maps the given editpart from one viewer to an editpart in another viewer.
     * It returns null if there is no corresponding part. Travels along the ancestors
     * towards root, and returns the first matching ancestor.
     *
     * @param viewer the viewer being mapped to
     * @param part a part from another viewer
     * @return <code>null</code>, or a corresponding editpart
     */
    @Override
    protected EditPart convert(EditPartViewer viewer, EditPart part) {
        if (part instanceof INedModelProvider) {
            INedElement model = ((INedModelProvider)part).getModel();
            return GraphicalNedEditor.getNearestEditPartForModel(viewer, model);
        }
        return null;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        // we don't want to publish the changes from the editor unless linkWithEditor is set
        if (!(event.getSource() instanceof ScrollingGraphicalViewer) || linkWithEditor ) {
            // Debug.println("*** ned synchronizer selection changed selection changed from: "+event.getSource());
            super.selectionChanged(event);
        }
    }
}
