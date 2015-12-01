package com.bc.fiduceo.geometry.s2;

import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Point;
import com.bc.fiduceo.geometry.Polygon;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class GeometryFactoryTest_S2 {

    private GeometryFactory factory;

    @Before
    public void setUp() {
        factory = new GeometryFactory(GeometryFactory.Type.S2);
    }

    @Test
    public void testParsePolygon() {
        final Geometry geometry = factory.parse("POLYGON((2 6, 2 5, 2 4, 2 3, 3 3, 3 4, 3 5, 3 6, 2 6))");
        assertNotNull(geometry);
        assertTrue(geometry instanceof S2Polygon);

        assertEquals("Polygon: (1) loops:\n" +
                "loop <\n" +
                "(6.0, 1.9999999999999996)\n" +
                "(4.999999999999999, 1.9999999999999996)\n" +
                "(4.0, 2.0)\n" +
                "(3.0000000000000004, 1.9999999999999996)\n" +
                "(3.000000000000001, 3.0000000000000004)\n" +
                "(4.0, 3.0000000000000004)\n" +
                "(4.999999999999999, 3.0000000000000004)\n" +
                "(6.000000000000001, 3.0000000000000004)\n" +
                ">\n", geometry.toString());
    }

    @Test
    public void testParseLineString() {
        final Geometry geometry = factory.parse("LINESTRING(2 1, 3 2, 4 3, 5 4)");
        assertNotNull(geometry);
        assertTrue(geometry instanceof S2LineString);

        // @todo 3 tb/tb invent some test here to verify the correctness of parsing 2015-12-01
        //assertEquals("bla", geometry.toString());
    }

    @Test
    public void testCreatePoint() {
        Point point = factory.createPoint(22.89, -12.45);
        assertNotNull(point);
        assertEquals(22.89, point.getLon(), 1e-8);
        assertEquals(-12.45, point.getLat(), 1e-8);

        point = factory.createPoint(-107.335, 20.97);
        assertNotNull(point);
        assertEquals(-107.335, point.getLon(), 1e-8);
        assertEquals(20.97, point.getLat(), 1e-8);
    }

    @Test
    public void testCreatePolygon() {
        final ArrayList<Point> points = new ArrayList<>();

        points.add(factory.createPoint(2, 2));
        points.add(factory.createPoint(4, 2));
        points.add(factory.createPoint(4, 4));
        points.add(factory.createPoint(2, 4));
        points.add(factory.createPoint(2, 2));

        final Polygon polygon = factory.createPolygon(points);
        assertNotNull(polygon);
        assertEquals("Polygon: (1) loops:\n" +
                "loop <\n" +
                "(1.9999999999999996, 2.0)\n" +
                "(2.0, 4.0)\n" +
                "(4.0, 4.0)\n" +
                "(4.0, 2.0)\n" +
                "(1.9999999999999996, 2.0)\n" +
                ">\n", polygon.toString());
    }
}