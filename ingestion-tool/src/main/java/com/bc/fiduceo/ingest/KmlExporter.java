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

package com.bc.fiduceo.ingest;

import com.bc.fiduceo.archive.Archive;
import com.bc.fiduceo.archive.ArchiveConfig;
import com.bc.fiduceo.core.SystemConfig;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryCollection;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.GeometryUtil;
import com.bc.fiduceo.geometry.LineString;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.geometry.TimeAxis;
import com.bc.fiduceo.reader.AcquisitionInfo;
import com.bc.fiduceo.reader.Reader;
import com.bc.fiduceo.reader.ReaderFactory;
import com.bc.fiduceo.tool.ToolContext;
import com.bc.fiduceo.util.TimeUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class KmlExporter {

    public static void main(String[] args) throws ParseException, IOException {
        final CommandLineParser parser = new PosixParser();
        final CommandLine commandLine = parser.parse(IngestionTool.getOptions(), args);

        final String configDirPath = commandLine.getOptionValue("config");
        final Path confDirPath = Paths.get(configDirPath);

        final String sensorType = commandLine.getOptionValue("s");
        final String processingVersion = commandLine.getOptionValue("v");

        final ToolContext toolContext = initializeContext(commandLine, confDirPath);

        final ReaderFactory readerFactory = ReaderFactory.get(toolContext.getGeometryFactory());
        final Reader reader = readerFactory.getReader(sensorType);

        final SystemConfig systemConfig = toolContext.getSystemConfig();
        final ArchiveConfig archiveConfig = systemConfig.getArchiveConfig();
        final Archive archive = new Archive(archiveConfig);
        final Date startDate = toolContext.getStartDate();
        final Date endDate = toolContext.getEndDate();

        final Path[] productPaths = archive.get(startDate, endDate, processingVersion, sensorType);
        for (final Path filePath : productPaths) {
            reader.open(filePath.toFile());
            try {
                final AcquisitionInfo acquisitionInfo = reader.read();

                final String fileName = filePath.getFileName().toString();
                final String withoutExtension = FileUtils.getFilenameWithoutExtension(fileName);

                final Geometry boundingGeometry = acquisitionInfo.getBoundingGeometry();
                if (boundingGeometry instanceof Polygon) {
                    final Polygon polygon = (Polygon) boundingGeometry;
                    writePolygonFile(withoutExtension, polygon, 0);

                    final TimeAxis[] timeAxes = acquisitionInfo.getTimeAxes();
                    final LineString axis = (LineString) timeAxes[0].getGeometry();
                    writeAxisFile(withoutExtension, axis, 0);
                } else {
                    final GeometryCollection collection = (GeometryCollection) boundingGeometry;
                    final Geometry[] geometries = collection.getGeometries();
                    final TimeAxis[] timeAxes = acquisitionInfo.getTimeAxes();
                    int i = 0;
                    for (final Geometry geometry : geometries) {
                        final Polygon polygon = (Polygon) geometry;
                        writePolygonFile(withoutExtension, polygon, i + 1);


                        final LineString axis = (LineString) timeAxes[i].getGeometry();
                        writeAxisFile(withoutExtension, axis, i + 1);
                        ++i;
                    }

                }

            } catch (Exception e) {
            } finally {
                reader.close();
            }
        }
    }

    private static void writeAxisFile(String withoutExtension, LineString axis, int index) throws IOException {
        FileOutputStream fileOutputStream;
        final String axisKML = GeometryUtil.toKml(axis);
        final File kmlAxisFile;
        if (index > 0) {
            kmlAxisFile = new File(withoutExtension + "_axis_" + index + ".kml");
        } else {
            kmlAxisFile = new File(withoutExtension + "_axis.kml");
        }
        kmlAxisFile.createNewFile();
        fileOutputStream = new FileOutputStream(kmlAxisFile);
        fileOutputStream.write(axisKML.getBytes());
        fileOutputStream.close();
    }

    private static void writePolygonFile(String withoutExtension, Polygon polygon, int index) throws IOException {
        final String boundingGeometryKML = GeometryUtil.toKml(polygon);

        final File kmlBoundFile;
        if (index > 0) {
            kmlBoundFile = new File(withoutExtension + "_bound_" + index + ".kml");
        } else {
            kmlBoundFile = new File(withoutExtension + "_bound.kml");
        }
        kmlBoundFile.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(kmlBoundFile);
        fileOutputStream.write(boundingGeometryKML.getBytes());
        fileOutputStream.close();
    }

    private static ToolContext initializeContext(CommandLine commandLine, Path confDirPath) throws IOException {
        final ToolContext context = new ToolContext();

        final String startTime = commandLine.getOptionValue("start");
        if (StringUtils.isNotNullAndNotEmpty(startTime)) {
            final Date startDate = TimeUtils.parse(startTime, "yyyy-DDD");
            context.setStartDate(startDate);
        }

        final String endTime = commandLine.getOptionValue("end");
        if (StringUtils.isNotNullAndNotEmpty(endTime)) {
            final Date endDate = TimeUtils.parse(endTime, "yyyy-DDD");
            context.setEndDate(endDate);
        }

        final SystemConfig systemConfig = SystemConfig.loadFrom(confDirPath.toFile());
        context.setSystemConfig(systemConfig);

        final GeometryFactory geometryFactory = new GeometryFactory(systemConfig.getGeometryLibraryType());
        context.setGeometryFactory(geometryFactory);

        return context;
    }
}
