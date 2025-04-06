/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.eventlog;

import java.util.ArrayList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.omnetpp.eventlog.EventLogEntry;
import org.omnetpp.eventlog.engine.PStringVector;

public class EventLogEntryPropertySource implements IPropertySource {
    private EventLogEntry eventLogEntry;

    public EventLogEntryPropertySource(EventLogEntry eventLogEntry) {
        this.eventLogEntry = eventLogEntry;
    }

    public Object getEditableValue() {
        return null;
    }

    public IPropertyDescriptor[] getPropertyDescriptors() {
        ArrayList<String> names = eventLogEntry.getAttributeNames();
        IPropertyDescriptor[] descriptors = new IPropertyDescriptor[(int) names.size()];

        for (int i = 0; i < names.size(); i++) {
            final String name = names.get(i);
            descriptors[i] = new PropertyDescriptor(name, name);
        }

        return descriptors;
    }

    public Object getPropertyValue(Object id) {
        return eventLogEntry.getAsString((String)id);
    }

    public void setPropertyValue(Object id, Object value) {
        throw new RuntimeException();
    }

    public boolean isPropertySet(Object id) {
        return true;
    }

    public void resetPropertyValue(Object id) {
        throw new RuntimeException();
    }
}
