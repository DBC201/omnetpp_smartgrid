/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model.interfaces;

import java.util.List;

import org.omnetpp.ned.model.ex.ConnectionElementEx;

/**
 * Marker interface for elements that can be connected to each other (compound module and submodules)
 *
 * @author rhornig
 */
public interface IConnectableElement extends IHasResolver, IHasName, IHasProperties, IHasParameters, IHasGates, IHasDisplayString {
    /**
     * Returns the typeinfo for the effective type.
     *
     * Returns null if the effective type is not filled in, or is not a valid NED type,
     * or not a type that's accepted at the given place (e.g. a channel for submodule type).
     */
    public INedTypeInfo getNedTypeInfo();

    /**
     * Returns ALL VALID connections contained in / and inherited by
     * this module where this module is the source.
     *
     * IMPORTANT: Cannot use this method to get the connections inside a compound module
     * where this module is an inherited submodule! The returned list won't contain
     * connections added in the derived compound module. Use editparts instead.
     */
    public List<ConnectionElementEx> getSrcConnections();

    /**
     * Returns ALL VALID connections contained in / and inherited by
     * this module where this module is the destination.
     *
     * IMPORTANT: Cannot use this method to get the connections inside a compound module
     * where this module is an inherited submodule! The returned list won't contain
     * connections added in the derived compound module. Use editparts instead.
     */
    public List<ConnectionElementEx> getDestConnections();

}