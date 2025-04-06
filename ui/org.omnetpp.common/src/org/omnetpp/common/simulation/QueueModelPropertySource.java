package org.omnetpp.common.simulation;

import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.omnetpp.common.properties.PropertySource;

public class QueueModelPropertySource extends PropertySource {
    private static final String PROPERTY_NAME               = "Id";
    private static final String PROPERTY_SIZE               = "Size";
    private static final String PROPERTY_COUNT              = "Count";
    private static final String PROPERTY_OWNER_MODULE       = "OwnerModule";

    private IPropertySourceProvider propertySourceProvider;
    private QueueModel simulationQueue;

    public QueueModelPropertySource(IPropertySourceProvider propertySourceProvider, QueueModel simulationQueue) {
        this.propertySourceProvider = propertySourceProvider;
        this.simulationQueue = simulationQueue;
    }

    @Override
    public Object getEditableValue() {
        return simulationQueue.getName();
    }

    @org.omnetpp.common.properties.Property(id = PROPERTY_NAME, category = "Name")
    public String getName() { return simulationQueue.getName(); }

    @org.omnetpp.common.properties.Property(id = PROPERTY_SIZE)
    public int getSize() { return simulationQueue.getSize(); }

    @org.omnetpp.common.properties.Property(id = PROPERTY_COUNT)
    public int getCount() { return simulationQueue.getCount(); }

    @org.omnetpp.common.properties.Property(id = PROPERTY_OWNER_MODULE)
    public IPropertySource getOwnerModule() { return propertySourceProvider.getPropertySource(simulationQueue.getOwnerModule()); }
}
