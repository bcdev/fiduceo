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
 */

package com.bc.fiduceo.post.plugin.distance;

import com.bc.fiduceo.math.Distance;
import com.bc.fiduceo.post.PostProcessing;
import com.bc.fiduceo.util.NetCDFUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.*;

import java.io.IOException;

class SphericalDistance extends PostProcessing {

    // @todo 3 tb/** maybe extract a configuration class here? 2016-12-23
    final String targetVarName;
    final String targetDataType;
    final String primLatVar;
    final String primLatScaleAttrName;
    final String primLatOffsetAttrName;
    final String primLonVar;
    final String primLonScaleAttrName;
    final String primLonOffsetAttrName;
    final String secoLatVar;
    final String secoLatScaleAttrName;
    final String secoLatOffsetAttrName;
    final String secoLonVar;
    final String secoLonScaleAttrName;
    final String secoLonOffsetAttrName;

    private final String matchupDimName = "matchup_count";

    SphericalDistance(String targetVarName, String targetDataType,
                      String primLatVar, String primLatScaleAttrName, String primLatOffsetAttrName,
                      String primLonVar, String primLonScaleAttrName, String primLonOffsetAttrName,
                      String secoLatVar, String secoLatScaleAttrName, String secoLatOffsetAttrName,
                      String secoLonVar, String secoLonScaleAttrName, String secoLonOffsetAttrName) {
        this.targetVarName = targetVarName;
        this.targetDataType = targetDataType;
        this.primLatVar = primLatVar;
        this.primLatScaleAttrName = primLatScaleAttrName;
        this.primLatOffsetAttrName = primLatOffsetAttrName;
        this.primLonVar = primLonVar;
        this.primLonScaleAttrName = primLonScaleAttrName;
        this.primLonOffsetAttrName = primLonOffsetAttrName;
        this.secoLatVar = secoLatVar;
        this.secoLatScaleAttrName = secoLatScaleAttrName;
        this.secoLatOffsetAttrName = secoLatOffsetAttrName;
        this.secoLonVar = secoLonVar;
        this.secoLonScaleAttrName = secoLonScaleAttrName;
        this.secoLonOffsetAttrName = secoLonOffsetAttrName;
    }

    @Override
    protected void prepare(NetcdfFile reader, NetcdfFileWriter writer) {
        writer.addVariable(null, targetVarName, DataType.getType(targetDataType), matchupDimName);
    }

    @Override
    protected void compute(NetcdfFile reader, NetcdfFileWriter writer) throws IOException, InvalidRangeException {
        final NetcdfFile netcdfFile = writer.getNetcdfFile();
        final int count = NetCDFUtils.getDimensionLength(matchupDimName, netcdfFile);

        final Variable targetVar = netcdfFile.findVariable(targetVarName);
        final Variable primLons = netcdfFile.findVariable(null, primLonVar);
        final Variable primLats = netcdfFile.findVariable(null, primLatVar);
        final Variable secoLons = netcdfFile.findVariable(null, secoLonVar);
        final Variable secoLats = netcdfFile.findVariable(null, secoLatVar);

        final Array p_lon = getCenterPosArray(primLons, primLonScaleAttrName, primLonOffsetAttrName);
        final Array p_lat = getCenterPosArray(primLats, primLatScaleAttrName, primLatOffsetAttrName);
        final Array s_lon = getCenterPosArray(secoLons, secoLonScaleAttrName, secoLonOffsetAttrName);
        final Array s_lat = getCenterPosArray(secoLats, secoLatScaleAttrName, secoLatOffsetAttrName);

        Array target = Array.factory(DataType.getType(targetDataType), new int[]{count});
        for (int i = 0; i < count; i++) {
            final double pLon = p_lon.getDouble(i);
            final double pLat = p_lat.getDouble(i);
            final double sLon = s_lon.getDouble(i);
            final double sLat = s_lat.getDouble(i);
            final double distanceKm = Distance.computeSpericalDistanceKm(pLon, pLat, sLon, sLat);
            target.setDouble(i, distanceKm);
        }
        writer.write(targetVar, target);
    }

    static double getValueFromAttribute(Variable variable, String attrName, final int defaultValue) {
        if (attrName != null) {
            final Attribute attribute = variable.findAttribute(attrName);
            if (attribute == null) {
                throw new RuntimeException("No attribute with name '" + attrName + "'.");
            }
            final Number number = attribute.getNumericValue();
            if (number == null) {
                throw new RuntimeException("Attribute '" + attrName + "' does not own a number value.");
            }
            return number.doubleValue();
        }
        return defaultValue;
    }

    private Array getCenterPosArray(Variable variable, String scaleAttrName, String offsetAttrName) throws IOException, InvalidRangeException {
        final int countIdx = variable.findDimensionIndex(matchupDimName);
        final int[] shape = variable.getShape();
        final int[] index = new int[shape.length];
        for (int i = 0; i < shape.length; i++) {
            int dimWith = shape[i];
            if (i != countIdx) {
                index[i] = dimWith / 2;
                shape[i] = 1;
            }
        }

        final Array array = variable.read(index, shape).reduce();

        double scaleFactor = getValueFromAttribute(variable, scaleAttrName, 1);
        double offset = getValueFromAttribute(variable, offsetAttrName, 0);
        if (scaleFactor != 1d || offset != 0d) {
            final MAMath.ScaleOffset scaleOffset = new MAMath.ScaleOffset(scaleFactor, offset);
            return MAMath.convert2Unpacked(array, scaleOffset);
        }
        return array;
    }
}
