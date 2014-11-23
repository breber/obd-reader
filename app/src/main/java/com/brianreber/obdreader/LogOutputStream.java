package com.brianreber.obdreader;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by breber on 11/22/14.
 */
public class LogOutputStream extends OutputStream {

    private OutputStream mOutStream;
    private StringBuilder mSb;

    public LogOutputStream(OutputStream outStream) {
        mOutStream = outStream;
        mSb = new StringBuilder();
    }

    @Override
    public void close() throws IOException {
        super.close();
        mOutStream.close();
    }

    @Override
    public void flush() throws IOException {
        super.flush();

        Log.d("LOS", mSb.toString());
        mSb = new StringBuilder();
    }

    @Override
    public void write(int i) throws IOException {
        mSb.append((char) i);
        mOutStream.write(i);
    }
}
