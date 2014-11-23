package com.brianreber.obdreader;

import java.io.IOException;
import java.io.InputStream;

import pt.lighthouselabs.obd.commands.ObdCommand;

/**
 * Created by breber on 11/21/14.
 */
public class VinCommand extends ObdCommand {

    private StringBuilder mVin = new StringBuilder();

    public VinCommand() {
        super("09 02");
    }

    @Override
    protected void fillBuffer() {
        String workingData = getResult().replaceAll("[\r\n]", "");

        // ignore first two bytes [XX XX] of the response
        // as well as the first ':'
        workingData = workingData.substring(4);

        // At least for Ford Focus, this is formatted as
        // 3 blocks split by ':'
        String[] splits = workingData.split(":");

        for (String split : splits) {
            int begin = 0;
            int end = 2;
            while (end <= rawData.length()) {
                buffer.add(Integer.decode("0x" + rawData.substring(begin, end)));
                begin = end;
                end += 2;
            }
        }
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
        rawData = rawData.replace("SEARCHING...", "");
        rawData = rawData.trim();
    }

    @Override
    protected void performCalculations() {
        mVin = new StringBuilder();

        // ignore first two bytes [XX XX] of the response
        for (int i = 0; i < buffer.size(); i++) {
            mVin.append((char) buffer.get(i).intValue());
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
