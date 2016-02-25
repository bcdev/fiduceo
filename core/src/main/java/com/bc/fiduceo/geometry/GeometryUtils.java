package com.bc.fiduceo.geometry;

import java.util.List;

public class GeometryUtils {

    // @todo 3 tb/tb write test for this 2016-02-14
    public static String plotMultiPolygon(List<Polygon> polygonList) {
        final StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append("MULTIPOLYGON(");

        for (int j = 0; j < polygonList.size(); j++) {
            Polygon polygon = polygonList.get(j);
            final Point[] points = polygon.getCoordinates();
            stringBuffer.append("((");
            for (int i = 0; i < points.length; i++) {
                Point coordinate = points[i];
                stringBuffer.append(coordinate.getLon());
                stringBuffer.append(" ");
                stringBuffer.append(coordinate.getLat());
                if (i < points.length - 1) {
                    stringBuffer.append(",");
                }
            }
            stringBuffer.append("))");
            if (j < polygonList.size() - 1) {
                stringBuffer.append(",");
            }
        }
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    // @todo 3 tb/tb write test for this 2016-02-14
    public static String plotPolygon(List<Point> points) {
        final StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append("POLYGON((");

        for (int j = 0; j < points.size(); j++) {
            stringBuffer.append(points.get(j).getLon());
            stringBuffer.append(" ");
            stringBuffer.append(points.get(j).getLat());
            if (j < points.size() - 1) {
                stringBuffer.append(",");
            }

            if (j == (points.size() - 1)) {
                stringBuffer.append(",");
                stringBuffer.append(points.get(0).getLon());
                stringBuffer.append(" ");
                stringBuffer.append(points.get(0).getLat());
            }
        }
        stringBuffer.append("))");

        return stringBuffer.toString();
    }
}
