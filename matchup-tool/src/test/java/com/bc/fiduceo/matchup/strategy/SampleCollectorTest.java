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

import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Point;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.matchup.MatchupSet;
import com.bc.fiduceo.matchup.Sample;
import com.bc.fiduceo.matchup.SampleSet;
import com.bc.fiduceo.reader.TimeLocator;
import com.bc.fiduceo.tool.ToolContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.when;

public class SampleCollectorTest {

    private SampleCollector collector;
    private GeometryFactory factory;
    private ToolContext context;

    @Before
    public void setUp() throws Exception {
        factory = new GeometryFactory(GeometryFactory.Type.S2);
        context = new ToolContext();
        context.setGeometryFactory(factory);

        final PixelLocator pixelLocator = new OffsetPixelLocator(11, 13);
        collector = new SampleCollector(context, pixelLocator);
    }

    @Test
    public void testAddPrimarySamples() {
        // preparation
        final List<Point> polygonPoints = createPoints(new double[]{
                1, 1,
                3, 1,
                3, 3,
                1, 3,
                1, 1
        });
        final Polygon polygon = factory.createPolygon(polygonPoints);

        // execution
        final MatchupSet matchupSet = new MatchupSet();
        collector.addPrimarySamples(polygon, matchupSet, new TestTimeLocator());

        // verification
        final List<SampleSet> sampleSets = matchupSet.getSampleSets();
        assertNotNull(sampleSets);
        final Sample[] expecteds = {
                new Sample(12, 14, 1.5, 1.5, 14012L),
                new Sample(13, 14, 2.5, 1.5, 14013L),
                new Sample(12, 15, 1.5, 2.5, 15012L),
                new Sample(13, 15, 2.5, 2.5, 15013L)

        };
        assertEquals(expecteds.length, sampleSets.size());
        for (int i = 0; i < expecteds.length; i++) {
            final Sample expected = expecteds[i];
            final SampleSet actual = sampleSets.get(i);
            final Sample primary = actual.getPrimary();
            assertEquals("Index = " + i, expected.x, primary.x);
            assertEquals("Index = " + i, expected.y, primary.y);
            assertEquals("Index = " + i, expected.lon, primary.lon, 0.000001);
            assertEquals("Index = " + i, expected.lat, primary.lat, 0.000001);
            assertEquals("Index = " + i, expected.time, primary.time);

            assertNull(actual.getSecondary());
        }
    }

    @Test
    public void testAddSecondarySamples() {
        final MatchupSet matchupSet = new MatchupSet();
        matchupSet.addPrimary(new Sample(2, 3, 4.5, 5.5, 100L));
        matchupSet.addPrimary(new Sample(6, 7, 8.5, 9.5, 200L));

        final List<SampleSet> sampleSets = collector.addSecondarySamples(matchupSet.getSampleSets(), new TestTimeLocator());

        assertEquals(2, sampleSets.size());

        SampleSet sampleSet = sampleSets.get(0);
        Sample primary = sampleSet.getPrimary();
        assertEquals(2, primary.x);

        Sample secondary = sampleSet.getSecondary();
        assertEquals(15, secondary.x);
        assertEquals(18, secondary.y);
        assertEquals(18015L, secondary.time);

        sampleSet = sampleSets.get(1);
        primary = sampleSet.getPrimary();
        assertEquals(6, primary.x);
        secondary = sampleSet.getSecondary();
        assertEquals(19, secondary.x);
        assertEquals(22, secondary.y);
        assertEquals(22019L, secondary.time);
    }

    @Test
    public void testRemoveSampleSetIfNoSecondarySampleCanBeFound() throws Exception {
        final PixelLocator locator = Mockito.mock(PixelLocator.class);
        collector = new SampleCollector(context, locator);

        final Sample primaryOne = new Sample(2, 3, 4.5, 5.5, 100L);
        final Sample primaryTwo = new Sample(6, 7, 8.5, 9.5, 200L);

        final Point2D[] emptyArray = {};
        when(locator.getPixelLocation(primaryOne.lon, primaryOne.lat)).thenReturn(emptyArray);
        when(locator.getPixelLocation(primaryTwo.lon, primaryTwo.lat)).thenReturn(new Point2D[]{new Point2D.Double(12.3, 14.8)});
        when(locator.getGeoLocation(eq(12.5), eq(14.5), isA(Point2D.class))).thenReturn(new Point2D.Double(170.123, 34.321));

        final MatchupSet matchupSet = new MatchupSet();
        matchupSet.addPrimary(primaryOne);
        matchupSet.addPrimary(primaryTwo);

        final List<SampleSet> sampleSets = collector.addSecondarySamples(matchupSet.getSampleSets(), new TestTimeLocator());

        assertEquals(1, sampleSets.size());

        SampleSet sampleSet = sampleSets.get(0);
        Sample primary = sampleSet.getPrimary();
        assertSame(primaryTwo, primary);
        final Sample secondary = sampleSet.getSecondary();
        assertNotNull(secondary);
        assertEquals(34.321, secondary.lat, 1e-8);
        assertEquals(170.123, secondary.lon, 1e-8);
        assertEquals(12, secondary.x);
        assertEquals(14, secondary.y);
        assertEquals(14012, secondary.time);
    }

    @Test
    public void testPointInPolygonTest_GeometriesCreatedByFactory() throws Exception {
        final Polygon polygon = factory.createPolygon(createPoints(new double[]{
                2, 2,
                6, 2,
                6, 6,
                2, 6,
                2, 2
        }));
        assertTrue(polygon.contains(factory.createPoint(4, 4)));
    }

    private List<Point> createPoints(final double[] lonsLats) {
        final ArrayList<Point> points = new ArrayList<>();
        for (int i = 0; i < lonsLats.length; i++) {
            double lon = lonsLats[i];
            double lat = lonsLats[++i];
            points.add(factory.createPoint(lon, lat));
        }
        return points;
    }

    private class OffsetPixelLocator implements PixelLocator {

        private final int offsetX;
        private final int offsetY;

        public OffsetPixelLocator(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        @Override
        public Point2D getGeoLocation(double x, double y, Point2D g) {
            return new Point2D.Double(x - offsetX, y - offsetY);
        }

        @Override
        public Point2D[] getPixelLocation(double lon, double lat) {
            return new Point2D[]{new Point2D.Double(lon + offsetX, lat + offsetY)};
        }
    }

    private class TestTimeLocator implements TimeLocator {

        @Override
        public long getTimeFor(int x, int y) {
            return x + 1000 * y;
        }
    }
}
