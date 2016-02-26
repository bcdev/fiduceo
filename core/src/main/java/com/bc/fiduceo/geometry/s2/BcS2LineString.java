
/*
 * Copyright (C) 2015 Brockmann Consult GmbH
 * This code was developed for the EC project "Fidelity and Uncertainty in
 * Climate Data Records from Earth Observations (FIDUCEO)".
 * Grant Agreement: 638822
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * A copy of the GNU General Public License should have been supplied along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package com.bc.fiduceo.geometry.s2;


import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.LineString;
import com.bc.fiduceo.geometry.Point;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2Polyline;

class BcS2LineString implements LineString {

    private final S2Polyline googleLineString;

    BcS2LineString(S2Polyline googleLineString) {
        this.googleLineString = googleLineString;
    }

    @Override
    public String toString() {
        // @todo 3 tb/** make this class return WKT 2016-02-26
        return googleLineString.toString();
    }

    @Override
    public Geometry getIntersection(Geometry other) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean isEmpty() {
        return googleLineString.numVertices() <= 0;
    }

    @Override
    public Point[] getCoordinates() {
        final int numVertices = googleLineString.numVertices();
        final Point[] coordinates = new Point[numVertices];
        for (int i = 0; i < numVertices; i++) {
            final S2Point googlePoint = googleLineString.vertex(i);
            coordinates[i] = new BcS2Point(new S2LatLng(googlePoint));
        }
        return coordinates;
    }

    @Override
    public Object getInner() {
        return googleLineString;
    }
}