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
import com.bc.fiduceo.location.PixelLocator;
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


public class TimeDiffCalculator {

    public static void main(String[] args) throws IOException, InvalidRangeException {
        final String productType = args[0];

        final GeometryFactory geometryFactory = new GeometryFactory(GeometryFactory.Type.S2);
        final ReaderFactory readerFactory = ReaderFactory.get(geometryFactory);
        final int lineNumber = Integer.parseInt(args[2]);

        try (Reader reader = readerFactory.getReader(productType)) {
            reader.open(new File(args[1]));

            final Dimension productSize = reader.getProductSize();
            final TimeLocator timeLocator = reader.getTimeLocator();

            final AcquisitionInfo acquisitionInfo = reader.read();
            final TimeAxis[] timeAxes = acquisitionInfo.getTimeAxes();
            TimeAxis timeAxis = timeAxes[0];
            if (timeAxes.length > 1) {
                if (lineNumber > (productSize.getNy() / 2)) {
                    timeAxis = timeAxes[1];
                }
            }

            final Interval interval = new Interval(1, 1);

            final float[] delta = new float[productSize.getNx()];
            for (int x = 0; x < productSize.getNx(); x++) {
                final long acquisitionTime = timeLocator.getTimeFor(x, lineNumber);
                //System.out.println("acquisitionTime = " + acquisitionTime);


                final Array lonArray = reader.readRaw(x, lineNumber, interval, "lon");
                final Index lonIndex = lonArray.getIndex();
                final float lon = lonArray.getFloat(lonIndex);

                final Array latArray = reader.readRaw(x, lineNumber, interval, "lat");
                final Index latIndex = latArray.getIndex();
                final float lat = latArray.getFloat(latIndex);

                //System.out.println("location = " + lon + " " + lat);
                final Date estimatedTIme = timeAxis.getTime(geometryFactory.createPoint(lon, lat));
                //System.out.println("estimatedTIme = " + estimatedTIme.getTime());
                final float deltaTIme = (acquisitionTime - estimatedTIme.getTime()) / 1000.f;
                delta[x] = deltaTIme;
                //System.out.println("delta = " + deltaTIme);

            }

            System.out.print("[");
            for (int x = 0; x < productSize.getNx(); x++) {
                System.out.print(delta[x]);
                System.out.print(" ");
            }
            System.out.println("]");

        }
    }
}
