package com.brianreber.obdreader;

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
    protected void performCalculations() {
        // ignore first two bytes [XX XX] of the response
        for (int i = 2; i < 20; i++) {
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
