/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.canvas;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Rectangular area with double precision coordinates.
 * Used by {@link ZoomableCachingCanvas}.
 *
 * @author tomi
 */
public class RectangularArea {

    public double minX;
    public double minY;
    public double maxX;
    public double maxY;

    public RectangularArea() {
    }

    public RectangularArea(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public double width() {
        return maxX - minX;
    }

    public double height() {
        return maxY - minY;
    }

    public boolean contains(double x, double y) {
        return minX <= x && x <= maxX && minY <= y && y <= maxY;
    }

    public boolean isFinite() {
        return !Double.isInfinite(minX) && !Double.isInfinite(minY) &&
                !Double.isInfinite(maxX) && !Double.isInfinite(maxY);
    }

    public RectangularArea unionWith(RectangularArea other) {
        minX = Math.min(minX, other.minX);
        minY = Math.min(minY, other.minY);
        maxX = Math.max(maxX, other.maxX);
        maxY = Math.max(maxY, other.maxY);
        return this;
    }

    public RectangularArea intersect(RectangularArea other) {
        return new RectangularArea(
                Math.max(minX, other.minX),
                Math.max(minY, other.minY),
                Math.min(maxX, other.maxX),
                Math.min(maxY, other.maxY)
                );
    }

    public String toString() {
        return String.format("Area(%s,%s,%s,%s)", minX, minY, maxX, maxY);
    }

    public static RectangularArea fromString(String str) {
        Scanner s = new Scanner(str);
        try {
            s.useDelimiter("[,()]");
            s.next("Area");
            RectangularArea area = new RectangularArea();
            area.minX = s.nextDouble();
            area.minY = s.nextDouble();
            area.maxX = s.nextDouble();
            area.maxY = s.nextDouble();
            return area;
        } catch (NoSuchElementException e) {
            return null;
        } finally {
            s.close();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(maxX);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(maxY);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minX);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minY);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final RectangularArea other = (RectangularArea) obj;
        if (Double.doubleToLongBits(maxX) != Double
                .doubleToLongBits(other.maxX))
            return false;
        if (Double.doubleToLongBits(maxY) != Double
                .doubleToLongBits(other.maxY))
            return false;
        if (Double.doubleToLongBits(minX) != Double
                .doubleToLongBits(other.minX))
            return false;
        if (Double.doubleToLongBits(minY) != Double
                .doubleToLongBits(other.minY))
            return false;
        return true;
    }
}