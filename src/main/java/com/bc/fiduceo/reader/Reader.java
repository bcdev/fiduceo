package com.bc.fiduceo.reader;

import com.bc.fiduceo.core.SatelliteObservation;

import java.io.File;
import java.io.IOException;

public interface Reader {

    void open(File file) throws IOException;

    void close() throws IOException;

    SatelliteObservation read() throws IOException;
}