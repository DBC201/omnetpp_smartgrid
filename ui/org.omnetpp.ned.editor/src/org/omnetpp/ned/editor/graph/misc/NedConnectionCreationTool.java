/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.misc;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.tools.ConnectionCreationTool;
import org.omnetpp.ned.editor.graph.commands.CreateConnectionCommand;
import org.omnetpp.ned.editor.graph.commands.ReconnectCommand;
import org.omnetpp.ned.editor.graph.parts.CompoundModuleEditPart;
import org.omnetpp.ned.editor.graph.parts.ModuleEditPart;
import org.omnetpp.ned.model.ex.CompoundModuleElementEx;
import org.omnetpp.ned.model.ex.ConnectionElementEx;

/**
 * Special connection tool that requests additional information regarding gate association at the
 * end of connection creation. It pops up a menu with all gate pairs for selection.
 *
 * @author rhornig
 */
// CHECKME we can use ConnectionDragCreationTool for a dran'n drop type behavior
public class NedConnectionCreationTool extends ConnectionCreationTool {

    public NedConnectionCreationTool() {
        // this is required only to override the cursors for the tool because the default cursors
        // in GEF have the image data and mask data swapped (a bug in the GEF code)
        // see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=467983
        setDefaultCursor(NedSharedCursors.CURSOR_PLUG);
        setDisabledCursor(NedSharedCursors.CURSOR_PLUG_NOT);
    }

    // override the method to fix a GEF BUGFIX
    @Override
    protected boolean handleButtonDown(int button) {
        // BUGFIX START
        // The original implementation reaches the TERMINAL state BEFORE calling
        // handleCreateConnect(). This causes a problem if we place an event loop in
        // handleCreateConnect() as the tool resets to initial state (no source or target parts)
        // leaving the previous sourceFeedback figure in place.
        if (button == 1 && isInState(STATE_CONNECTION_STARTED)) {
            boolean creationResult = handleCreateConnection();
            setState(STATE_TERMINAL);
            return creationResult;
        }
        // BUGFIX END

        super.handleButtonDown(button);
        if (isInState(STATE_CONNECTION_STARTED))
            //Fake a drag to cause feedback to be displayed immediately on mouse down.
            handleDrag();
        return true;
    }


    // overridden to enable a popup menu during connection creation asking the user
    // which gates should be connected
    @Override
    protected boolean handleCreateConnection() {
        Command command = getCommand();
        if (command == null || !(command instanceof CreateConnectionCommand))
            return false;

        CreateConnectionCommand endCommand = (CreateConnectionCommand)command;
        ModuleEditPart destMod = (ModuleEditPart)getTargetEditPart();
        CompoundModuleElementEx compoundMod = destMod.getCompoundModulePart().getModel();
        // ask the user about which gates should be connected, ask for both source and destination gates
        ConnectionElementEx connection = endCommand.getConnection();
        ConnectionElementEx templateConn = new ConnectionChooser().open(compoundMod, connection, true, true);

        eraseSourceFeedback();

        // if no selection was made, cancel the command
        if (templateConn == null)
            // revert the connection change (user cancel - do not execute the command)
            return false;

        ReconnectCommand.copyConn(templateConn, connection);
        if (connection.getType() == null && templateConn.getType() != null)
            connection.setType(templateConn.getType());

        setCurrentCommand(endCommand);
        executeCurrentCommand();
        return true;
    }

    // filter which editparts can be used as connection source or target
    @Override
    protected EditPartViewer.Conditional getTargetingConditional() {

        return new EditPartViewer.Conditional() {
            public boolean evaluate(EditPart editpart) {
                // during the connection creation, check if the target editpart is a valid editpart
                // ie: submodule in the same compound module or the parent compound module
                if (isInState(STATE_CONNECTION_STARTED)) {
                    EditPart srcEditPart = ((CreateConnectionRequest)getTargetRequest()).getSourceEditPart();
                    EditPart destEditPart = editpart;

                    if (srcEditPart == null || destEditPart == null ||
                            !(srcEditPart instanceof ModuleEditPart) || !(destEditPart instanceof ModuleEditPart))
                        return false;

                    if (((ModuleEditPart)srcEditPart).getCompoundModulePart() != ((ModuleEditPart)destEditPart).getCompoundModulePart())
                        return false;
                }
                // if the selection target is a CompoundModule, allow selection ONLY using it's borders
                if (editpart instanceof CompoundModuleEditPart) {
                    CompoundModuleEditPart cmep = (CompoundModuleEditPart)editpart;
                    return cmep.isOnBorder(getLocation().x, getLocation().y);
                }

                return editpart.isSelectable();
            }
        };
    }
}
