package com.brianreber.obdreader;

import java.io.IOException;
import java.io.InputStream;

import pt.lighthouselabs.obd.commands.ObdCommand;

/**
 * Created by breber on 11/21/14.
 */
public class VinCommand extends ObdCommand {

    private StringBuilder mVin;

    public VinCommand() {
        super("09 02");
    }

    @Override
    protected void fillBuffer() {
    }

    @Override
    protected void readRawData(InputStream in) throws IOException {
        byte b = 0;
        StringBuilder res = new StringBuilder();

        // read until '>' arrives
        while ((char) (b = (byte) in.read()) != '>') {
            if ((char) b != ' ') {
                res.append((char) b);
            }
        }

        rawData = res.toString().trim();
    }

    @Override
    protected void performCalculations() {
        String workingData = getResult().replaceAll("[\r\n]", "");

        // ignore first two bytes [XX XX] of the response
        for (int i = 0; i < workingData.length(); i++) {
            mVin.append((char) Integer.parseInt("" + workingData.charAt(i), 16));
        }
    }

    @Override
    public String getFormattedResult() {
        return mVin.toString();
    }

    @Override
    public String getName() {
        return "VIN";
    }
}
