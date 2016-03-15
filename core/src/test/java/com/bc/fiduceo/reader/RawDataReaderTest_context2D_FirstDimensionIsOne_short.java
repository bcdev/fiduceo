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

public class RawDataReaderTest_context2D_FirstDimensionIsOne_short {

    private Interval windowSize;
    private Number fillValue;
    private short fv;
    private Array rawArray;

    @Before
    public void setUp() throws Exception {
        windowSize = new Interval(3, 3);
        fillValue = -2;
        fv = fillValue.byteValue();
        rawArray = getShortRawArray();
    }

    @Test
    public void testWindowCenter() throws Exception {

        final Array array = RawDataReader.read(3, 3, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(short.class, array.getElementType());
        assertEquals(9, array.getSize());
        final short[] expecteds = {2, 2, 2, 3, 3, 3, 4, 4, 4};
        final short[] actuals = (short[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testTopRightWindowOut() throws Exception {

        final Array array = RawDataReader.read(9, 0, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(short.class, array.getElementType());
        assertEquals(9, array.getSize());
        final short[] expecteds = {fv, fv, fv, 0, 0, fv, 1, 1, fv};
        final short[] actuals = (short[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testTopLeftWindowOut() throws Exception {

        final Array array = RawDataReader.read(0, 0, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(short.class, array.getElementType());
        assertEquals(9, array.getSize());
        final short[] expecteds = {fv, fv, fv, fv, 0, 0, fv, 1, 1};
        final short[] actuals = (short[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testBottomLeftWindowOut() throws Exception {
        final Array array = RawDataReader.read(0, 9, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(short.class, array.getElementType());
        assertEquals(9, array.getSize());
        final short[] expecteds = {fv, 8, 8, fv, 9, 9, fv, fv, fv};
        final short[] actuals = (short[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testBottomRightWindowOut() throws Exception {
        final Array array = RawDataReader.read(9, 9, windowSize, fillValue, rawArray, 10);

        assertNotNull(array);
        assertEquals(short.class, array.getElementType());
        assertEquals(9, array.getSize());
        final short[] expecteds = {8, 8, fv, 9, 9, fv, fv, fv, fv};
        final short[] actuals = (short[]) array.get1DJavaArray(array.getElementType());
        assertArrayEquals(expecteds, actuals);
    }

    @Test
    public void testRawArrayHasMoreThanTwoDimensions() throws InvalidRangeException {
        final Array rawArray = Array.factory(new short[][][]{
                {{11, 12, 13}, {14, 15, 16}, {17, 18, 19},},
                {{21, 22, 23}, {24, 25, 26}, {27, 28, 29},},
                {{31, 32, 33}, {34, 35, 36}, {37, 38, 39},}
        });

        try {
            RawDataReader.read(1, 1, new Interval(3, 3), -4d, rawArray, 10);
            fail("InvalidRangeException expected");
        } catch (InvalidRangeException expected) {
        }
    }


    @Test
    public void testRawArrayHasLessThanTwoDimensions() throws InvalidRangeException {
        final Array rawArray = Array.factory(new short[]{11, 12, 13});

        try {
            RawDataReader.read(1, 1, new Interval(3, 3), -4d, rawArray, 10);
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }
    }

    private Array getShortRawArray() {
        final short[][] bytes = new short[1][];
        bytes[0] = new short[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        return Array.factory(bytes);
    }
}