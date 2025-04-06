/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.model2;

import org.omnetpp.common.engine.BigDecimal;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.model.Chart;

/**
 * Identifies a data point on a line chart.
 *
 * @author tomi
 */
//TODO this class is currently unused, delete?
public class ChartDataPoint extends ChartLine {

    private int index;
    private long eventNum;
    private BigDecimal preciseX;
    private double x,y;

    public ChartDataPoint(Chart chart, int series, String seriesKey, long id, int index, long eventNum,
            BigDecimal preciseX, double x, double y, ResultFileManager manager) {
        super(chart, series, seriesKey, id, manager);
        this.index = index;
        this.eventNum = eventNum;
        this.preciseX = preciseX;
        this.x = x;
        this.y = y;
    }

    public int getIndex() {
        return index;
    }

    public long getEventNum() {
        return eventNum;
    }

    public BigDecimal getPreciseX() {
        return preciseX;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
