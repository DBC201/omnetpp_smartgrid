/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.omnetpp.ned.editor.graph.GraphicalNedEditor;
import org.omnetpp.ned.editor.graph.actions.GNedActionBarContributor;
import org.omnetpp.ned.editor.text.TextualNedEditor;
import org.omnetpp.ned.editor.text.TextualNedEditorActionContributor;

/**
 * Manages the installation/deinstallation of global actions for multi-page editors.
 * Responsible for the redirection of global actions to the active editor.
 * Multi-page contributor replaces the contributors for the individual editors in the multi-page editor.
 *
 * @author rhornig
 */
public class NedEditorActionContributor extends MultiPageEditorActionBarContributor {
    private GNedActionBarContributor graphContrib;
    private TextualNedEditorActionContributor textContrib;
    private IEditorPart activeEditorPart;
    /**
     * Creates a multi-page contributor.
     */
    public NedEditorActionContributor() {
        super();
        // create the multi page editor's own actions (if any)
        graphContrib = new GNedActionBarContributor();
        textContrib = new TextualNedEditorActionContributor();
    }

    /* (non-JavaDoc)
     * Method declared in AbstractMultiPageEditorActionBarContributor.
     */

    @Override
    public void init(IActionBars bars) {
        super.init(bars);
        graphContrib.init(bars, getPage());
        textContrib.init(bars, getPage());
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorActionBarContributor#setActivePage(org.eclipse.ui.IEditorPart)
     * Respond to an editor change with reassigning the local and global actions
     */
    @Override
    public void setActivePage(IEditorPart part) {
        if (activeEditorPart == part)
            return;

        // set the new active editor
        activeEditorPart = part;

        // first remove the old global and local action handlers
        // then add the ones for the new editor
        if (part instanceof GraphicalNedEditor) {
            textContrib.setActiveEditor(activeEditorPart);
            graphContrib.setActiveEditor(activeEditorPart);
        }
        else if (part instanceof TextualNedEditor) {
            graphContrib.setActiveEditor(activeEditorPart);
            textContrib.setActiveEditor(activeEditorPart);
        } else
            return;

        getActionBars().updateActionBars();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
