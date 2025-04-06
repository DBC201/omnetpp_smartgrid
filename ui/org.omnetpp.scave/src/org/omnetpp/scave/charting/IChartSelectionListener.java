/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.charting;

/**
 * Interface for listening of chart selection changes.
 *
 * @author tomi
 */
public interface IChartSelectionListener {
    public void selectionChanged(IPlotSelection selection);
}
