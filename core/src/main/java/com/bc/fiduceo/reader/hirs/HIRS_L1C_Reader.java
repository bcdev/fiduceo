/*
 * Copyright (C) 2016 Brockmann Consult GmbH
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

package com.bc.fiduceo.reader.hirs;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.reader.AcquisitionInfo;
import com.bc.fiduceo.reader.ArrayCache;
import com.bc.fiduceo.reader.BoundingPolygonCreator;
import com.bc.fiduceo.reader.Reader;
import com.bc.fiduceo.reader.TimeLocator;
import com.bc.fiduceo.util.TimeUtils;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class HIRS_L1C_Reader implements Reader {

    private final GeometryFactory geometryFactory;

    private NetcdfFile netcdfFile;

    private ArrayCache arrayCache;

    public HIRS_L1C_Reader(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    @Override
    public void open(File file) throws IOException {
        netcdfFile = NetcdfFile.open(file.getPath());
        arrayCache = new ArrayCache(netcdfFile);
    }

    @Override
    public void close() throws IOException {
        arrayCache = null;
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    @Override
    public AcquisitionInfo read() throws IOException {
        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();

        setSensingTimes(acquisitionInfo);

        acquisitionInfo.setNodeType(NodeType.UNDEFINED);

        // @todo 1 tb/tb continue here 2016-08-01
        final BoundingPolygonCreator boundingPolygonCreator = new BoundingPolygonCreator(new Interval(4, 10), geometryFactory);
        final Array lon = arrayCache.get("lon");
        final Array lat = arrayCache.get("lat");
        final Geometry boundingGeometry = boundingPolygonCreator.createBoundingGeometry(lon, lat);


        return acquisitionInfo;
    }

    @Override
    public String getRegEx() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public PixelLocator getPixelLocator() throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public PixelLocator getSubScenePixelLocator(Polygon sceneIndex) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public TimeLocator getTimeLocator() throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public Array readRaw(int centerX, int centerY, Interval interval, String variableName) throws IOException, InvalidRangeException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public Array readScaled(int centerX, int centerY, Interval interval, String variableName) throws IOException, InvalidRangeException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public ArrayInt.D2 readAcquisitionTime(int x, int y, Interval interval) throws IOException, InvalidRangeException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public List<Variable> getVariables() throws InvalidRangeException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public Dimension getProductSize() {
        throw new IllegalStateException("not implemented");
    }

    private void setSensingTimes(AcquisitionInfo acquisitionInfo) throws IOException {
        final Array time = arrayCache.get("time");
        final long startMillisSince1970 = time.getLong(0) * 1000;
        final Date sensingStart = TimeUtils.create(startMillisSince1970);
        acquisitionInfo.setSensingStart(sensingStart);

        final int[] shape = time.getShape();
        final long stopMillisSince1970 = time.getLong(shape[0] - 1) * 1000;
        final Date sensingStop = TimeUtils.create(stopMillisSince1970);
        acquisitionInfo.setSensingStop(sensingStop);
    }
}