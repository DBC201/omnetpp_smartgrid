/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.editors;

import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.omnetpp.common.properties.PropertySource;
import org.omnetpp.scave.engine.ResultFileManager;
import org.omnetpp.scave.model.Chart;
import org.omnetpp.scave.model2.ChartLine;
import org.omnetpp.scave.model2.ResultItemRef;

/**
 * Provides properties of Scave model objects and charts.
 *
 * @author tomi
 */
public class ScavePropertySourceProvider implements IPropertySourceProvider {

    private ResultFileManager manager;

    public ScavePropertySourceProvider(ResultFileManager manager) {
        this.manager = manager;
    }

    @Override
    public IPropertySource getPropertySource(final Object object) {
        if (object instanceof PropertySource)
            return (PropertySource)object;
        else if (object instanceof Chart)
            return new ChartPropertySource((Chart)object);
        else if (object instanceof ChartLine) {
            ChartLine lineID = (ChartLine)object;
            // TODO
//            ChartProperties properties = ChartProperties.createPropertySource(lineID.getChart(), manager);
//            if (properties instanceof VectorChartProperties)
//                return ((VectorChartProperties)properties).getLineProperties(lineID.getKey());
        }
        else if (object instanceof ResultItemRef)
            return new ResultItemPropertySource((ResultItemRef)object);
        return null; // TODO ?
    }
}
