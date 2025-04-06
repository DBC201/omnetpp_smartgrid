/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.util;

import java.io.Serializable;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Utility class to group two values.
 * Mainly used to return two values from a method.
 *
 * @author tomi
 */
public class Pair<T1,T2> implements Serializable {
    private static final long serialVersionUID = 1L;

    public T1 first;
    public T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public static <T1,T2> Pair<T1,T2> pair(T1 first, T2 second) {
        return new Pair<T1,T2>(first, second);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCode(first) ^ ObjectUtils.hashCode(second);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        Pair<?,?> otherPair = (Pair<?,?>)other;
        return ObjectUtils.equals(first, otherPair.first) &&
                ObjectUtils.equals(second, otherPair.second);
    }

    @Override
    public String toString() {
        return "(" + first.toString() + ", " + second.toString() + ")";
    }
}
