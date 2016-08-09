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

package com.bc.fiduceo.reader.atsr;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.LineString;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.reader.*;
import com.bc.fiduceo.util.TimeUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.dataio.envisat.EnvisatConstants;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ATSR_L1B_Reader implements Reader {

    private static final Interval INTERVAL = new Interval(5, 20);

    private final GeometryFactory geometryFactory;

    private Product product;

    ATSR_L1B_Reader(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    @Override
    public void open(File file) throws IOException {
        product = ProductIO.readProduct(file, EnvisatConstants.ENVISAT_FORMAT_NAME);
        if (product == null) {
            throw new IOException("Unable to read ATSR product: " + file.getAbsolutePath());
        }
    }

    @Override
    public void close() throws IOException {
        if (product != null) {
            product.dispose();
            product = null;
        }
    }

    @Override
    public AcquisitionInfo read() throws IOException {
        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();

        extractSensingTimes(acquisitionInfo);

        acquisitionInfo.setNodeType(NodeType.UNDEFINED);

        final Geometries geometries = calculateGeometries();
        acquisitionInfo.setBoundingGeometry(geometries.getBoundingGeometry());
        ReaderUtils.setTimeAxes(acquisitionInfo, geometries, geometryFactory);

        return acquisitionInfo;
    }

    @Override
    public String getRegEx() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public PixelLocator getPixelLocator() throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public PixelLocator getSubScenePixelLocator(Polygon sceneGeometry) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public TimeLocator getTimeLocator() throws IOException {
        return new ATSR_TimeLocator(product);
    }

    @Override
    public Array readRaw(int centerX, int centerY, Interval interval, String variableName) throws IOException, InvalidRangeException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Array readScaled(int centerX, int centerY, Interval interval, String variableName) throws IOException, InvalidRangeException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ArrayInt.D2 readAcquisitionTime(int x, int y, Interval interval) throws IOException, InvalidRangeException {
        final int width = interval.getX();
        final int height = interval.getY();
        final int[] timeArray = new int[width * height];

        final PixelPos pixelPos = new PixelPos();
        final TimeCoding sceneTimeCoding = product.getSceneTimeCoding();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int halfHeight = height / 2;
        final int halfWidth = width / 2;
        int writeOffset = 0;
        for (int yRead = y - halfHeight; yRead <= y + halfHeight; yRead++) {
            int lineTimeSeconds = Integer.MIN_VALUE;
            if (yRead >= 0 && yRead < sceneRasterHeight) {
                pixelPos.setLocation(x, yRead + 0.5);
                final double lineMjd = sceneTimeCoding.getMJD(pixelPos);
                final long lineTime = TimeUtils.mjd2000ToDate(lineMjd).getTime();
                lineTimeSeconds = (int) Math.round(lineTime / 1000.0);
            }

            for (int xRead = x - halfWidth; xRead <= x + halfWidth; xRead++) {
                if (xRead >= 0 && xRead < sceneRasterWidth) {
                    timeArray[writeOffset] = lineTimeSeconds;
                } else {
                    timeArray[writeOffset] = Integer.MIN_VALUE;
                }
                ++writeOffset;
            }
        }

        final int[] shape = new int[2];
        shape[0] = height;
        shape[1] = width;

        return (ArrayInt.D2) Array.factory(DataType.INT, shape, timeArray);
    }

    @Override
    public List<Variable> getVariables() throws InvalidRangeException {
        final List<Variable> result = new ArrayList<>();

        final Band[] bands = product.getBands();
        for (final Band band : bands) {
            final VariableProxy variableProxy = new VariableProxy(band);
            result.add(variableProxy);
        }

        final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        for (final TiePointGrid tiePointGrid : tiePointGrids) {
            final VariableProxy variableProxy = new VariableProxy(tiePointGrid);
            result.add(variableProxy);
        }

        return result;
    }

    @Override
    public Dimension getProductSize() throws IOException {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        return new Dimension("product_size", width, height);
    }

    private void extractSensingTimes(AcquisitionInfo acquisitionInfo) {
        final ProductData.UTC startTime = product.getStartTime();
        acquisitionInfo.setSensingStart(startTime.getAsDate());

        final ProductData.UTC endTime = product.getEndTime();
        acquisitionInfo.setSensingStop(endTime.getAsDate());
    }

    private Geometries calculateGeometries() throws IOException {
        final Geometries geometries = new Geometries();

        final TiePointGrid longitude = product.getTiePointGrid("longitude");
        final TiePointGrid latitude = product.getTiePointGrid("latitude");

        final int[] shape = new int[2];
        shape[0] = longitude.getGridHeight();
        shape[1] = longitude.getGridWidth();

        final DataType netcdfDataType = ReaderUtils.getNetcdfDataType(longitude.getDataType());
        if (netcdfDataType == null) {
            throw new IOException("Unsupported data type: " + longitude.getDataType());
        }

        final ProductData longitudeGridData = longitude.getGridData();
        final ProductData latitudeGridData = latitude.getGridData();
        final Array lonArray = Array.factory(netcdfDataType, shape, longitudeGridData.getElems());
        final Array latArray = Array.factory(netcdfDataType, shape, latitudeGridData.getElems());

        final BoundingPolygonCreator boundingPolygonCreator = new BoundingPolygonCreator(INTERVAL, geometryFactory);
        final Geometry boundingGeometry = boundingPolygonCreator.createBoundingGeometry(lonArray, latArray);
        if (!boundingGeometry.isValid()) {
            throw new IOException("Invalid bounding geometry: implement splitted approach then");
        }
        geometries.setBoundingGeometry(boundingGeometry);

        final LineString timeAxisGeometry = boundingPolygonCreator.createTimeAxisGeometry(lonArray, latArray);
        geometries.setTimeAxesGeometry(timeAxisGeometry);

        return geometries;
    }
}