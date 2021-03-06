package com.bc.fiduceo.reader;

import com.bc.fiduceo.core.Interval;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class RawDataReaderTest_context2D_FirstDimensionIsOne_float {

    private Interval windowSize;
    private Number fillValue;
    private float f;
    private Array rawArray;

    @Before
    public void setUp() throws Exception {
        windowSize = new Interval(3, 3);
        fillValue = -2;
        f = fillValue.byteValue();
        rawArray = getFloatRawArray();
    }

    @Test
    public void testWindowCenter() throws Exception {

        final Array array = RawDataReader.read(3, 3, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(float.class, array.getElementType());
        assertEquals(9, array.getSize());
        final float[] expecteds = {
                2, 2, 2,
                3, 3, 3,
                4, 4, 4};
        final float[] actuals = (float[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals, 1e-8f);
    }

    @Test
    public void testTopRightWindowOut() throws Exception {

        final Array array = RawDataReader.read(9, 0, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(float.class, array.getElementType());
        assertEquals(9, array.getSize());
        final float[] expecteds = {
                f, f, f,
                0, 0, f,
                1, 1, f};
        final float[] actuals = (float[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals, 1e-8f);
    }

    @Test
    public void testTopLeftWindowOut() throws Exception {

        final Array array = RawDataReader.read(0, 0, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(float.class, array.getElementType());
        assertEquals(9, array.getSize());
        final float[] expecteds = {
                f, f, f,
                f, 0, 0,
                f, 1, 1};
        final float[] actuals = (float[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals, 1e-8f);
    }

    @Test
    public void testBottomLeftWindowOut() throws Exception {
        final Array array = RawDataReader.read(0, 9, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(float.class, array.getElementType());
        assertEquals(9, array.getSize());
        final float[] expecteds = {
                f, 8, 8,
                f, 9, 9,
                f, f, f};
        final float[] actuals = (float[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals, 1e-8f);
    }

    @Test
    public void testBottomRightWindowOut() throws Exception {
        final Array array = RawDataReader.read(9, 9, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(float.class, array.getElementType());
        assertEquals(9, array.getSize());
        final float[] expecteds = {
                8, 8, f,
                9, 9, f,
                f, f, f};
        final float[] actuals = (float[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals, 1e-8f);
    }

    @Test
    public void testRawArrayHasMoreThanTwoDimensions() throws InvalidRangeException {
        final Array rawArray = Array.factory(new float[][][]{
                {{11, 12, 13}, {14, 15, 16}, {17, 18, 19},},
                {{21, 22, 23}, {24, 25, 26}, {27, 28, 29},},
                {{31, 32, 33}, {34, 35, 36}, {37, 38, 39},}
        });

        try {
            RawDataReader.read(1, 1, new Interval(3, 3), -4d, rawArray, 10);
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }
    }

    private Array getFloatRawArray() {
        final float[][] bytes = new float[1][];
        bytes[0] = new float[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        return Array.factory(bytes);
    }
}