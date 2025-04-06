/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting;

import java.math.BigDecimal;

/**
 * Interface for enumerating the ticks.
 *
 * @author tomi
 */
public interface ITicks extends Iterable<BigDecimal> {

    boolean isMajorTick(BigDecimal d);
}
