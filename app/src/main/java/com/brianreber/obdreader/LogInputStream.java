package com.brianreber.obdreader;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by breber on 11/22/14.
 */
public class LogInputStream extends InputStream {

    private InputStream mInStream;
    private StringBuilder mSb;

    public LogInputStream(InputStream inStream) {
        mInStream = inStream;
        mSb = new StringBuilder();
    }

    @Override
    public int read() throws IOException {
        int read = mInStream.read();

        byte readByte = (byte) read;
        mSb.append((char) readByte);

        return read;
    }

    public void flush() {
        Log.d("LIS", mSb.toString());
        mSb = new StringBuilder();
    }
}
