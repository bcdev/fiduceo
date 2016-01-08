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
package com.bc.fiduceo.reader;

import com.bc.fiduceo.IOTestRunner;
import com.bc.fiduceo.TestUtil;
import com.bc.fiduceo.geometry.Point;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author muhammad.bc
 */
@RunWith(IOTestRunner.class)
public class AMSU_MHS_Reader_IO_Test {

    private static final DateFormat DATEFORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private AMSU_MHS_Reader reader;
    private File testDataDirectory;
    private File file;

    @Before
    public void setUp() throws IOException {
        reader = new AMSU_MHS_Reader();
        testDataDirectory = TestUtil.getTestDataDirectory();
    }

    @Test
    public void testOpenHDF5() throws IOException {
        file = new File(testDataDirectory, "fiduceo_test_product_AMSU_B.h5");
        reader.open(file);
        reader.close();
    }

    @Test
    public void testGetElementValues() throws IOException {
        file = new File(testDataDirectory, "fiduceo_test_product_AMSU_B.h5");
        reader.open(file);
        AcquisitionInfo read = reader.read();
        Assert.assertNotNull(read.getSensingStart());
        Assert.assertNotNull(read.getSensingStop());
        List<Point> coordinates = read.getCoordinates();
        Assert.assertNotNull(coordinates);

    }

    @Test
    public void testTime() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        Date time = new SimpleDateFormat("HHmmssSSSSSS").parse("83730128");
        calendar.setTime(time);
        calendar.set(Calendar.YEAR, 2015);
        calendar.set(Calendar.DAY_OF_YEAR, 347);

        String hour = String.valueOf(calendar.get(Calendar.HOUR));
        String min = String.valueOf(calendar.get(Calendar.MINUTE));
        String second = String.valueOf(calendar.get(Calendar.SECOND));
        String mlSecond = String.valueOf(calendar.get(Calendar.MILLISECOND));
        String dy = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        String mn = String.valueOf(calendar.get(Calendar.MONTH));
        String yr = String.valueOf(calendar.get(Calendar.YEAR));
        Date date = DATEFORMAT.parse(yr + "-" + mn + "-" + dy + " " + hour + ":" + min + ":" + second + "." + mlSecond);
        Assert.assertNotNull(date);
    }

    @After
    public void testCloseHDF5() throws IOException {
        file = new File(testDataDirectory, "fiduceo_test_product_AMSU_B.h5");
        reader.open(file);
        reader.close();
    }
}
