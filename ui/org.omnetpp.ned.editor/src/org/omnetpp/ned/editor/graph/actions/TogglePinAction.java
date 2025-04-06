/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.commands.UnexecutableCommand;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPart;
import org.omnetpp.common.displaymodel.PointF;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.figures.SubmoduleFigure;
import org.omnetpp.ned.editor.graph.commands.SetConstraintCommand;
import org.omnetpp.ned.editor.graph.parts.EditPartUtil;
import org.omnetpp.ned.editor.graph.parts.SubmoduleEditPart;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.IConnectableElement;

/**
 * TODO add documentation
 *
 * @author rhornig
 */
public class TogglePinAction extends org.eclipse.gef.ui.actions.SelectionAction {

    public static final String ID = "Pinned";
    public static final String MENUNAME = "Pi&nned";
    public static final String TOOLTIP = "Pins the module to a location";
    public static final ImageDescriptor IMAGE = ImageFactory.global().getDescriptor(ImageFactory.TOOLBAR_IMAGE_UNPIN);

    public TogglePinAction(IWorkbenchPart part) {
        super(part, IAction.AS_CHECK_BOX);
        setText(MENUNAME);
        setId(ID);
        setToolTipText(TOOLTIP);
        setImageDescriptor(IMAGE);
        setHoverImageDescriptor(IMAGE);
    }

    /**
     * This command can be executed ONLY on submodules that have fixed location
     */
    @Override
    protected boolean calculateEnabled() {
        Command cmd = getCommand();
        if (cmd == null) {
            setChecked(false);
            return false;
        }
        return cmd.canExecute();
    }

    @Override
    protected void refresh() {
        int size = getSelectedObjects().size();
        if (size > 0) {
            Object primarySelection = getSelectedObjects().get(size-1);
            PointF loc = null;
            if (primarySelection instanceof GraphicalEditPart) {
                Object model = ((GraphicalEditPart)primarySelection).getModel();
                if (model instanceof IConnectableElement)
                    loc = ((IConnectableElement)model).getDisplayString().getLocation();
            }
            setChecked(loc != null);
        }
        super.refresh();
    }

    @Override
    public void run() {
        execute(getCommand());
    }

    /**
     * Returns the <code>Command</code> to unpin a group of children.
     * @param request the ChangeBoundsRequest
     * @param toggleEnabled whether we want to toggle the state of the child
     * @param pinState if toggleEnabled was false, we can set the new pin state directly here
     */
    protected Command getCommand() {
        CompoundCommand resize = new CompoundCommand();

        for (Object child : getSelectedObjects()) {
            if (child instanceof GraphicalEditPart) {
                SetConstraintCommand c = createTogglePinCommand((GraphicalEditPart)child);
                if (c != null && EditPartUtil.isEditable(child))
                    resize.add(c);
            }
        }
        // do not provide a command if there were no submodules that can be pinned unpinned
        if (resize.size() < 1) return UnexecutableCommand.INSTANCE;

        return resize.unwrap();
    }

    /**
     * Generate a constraint change command in response to a pin/unpin request
     * @param request
     * @param child
     * @param toggleEnabled whether we want to toggle the state of the child
     * @param pinState if toggleEnabled was false, we can set the new pin state directly here
     * @return The created ConstraintCommand
     */
    protected SetConstraintCommand createTogglePinCommand(GraphicalEditPart child) {
        if (child instanceof SubmoduleEditPart) {
            SubmoduleEditPart submoduleEditPart = (SubmoduleEditPart) child;
            SubmoduleElementEx submodule = submoduleEditPart.getModel();
            float scale = submoduleEditPart.getScale();

            // create a command that deletes the location from the display string
            SetConstraintCommand cmd = new SetConstraintCommand(submodule, null);  //FIXME gyanus!!!!

            if (isChecked()) {
                // pin the module at the current temporary location if it is not fixed
                Point loc = ((SubmoduleFigure)child.getFigure()).getCenterPosition();
                PointF pos = PointF.fromPixels(loc, scale);
                cmd.setPinLocation(pos);
            }
            else {
                cmd.setPinLocation(null);
            }
            return cmd;
        }

        return null;
    }

}
