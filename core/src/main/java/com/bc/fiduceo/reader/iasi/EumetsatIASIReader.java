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

package com.bc.fiduceo.reader.iasi;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.reader.AcquisitionInfo;
import com.bc.fiduceo.reader.BoundingPolygonCreator;
import com.bc.fiduceo.reader.Reader;
import com.bc.fiduceo.reader.TimeLocator;
import com.bc.fiduceo.util.NetCDFUtils;
import com.bc.fiduceo.util.TimeUtils;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;


public class EumetsatIASIReader implements Reader {

    // @todo 3 tb/tb move to config file 2015-12-09
    private static final int GEO_INTERVAL_X = 6;
    private static final int GEO_INTERVAL_Y = 6;

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    private NetcdfFile netcdfFile = null;
    private BoundingPolygonCreator boundingPolygonCreator;

    EumetsatIASIReader(GeometryFactory geometryFactory) {
        final Interval interval = new Interval(GEO_INTERVAL_X, GEO_INTERVAL_Y);

        boundingPolygonCreator = new BoundingPolygonCreator(interval, geometryFactory);
    }

    @Override
    public void open(File file) throws IOException {
        netcdfFile = NetcdfFile.open(file.getPath());
    }

    @Override
    public void close() throws IOException {
        netcdfFile.close();
    }

    @Override
    public AcquisitionInfo read() throws IOException {
        final Date timeConverageStart = getGlobalAttributeAsDate("time_converage_start", netcdfFile);
        final Date timeConverageEnd = getGlobalAttributeAsDate("time_converage_end", netcdfFile);

        final Variable latVariable = netcdfFile.findVariable("lat");
        final Variable lonVariable = netcdfFile.findVariable("lon");
        final Array latArray = latVariable.read();
        final Array lonArray = lonVariable.read();

        final AcquisitionInfo acquisitionInfo = boundingPolygonCreator.createIASIBoundingPolygon((ArrayFloat.D2) latArray, (ArrayFloat.D2) lonArray);
        acquisitionInfo.setSensingStart(timeConverageStart);
        acquisitionInfo.setSensingStop(timeConverageEnd);
        return acquisitionInfo;
    }

    @Override
    public PixelLocator getPixelLocator() throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String getRegEx() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public PixelLocator getSubScenePixelLocator(Polygon sceneIndex) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public TimeLocator getTimeLocator() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Array readRaw(int centerX, int centerY, Interval interval, String variableName) throws IOException, InvalidRangeException {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public Array readScaled(int centerX, int centerY, Interval interval, String variableName) throws IOException, InvalidRangeException {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public ArrayInt.D2 readAcquisitionTime(int x, int y, Interval interval) throws IOException, InvalidRangeException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public List<Variable> getVariables() {
        throw new RuntimeException("not implemented");
    }

    static Date getGlobalAttributeAsDate(String timeCoverage, NetcdfFile netcdfFile) throws IOException {
        final String attributeString = NetCDFUtils.getGlobalAttributeString(timeCoverage, netcdfFile);
        return TimeUtils.parse(attributeString, DATE_FORMAT);
    }

    @Override
    public Dimension getProductSize() {
        throw new RuntimeException("Not yet implemented");
    }
}
