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

package com.bc.fiduceo.reader.airs;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.log.FiduceoLogger;
import com.bc.fiduceo.reader.AcquisitionInfo;
import com.bc.fiduceo.reader.Reader;
import com.bc.fiduceo.reader.TimeLocator;
import com.bc.fiduceo.util.TimeUtils;
import org.esa.snap.core.util.StringUtils;
import org.jdom2.Element;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class AIRS_L1B_Reader implements Reader {

    private static final String RANGE_BEGINNING_DATE = "RANGEBEGINNINGDATE";
    private static final String RANGE_ENDING_DATE = "RANGEENDINGDATE";
    private static final String RANGE_BEGINNING_TIME = "RANGEBEGINNINGTIME";
    private static final String RANGE_ENDING_TIME = "RANGEENDINGTIME";
    private static final String CORE_METADATA = "coremetadata";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.S";


    private final Logger logger;
    private NetcdfFile netcdfFile = null;

    AIRS_L1B_Reader(GeometryFactory geometryFactory) {
        logger = FiduceoLogger.getLogger();
    }

    static String getElementValue(Element element, String attribute) {
        if (element.getName().equals(attribute)) {
            return element.getChild("VALUE").getValue();
        }
        for (Element subElement : element.getChildren()) {
            if (subElement.getName().equals(attribute)) {
                return subElement.getChild("VALUE").getValue();
            } else {
                final String elementValue = getElementValue(subElement, attribute);
                if (StringUtils.isNotNullAndNotEmpty(elementValue)) {
                    return elementValue;
                }
            }
        }
        return null;
    }

    // package access for testing only tb 2015-08-05
    static Element getEosElement(String satelliteMeta) throws IOException {
        String trimmedMetaString = satelliteMeta.replaceAll("\\s+=\\s+", "=");
        trimmedMetaString = trimmedMetaString.replaceAll("\\?", "_");

        final StringBuilder sb = new StringBuilder(trimmedMetaString.length());
        final StringTokenizer lineFinder = new StringTokenizer(trimmedMetaString, "\t\n\r\f");
        while (lineFinder.hasMoreTokens()) {
            final String line = lineFinder.nextToken().trim();
            sb.append(line);
            sb.append("\n");
        }
        final EosCoreMetaParser parser = new EosCoreMetaParser();
        return parser.parseFromString(sb.toString());
    }

    // package access for testing only tb 2015-08-05
    static String getEosMetadata(String name, Group eosGroup) throws IOException {
        final Variable structMetadataVar = eosGroup.findVariable(name);
        if (structMetadataVar == null) {
            return null;
        }
        final Array metadataArray = structMetadataVar.read();
        return metadataArray.toString();
    }

    @Override
    public PixelLocator getPixelLocator() throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void open(File file) throws IOException {
        netcdfFile = NetcdfFile.open(file.getPath());
    }

    @Override
    public void close() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
        }
    }

    @Override
    public AcquisitionInfo read() throws IOException {
        final Group rootGroup = netcdfFile.getRootGroup();
        final String coreMateString = getEosMetadata(CORE_METADATA, rootGroup);
        final Element eosElement = getEosElement(coreMateString);

        final NodeType nodeType = readNodeType();

        final Group l1bAirsGroup = rootGroup.findGroup("L1B_AIRS_Science");
        if (l1bAirsGroup == null) {
            throw new IOException("'L1B_AIRS_Science' data group not found");
        }
        final Group geolocationFields = l1bAirsGroup.findGroup("Geolocation_Fields");
        final Variable latitudeVariable = geolocationFields.findVariable("Latitude");
        final Variable longitudeVariable = geolocationFields.findVariable("Longitude");
        final Array latitudes = latitudeVariable.read();
        final Array longitudes = longitudeVariable.read();


        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();
        acquisitionInfo.setNodeType(nodeType);

        final Date sensingStart = parseDate(getElementValue(eosElement, RANGE_BEGINNING_DATE), getElementValue(eosElement, RANGE_BEGINNING_TIME));
        final Date sensingStop = parseDate(getElementValue(eosElement, RANGE_ENDING_DATE), getElementValue(eosElement, RANGE_ENDING_TIME));

        acquisitionInfo.setSensingStart(sensingStart);
        acquisitionInfo.setSensingStop(sensingStop);

        throw new RuntimeException("incomplete code, continue implementing tb 2016-04-13");

//        return acquisitionInfo;
    }

    @Override
    public String getRegEx() {
        return "AIRS.\\d{4}.\\d{2}.\\d{2}.\\d{3}.L1B.*.hdf";
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

    @Override
    public Dimension getProductSize() {
        throw new RuntimeException("Not yet implemented");
    }

    // package access for testing only tb 2016-01-08
    Date parseDate(String dateString, String timeString) {
        final String timeStringWithMillis = stripMicrosecs(timeString);
        final String rangeBeginningDate = dateString + " " + timeStringWithMillis;
        return TimeUtils.parse(rangeBeginningDate, DATE_FORMAT);
    }

    // @todo 3 tb/** make static, packagelocal and write tests for this method 2016-03-16
    private String stripMicrosecs(String timeString) {
        final int lastDotIndex = timeString.lastIndexOf('.');
        return timeString.substring(0, lastDotIndex + 4);
    }

    private NodeType readNodeType() {
        String nodeType = null;
        final List<Group> groups = netcdfFile.getRootGroup().getGroups().get(0).getGroups();
        for (Group group : groups) {
            if (group.getShortName().equals("Swath_Attributes")) {
                List<Attribute> attributes = group.getAttributes();
                for (Attribute attribute : attributes) {
                    if (attribute.getShortName().equals("node_type")) {
                        nodeType = attribute.getStringValue();
                    }
                }
            }
        }
        if (nodeType == null) {
            logger.info("NodeType is not DEFINED.");
            return NodeType.UNDEFINED;
        }

        return NodeType.fromId(nodeType.equals("Ascending") ? 0 : 1);
    }
}
