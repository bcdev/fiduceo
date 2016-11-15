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

package com.bc.fiduceo;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.TimeAxis;
import com.bc.fiduceo.reader.AcquisitionInfo;
import com.bc.fiduceo.reader.Reader;
import com.bc.fiduceo.reader.ReaderFactory;
import com.bc.fiduceo.reader.TimeLocator;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class TimeDiffOrbitCalculator {

    public static void main(String[] args) throws IOException, InvalidRangeException {
        final String productType = args[0];

        final GeometryFactory geometryFactory = new GeometryFactory(GeometryFactory.Type.S2);
        final ReaderFactory readerFactory = ReaderFactory.get(geometryFactory);

        final float[] minDelta = new float[181];
        final float[] maxDelta = new float[181];
        for (int i = 0; i < 181; i++) {
            minDelta[i] = Float.MAX_VALUE;
            maxDelta[i] = Float.MIN_VALUE;
        }
        final Interval interval = new Interval(1, 1);

        try (Reader reader = readerFactory.getReader(productType)) {
            reader.open(new File(args[1]));

            final Dimension productSize = reader.getProductSize();
            final TimeLocator timeLocator = reader.getTimeLocator();
            final AcquisitionInfo acquisitionInfo = reader.read();

            final TimeAxis[] timeAxes = acquisitionInfo.getTimeAxes();
            TimeAxis timeAxis = timeAxes[0];

            for (int line = 0; line < productSize.getNy(); line++) {
                if (timeAxes.length > 1) {
                    if (line > (productSize.getNy() / 2)) {
                        timeAxis = timeAxes[1];
                    }
                }

                for (int x = 0; x <  productSize.getNx(); x++) {
                    final long acquisitionTime = timeLocator.getTimeFor(x, line);

                    final Array lonArray = reader.readRaw(x, line, interval, "lon");
                    final Index lonIndex = lonArray.getIndex();
                    final float lon = lonArray.getFloat(lonIndex);

                    final Array latArray = reader.readRaw(x, line, interval, "lat");
                    final Index latIndex = latArray.getIndex();
                    final float lat = latArray.getFloat(latIndex);

                    int latPositionRastered = 90 + (int) Math.round(lat);

                    final Date estimatedTIme = timeAxis.getTime(geometryFactory.createPoint(lon, lat));

                    final float deltaTime = (acquisitionTime - estimatedTIme.getTime()) / 1000.f;
                    final float absDelta = Math.abs(deltaTime);

                    if (absDelta < minDelta[latPositionRastered]) {
                        minDelta[latPositionRastered] = absDelta;
                    }

                    if (absDelta > maxDelta[latPositionRastered]) {
                        maxDelta[latPositionRastered] = absDelta;
                    }
                }

                System.out.println("line = " + line );
            }

            System.out.print("[");
            for (int x = 0; x < 181; x++) {
                if (minDelta[x] > 10000) {
                    minDelta[x] = 0;
                }
                System.out.print(minDelta[x]);
                System.out.print(" ");
            }
            System.out.println("]");
            System.out.print("[");
            for (int x = 0; x < 181; x++) {
                if (maxDelta[x] < -10000) {
                    maxDelta[x] = 0;
                }
                System.out.print(maxDelta[x]);
                System.out.print(" ");
            }
            System.out.println("]");

        }
    }
}
