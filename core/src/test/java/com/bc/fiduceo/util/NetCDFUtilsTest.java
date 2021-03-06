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

package com.bc.fiduceo.util;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.netcdf3.N3iosp;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetCDFUtilsTest {

    @Test
    public void testGetDefaultFillValue_Array_Double() throws Exception {
        final Array array = mock(Array.class);
        when(array.getDataType()).thenReturn(DataType.DOUBLE);

        final Number value = NetCDFUtils.getDefaultFillValue(array);
        assertEquals(N3iosp.NC_FILL_DOUBLE, value);
    }

    @Test
    public void testGetDefaultFillValue_Aray_Long() throws Exception {
        final Array array = mock(Array.class);
        when(array.getDataType()).thenReturn(DataType.LONG);

        final Number value = NetCDFUtils.getDefaultFillValue(array);
        assertEquals(N3iosp.NC_FILL_LONG, value);
    }

    @Test
    public void testToFloat() {
        final int[] ints = {12, 23, 45, 67};
        final Array intArray = Array.factory(ints);

        final Array floatArray = NetCDFUtils.toFloat(intArray);
        assertNotNull(floatArray);
        assertEquals(float.class, floatArray.getDataType().getPrimitiveClassType());
        assertEquals(12.0, floatArray.getFloat(0), 1e-8);
        assertEquals(23.0, floatArray.getFloat(1), 1e-8);
        assertEquals(45.0, floatArray.getFloat(2), 1e-8);
        assertEquals(67.0, floatArray.getFloat(3), 1e-8);
    }

    @Test
    public void testGetGlobalAttributeString() throws IOException {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);
        final Attribute attribute = mock(Attribute.class);

        when(attribute.getStringValue()).thenReturn("theValue");
        when(netcdfFile.findGlobalAttribute("the_attribute")).thenReturn(attribute);

        final String value = NetCDFUtils.getGlobalAttributeString("the_attribute", netcdfFile);
        assertEquals("theValue", value);
    }

    @Test
    public void testGetGlobalAttributeString_missingAttribute() throws IOException {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);

        when(netcdfFile.findGlobalAttribute("the_attribute")).thenReturn(null);

        try {
            NetCDFUtils.getGlobalAttributeString("the_attribute", netcdfFile);
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    @Test
    public void testGetFillValue_fromAttribute() {
        final Variable variable = mock(Variable.class);
        final Attribute attribute = mock(Attribute.class);
        when(attribute.getNumericValue()).thenReturn(19);
        when(variable.findAttribute("_FillValue")).thenReturn(attribute);

        final Number fillValue = NetCDFUtils.getFillValue(variable);
        assertEquals(19, fillValue.intValue());
    }

    @Test
    public void testGetFillValue_fromDataType() {
        final Variable variable = mock(Variable.class);
        when(variable.getDataType()).thenReturn(DataType.FLOAT);

        final Number fillValue = NetCDFUtils.getFillValue(variable);
        assertEquals(N3iosp.NC_FILL_FLOAT, fillValue.floatValue(), 1e-8);
    }

    @Test
    public void testGetDimensionSize() {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);
        final Dimension dimension = mock(Dimension.class);
        when(dimension.getLength()).thenReturn(22);
        when(netcdfFile.findDimension("matchup_count")).thenReturn(dimension);

        assertEquals(22, NetCDFUtils.getDimensionLength("matchup_count", netcdfFile));
    }

    @Test
    public void testGetDimensionSize_dimensionNotPresent() {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);

        try {
            NetCDFUtils.getDimensionLength("matchup_count", netcdfFile);
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }
    }
}
