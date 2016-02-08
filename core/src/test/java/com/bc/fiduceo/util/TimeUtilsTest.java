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

import com.bc.fiduceo.TestUtil;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TimeUtilsTest {

    @Test
    public void testCreate_fromMillisSinceEpoch() {
        Date date = TimeUtils.create(1435000000000L);
        assertNotNull(date);
        TestUtil.assertCorrectUTCDate(2015, 6, 22, 19, 6, 40, 0, date);

        date = TimeUtils.create(1435100000000L);
        assertNotNull(date);
        TestUtil.assertCorrectUTCDate(2015, 6, 23, 22, 53, 20, 0, date);
    }

    @Test
    public void testFormat() {
        final Date date = TimeUtils.create(1435000000000L);

        final String formatted = TimeUtils.format(date);
        assertNotNull(formatted);
        assertEquals("22-Jun-2015 19:06:40", formatted);
    }

    @Test
    public void testFormat_withPattern() {
        final Date date = TimeUtils.create(1435000000123L);

        final String formatted = TimeUtils.format(date, "yyyy-MM-dd HH:mm:ss.S");
        assertNotNull(formatted);
        assertEquals("2015-06-22 19:06:40.123", formatted);
    }

    @Test
    public void testTimeStampToDate() {
        final Timestamp timestamp = new Timestamp(1435000000234L);

        final Date date = TimeUtils.toDate(timestamp);
        assertNotNull(date);
        assertEquals("2015-06-22 19:06:40.234", TimeUtils.format(date, "yyyy-MM-dd HH:mm:ss.S"));
    }

    @Test
    public void testToTimeStamp() {
        final Date date = TimeUtils.create(1435100000345L);

        final Timestamp timestamp = TimeUtils.toTimestamp(date);
        assertEquals(1435100000345L, timestamp.getTime());
    }
}
