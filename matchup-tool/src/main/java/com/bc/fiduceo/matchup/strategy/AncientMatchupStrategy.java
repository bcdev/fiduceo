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

package com.bc.fiduceo.matchup.strategy;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.SatelliteObservation;
import com.bc.fiduceo.core.UseCaseConfig;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryCollection;
import com.bc.fiduceo.geometry.Point;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.matchup.MatchupCollection;
import com.bc.fiduceo.matchup.MatchupSet;
import com.bc.fiduceo.matchup.SampleSet;
import com.bc.fiduceo.matchup.condition.ConditionEngine;
import com.bc.fiduceo.matchup.condition.ConditionEngineContext;
import com.bc.fiduceo.matchup.screening.ScreeningEngine;
import com.bc.fiduceo.math.Intersection;
import com.bc.fiduceo.math.IntersectionEngine;
import com.bc.fiduceo.math.TimeInfo;
import com.bc.fiduceo.math.TimeInterval;
import com.bc.fiduceo.reader.Reader;
import com.bc.fiduceo.reader.ReaderFactory;
import com.bc.fiduceo.reader.TimeLocator;
import com.bc.fiduceo.tool.ToolContext;
import com.bc.fiduceo.util.TimeUtils;
import ucar.ma2.InvalidRangeException;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class AncientMatchupStrategy extends AbstractMatchupStrategy {

    public AncientMatchupStrategy(Logger logger) {
        super(logger);
    }

    @Override
    public MatchupCollection createMatchupCollection(ToolContext context) throws SQLException, IOException, InvalidRangeException {
        final MatchupCollection matchupCollection = new MatchupCollection();

        final UseCaseConfig useCaseConfig = context.getUseCaseConfig();

        final ConditionEngine conditionEngine = new ConditionEngine();
        final ConditionEngineContext conditionEngineContext = ConditionEngine.createContext(context);
        conditionEngine.configure(useCaseConfig);

        final ScreeningEngine screeningEngine = new ScreeningEngine();
        screeningEngine.configure(useCaseConfig);

        final ReaderFactory readerFactory = ReaderFactory.get(context.getGeometryFactory());

        final long timeDeltaInMillis = conditionEngine.getMaxTimeDeltaInMillis();
        final int timeDeltaSeconds = (int) (timeDeltaInMillis / 1000);

        final List<SatelliteObservation> primaryObservations = getPrimaryObservations(context);
        for (final SatelliteObservation primaryObservation : primaryObservations) {
            try (final Reader primaryReader = readerFactory.getReader(primaryObservation.getSensor().getName())) {
                final Date searchTimeStart = TimeUtils.addSeconds(-timeDeltaSeconds, primaryObservation.getStartTime());
                final Date searchTimeEnd = TimeUtils.addSeconds(timeDeltaSeconds, primaryObservation.getStopTime());

                // @todo 2 tb/tb extract method
                final Geometry primaryGeoBounds = primaryObservation.getGeoBounds();
                final boolean isPrimarySegmented = AbstractMatchupStrategy.isSegmented(primaryGeoBounds);

                primaryReader.open(primaryObservation.getDataFilePath().toFile());

                final List<SatelliteObservation> secondaryObservations = getSecondaryObservations(context, searchTimeStart, searchTimeEnd);
                for (final SatelliteObservation secondaryObservation : secondaryObservations) {
                    try (Reader secondaryReader = readerFactory.getReader(secondaryObservation.getSensor().getName())) {
                        secondaryReader.open(secondaryObservation.getDataFilePath().toFile());
                        final Geometry secondaryGeoBounds = secondaryObservation.getGeoBounds();
                        final boolean isSecondarySegmented = AbstractMatchupStrategy.isSegmented(secondaryGeoBounds);

                        final Geometry[] primaryGeometries = getGeometryArray(primaryObservation);
                        final Geometry[] secondaryGeometries = getGeometryArray(secondaryObservation);

                        final List<Intersection> intersectionList = new ArrayList<>();
                        for (int primaryIndex = 0; primaryIndex < primaryGeometries.length; primaryIndex++) {
                            for (int secondaryIndex = 0; secondaryIndex < secondaryGeometries.length; secondaryIndex++) {
                                final Geometry primaryGeometry = primaryGeometries[primaryIndex];
                                final Geometry secondaryGeometry = secondaryGeometries[secondaryIndex];
                                final Intersection intersection = getIntersection(primaryGeometry, secondaryGeometry, primaryReader, isPrimarySegmented, secondaryReader, isSecondarySegmented);
                                if (intersection != null) {
                                    intersection.setPrimaryGeometry(primaryGeometry);
                                    intersection.setSecundaryGeometry(secondaryGeometry);
                                    intersectionList.add(intersection);
                                }
                            }
                        }

                        if (intersectionList.isEmpty()) {
                            continue;
                        }

                        final MatchupSet matchupSet = new MatchupSet();
                        matchupSet.setPrimaryObservationPath(primaryObservation.getDataFilePath());
                        matchupSet.setSecondaryObservationPath(secondaryObservation.getDataFilePath());

                        for (final Intersection intersection : intersectionList) {
                            final TimeInfo timeInfo = intersection.getTimeInfo();
                            final int minimalTimeDelta = timeInfo.getMinimalTimeDelta();
                            if (minimalTimeDelta < (timeDeltaInMillis + 30000)) {
                                final PixelLocator primaryPixelLocator = getPixelLocator(primaryReader, isPrimarySegmented, (Polygon) intersection.getPrimaryGeometry());
                                final PixelLocator secondaryPixelLocator = getPixelLocator(secondaryReader, isSecondarySegmented, (Polygon) intersection.getSecondaryGeometry());

                                if (primaryPixelLocator == null || secondaryPixelLocator == null) {
                                    logger.warning("Unable to create valid pixel locators. Skipping intersection segment.");
                                    continue;
                                }

                                SampleCollector sampleCollector = new SampleCollector(context, primaryPixelLocator);
                                sampleCollector.addPrimarySamples((Polygon) intersection.getGeometry(), matchupSet, primaryReader.getTimeLocator());

                                sampleCollector = new SampleCollector(context, secondaryPixelLocator);
                                final List<SampleSet> completeSamples = sampleCollector.addSecondarySamples(matchupSet.getSampleSets(), secondaryReader.getTimeLocator());
                                matchupSet.setSampleSets(completeSamples);

                                if (matchupSet.getNumObservations() > 0) {
                                    applyConditionsAndScreenings(matchupCollection, conditionEngine, conditionEngineContext, screeningEngine, primaryReader, matchupSet, secondaryReader);
                                }
                            }
                        }
                    }
                }
            }
        }

        return matchupCollection;
    }

    private static Geometry[] getGeometryArray(SatelliteObservation observation) {
        Geometry[] geometries;
        final Geometry primaryGeometry = observation.getGeoBounds();
        if (primaryGeometry instanceof GeometryCollection) {
            final GeometryCollection primaryCollection = (GeometryCollection) primaryGeometry;
            geometries = primaryCollection.getGeometries();
        } else {
            geometries = new Geometry[]{primaryGeometry};
        }
        return geometries;
    }

    private static Intersection getIntersection(Geometry primaryGeometry, Geometry secondaryGeometry,
                                                Reader primaryReader, boolean isPrimarySegmented,
                                                Reader secondaryReader, boolean isSecondarySegmented) throws IOException {
        final TimeInfo timeInfo = new TimeInfo();
        final Geometry intersectionGeometry = primaryGeometry.getIntersection(secondaryGeometry);
        if (intersectionGeometry.isEmpty()) {
            return null;
        }

        final Point[] coordinates = intersectionGeometry.getCoordinates();
        final ArrayList<Date> primarySensorTimes = new ArrayList<>(coordinates.length);
        final ArrayList<Date> secondarySensorTimes = new ArrayList<>(coordinates.length);

        final PixelLocator primaryPixelLocator = getPixelLocator(primaryReader, isPrimarySegmented, (Polygon) primaryGeometry);
        final TimeLocator primaryTimeLocator = primaryReader.getTimeLocator();
        final Dimension primarySize = primaryReader.getProductSize();

        final PixelLocator secondaryPixelLocator = getPixelLocator(secondaryReader, isSecondarySegmented, (Polygon) secondaryGeometry);
        final TimeLocator secondaryTimeLocator = secondaryReader.getTimeLocator();
        final Dimension secondarySize = secondaryReader.getProductSize();


        for (int i = 0; i < coordinates.length - 1; i++) {
            final Point coordinate = coordinates[i];
            final double lon = coordinate.getLon();
            final double lat = coordinate.getLat();

            final Point2D[] primaryPixelLocations = primaryPixelLocator.getPixelLocation(lon, lat);
            if (primaryPixelLocations == null || primaryPixelLocations.length == 0) {
                continue;
            }

            final Point2D[] secondaryPixelLocations = secondaryPixelLocator.getPixelLocation(lon, lat);
            if (secondaryPixelLocations == null || secondaryPixelLocations.length == 0) {
                continue;
            }

            for (int primary = 0; primary < primaryPixelLocations.length; primary ++) {
                for (int secondary = 0; secondary < secondaryPixelLocations.length; secondary ++) {
                    int x = (int) Math.round(primaryPixelLocations[primary].getX());
                    int y = (int) Math.round(primaryPixelLocations[primary].getY());
                    if (contains(primarySize, x, y)) {
                        final long primaryTime = primaryTimeLocator.getTimeFor(x, y);
                        primarySensorTimes.add(TimeUtils.create(primaryTime));
                    }

                    x = (int) Math.round(secondaryPixelLocations[secondary].getX());
                    y = (int) Math.round(secondaryPixelLocations[secondary].getY());
                    if (contains(secondarySize, x, y)) {
                        final long secondaryTime = secondaryTimeLocator.getTimeFor(x, y);
                        secondarySensorTimes.add(TimeUtils.create(secondaryTime));
                    }
                }
            }

        }

        final TimeInterval primaryCommonInterval = TimeInterval.create(primarySensorTimes);
        final TimeInterval secondaryCommonInterval = TimeInterval.create(secondarySensorTimes);

        final TimeInterval overlapInterval = primaryCommonInterval.intersect(secondaryCommonInterval);
        if (overlapInterval == null) {
            final int timeDelta = IntersectionEngine.calculateTimeDelta(primaryCommonInterval, secondaryCommonInterval);
            timeInfo.setMinimalTimeDelta(timeDelta);
        } else {
            timeInfo.setMinimalTimeDelta(0);
            timeInfo.setOverlapInterval(overlapInterval);
        }

        final Intersection intersection = new Intersection();
        intersection.setGeometry(intersectionGeometry);
        intersection.setTimeInfo(timeInfo);
        return intersection;
    }

    private static boolean contains(Dimension dimension, int x, int y) {
        return x >= 0 && y >= 0 && x < dimension.getNx() && y < dimension.getNy();
    }
}
