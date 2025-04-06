/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.properties.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.omnetpp.ned.model.INedElement;

/**
 * Merges several IPropertySource into a single PropertySource (flattens structure)
 *
 * @author rhornig
 */
// TODO: Use org.omnetpp.common.properties.MergedPropertySource
public class MergedPropertySource extends NedBasePropertySource {
    public static final String BASE_CATEGORY = "Base";

    List<IPropertySource> mergedList = new ArrayList<IPropertySource>();

    private boolean readOnly = false;

    public MergedPropertySource(INedElement model) {
        super(model);
    }

    /**
     * Adds a property source to be merged into this
     * @param ps
     */
    public void mergePropertySource(IPropertySource ps) {
        mergedList.add(ps);
    }

    public boolean isPropertyResettable(Object id) {
        // ask all registered PS if it knows about this
        for (IPropertySource psrc : mergedList)
            if (psrc instanceof IPropertySource2)
                if (((IPropertySource2)psrc).isPropertyResettable(id))
                    return true;

        return false;
    }

    public boolean isPropertySet(Object id) {
        // ask all registered PS if it knows about this
        for (IPropertySource psrc : mergedList)
            if (psrc instanceof IPropertySource2)
                if (((IPropertySource2)psrc).isPropertySet(id))
                    return true;

        return false;
    }

    public Object getEditableValue() {
        // by default return ourselves
        // TODO or maybe a copy of ourselves ??? Test with editing multiple objects
        return this;
    }

    public IPropertyDescriptor[] getPropertyDescriptors() {
        List<IPropertyDescriptor> mergedPDList = new ArrayList<IPropertyDescriptor>();
        // first we check whether the model element is inside the model (if not we should not
        // provide descriptors) ie. property editor is available ONLY for elements IN the model
        if (getModel().getParent() == null)
            return new IPropertyDescriptor[0];

        // walk through all property source and merge its descriptors into a single list
        for (IPropertySource psrc : mergedList)
            for (IPropertyDescriptor pdesc : psrc.getPropertyDescriptors()) {
                if (readOnly && (pdesc.getClass()!=PropertyDescriptor.class)) {
                    // if we are read only, replace the property descriptor with a readonly one
                    PropertyDescriptor readOnlyPDesc =
                        new PropertyDescriptor(pdesc.getId(), pdesc.getDisplayName());
                    readOnlyPDesc.setCategory(pdesc.getCategory());
                    readOnlyPDesc.setDescription(pdesc.getDescription()+" - (read only)");
                    mergedPDList.add(readOnlyPDesc);
                } else
                    mergedPDList.add(pdesc);
            }

        return mergedPDList.toArray(new IPropertyDescriptor[mergedPDList.size()]);
    }

    public Object getPropertyValue(Object id) {
        // ask all registered property sources for the given value
        for (IPropertySource psrc : mergedList) {
            Object result = psrc.getPropertyValue(id);
            if (result != null) return result;
        }

        return null;
    }

    public void resetPropertyValue(Object id) {
        // try to reset the value on all registered property source
        for (IPropertySource psrc : mergedList)
            psrc.resetPropertyValue(id);
    }

    public void setPropertyValue(Object id, Object value) {
        // try to set the value on all registered property source
        for (IPropertySource psrc : mergedList)
            psrc.setPropertyValue(id, value);
    }

    /**
     * Sets the property source's read only state
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

}
