/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.model.ex;

import java.util.HashMap;
import java.util.Map;

import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.interfaces.IHasName;
import org.omnetpp.ned.model.interfaces.IHasParameters;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.interfaces.INedTypeInfo;
import org.omnetpp.ned.model.pojo.LiteralElement;
import org.omnetpp.ned.model.pojo.ParamElement;
import org.omnetpp.ned.model.pojo.PropertyKeyElement;

/**
 * Extended parameter node
 *
 * @author rhornig
 */
public class ParamElementEx extends ParamElement implements IHasName {

    protected ParamElementEx() {
        super();
    }

    protected ParamElementEx(INedElement parent) {
        super(parent);
    }

    /**
     * Returns local properties of this element. The map is indexed with the
     * property name, or with name + ":" + index for properties that have
     * a non-empty index.
     *
     * Note about efficiency: the result is currently NOT cached, this method
     * will create a new map on each call.
     */
    public Map<String, PropertyElementEx> getLocalProperties() {
        Map<String, PropertyElementEx> result = new HashMap<String, PropertyElementEx>();
        for (INedElement child : this) {
            if (child instanceof PropertyElementEx) {
                PropertyElementEx property = (PropertyElementEx)child;
                if (StringUtils.isEmpty(property.getIndex()))
                    result.put(property.getName(), property);
                else
                    result.put(property.getName() + ":" + property.getIndex(), property);
            }
        }
        return result;
    }

    /**
     * Returns all inherited properties of this element. The map is indexed with the
     * property name, or with name + ":" + index for properties that have
     * a non-empty index.
     *
     * Note about efficiency: the result is currently NOT cached, this method
     * will create a new map on each call.
     */
    public Map<String, PropertyElementEx> getProperties() {
        String name = getName();
        Map<String, PropertyElementEx> result = new HashMap<String, PropertyElementEx>();
        INedTypeElement typeElement = getEnclosingTypeElement();

        if (typeElement != null) {
            INedTypeInfo typeInfo = typeElement.getNedTypeInfo();

            if (typeInfo != null) {
                for (INedTypeInfo superTypeElement : typeInfo.getInheritanceChain()) {
                    ParamElementEx paramElement = superTypeElement.getParamDeclarations().get(name);

                    if (paramElement != null)
                        result.putAll(paramElement.getLocalProperties());
                }
            }
        }

        return result;
    }

    public String getUnit() {
        PropertyElementEx propertyElement = getProperties().get("unit");

        if (propertyElement == null)
            return null;
        else {
            PropertyKeyElement propertyKey = propertyElement.getFirstPropertyKeyChild();

            if (propertyKey == null)
                return null;
            else {
                LiteralElement literal = propertyKey.getFirstLiteralChild();

                if (literal == null)
                    return null;
                else
                    return literal.getValue();
            }
        }
    }

    public IHasParameters getOwner() {
        return (IHasParameters)getParent().getParent();
    }
}
