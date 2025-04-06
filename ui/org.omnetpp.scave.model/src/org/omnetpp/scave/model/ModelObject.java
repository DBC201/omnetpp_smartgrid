/*--------------------------------------------------------------*
  Copyright (C) 2006-2020 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.scave.model;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;

public abstract class ModelObject implements Cloneable {
    protected ModelObject parent;

    ListenerList<IModelChangeListener> listeners = new ListenerList<IModelChangeListener>();

    public ModelObject getParent() {
        return parent;
    }

    public void addListener(IModelChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IModelChangeListener listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners() {
        notifyListeners(new ModelChangeEvent(this));
    }

    protected void notifyListeners(ModelChangeEvent event) {
        listeners.forEach((l) -> l.modelChanged(event) );
        if (parent != null)
            parent.notifyListeners(event);
    }

    public ModelObject dup() {
        try {
            return clone();
        } catch (CloneNotSupportedException e) {
            // this cannot happen
            throw new RuntimeException(e);
        }
    }

    protected void detectCycles() {
        ModelObject o = this;
        while (o.parent != null) {
            o = o.parent;
            Assert.isTrue(o != this, "cycle detected");
        }
    }

    @Override
    protected ModelObject clone() throws CloneNotSupportedException {
        ModelObject clone = (ModelObject)super.clone();
        clone.parent = null;
        clone.listeners = new ListenerList<IModelChangeListener>();
        return clone;
    }
}
