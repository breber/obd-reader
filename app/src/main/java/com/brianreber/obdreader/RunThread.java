package com.brianreber.obdreader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.commands.control.DistanceTraveledSinceCodesClearedObdCommand;
import pt.lighthouselabs.obd.commands.control.DtcNumberObdCommand;
import pt.lighthouselabs.obd.commands.control.TroubleCodesObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineLoadObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRuntimeObdCommand;
import pt.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import pt.lighthouselabs.obd.commands.engine.ThrottlePositionObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FindFuelTypeObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelConsumptionRateObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelEconomyObdCommand;
import pt.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import pt.lighthouselabs.obd.commands.pressure.BarometricPressureObdCommand;
import pt.lighthouselabs.obd.commands.pressure.FuelPressureObdCommand;
import pt.lighthouselabs.obd.commands.pressure.IntakeManifoldPressureObdCommand;
import pt.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.LineFeedOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import pt.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import pt.lighthouselabs.obd.commands.temperature.AirIntakeTemperatureObdCommand;
import pt.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import pt.lighthouselabs.obd.commands.temperature.EngineCoolantTemperatureObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;

/**
 * Created by breber on 11/21/14.
 */
public class RunThread implements Runnable {

    private static final String TAG = "RunThread";
    private String mDeviceAddress;
    private Activity mContext;
    private List<ObdCommand> mObdCommands = new ArrayList<ObdCommand>();

    public RunThread(Activity activity, String deviceAddress) {
        mContext = activity;
        mDeviceAddress = deviceAddress;

        addCommands();
    }

    private void addCommands() {
        mObdCommands.add(new AirIntakeTemperatureObdCommand());
        mObdCommands.add(new AmbientAirTemperatureObdCommand());
        mObdCommands.add(new EngineCoolantTemperatureObdCommand());
        mObdCommands.add(new BarometricPressureObdCommand());
        mObdCommands.add(new FuelPressureObdCommand());
        mObdCommands.add(new IntakeManifoldPressureObdCommand());
        mObdCommands.add(new FindFuelTypeObdCommand());
        mObdCommands.add(new FuelConsumptionRateObdCommand());
        mObdCommands.add(new FuelEconomyObdCommand());
        mObdCommands.add(new FuelLevelObdCommand());
//        mObdCommands.add(new FuelTrimObdCommand());
        mObdCommands.add(new EngineLoadObdCommand());
        mObdCommands.add(new EngineRPMObdCommand());
        mObdCommands.add(new EngineRuntimeObdCommand());
        mObdCommands.add(new MassAirFlowObdCommand());
        mObdCommands.add(new ThrottlePositionObdCommand());
//        mObdCommands.add(new CommandEquivRatioObdCommand());
        mObdCommands.add(new DistanceTraveledSinceCodesClearedObdCommand());
        mObdCommands.add(new DtcNumberObdCommand());
//        mObdCommands.add(new TimingAdvanceObdCommand());
        mObdCommands.add(new TroubleCodesObdCommand());
    }

    // See http://en.wikipedia.org/wiki/OBD-II_PIDs#Standard_PIDs
    // See http://blog.lemberg.co.uk/how-guide-obdii-reader-app-development
    // See https://docs.google.com/spreadsheet/ccc?key=0Ajz-75u_7nEydFJxUG4yOVZ1NXJlcjNvdzdSTDdyY0E#gid=0
    // See https://github.com/openxc/vi-firmware/blob/next/src/obd2.cpp#L41

    @Override
    public void run() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice(mDeviceAddress);

        // UUID for serial device
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();

            LogInputStream inStream = new LogInputStream(socket.getInputStream());
            LogOutputStream outStream = new LogOutputStream(socket.getOutputStream());

            // Do some basic initialization
            new EchoOffObdCommand().run(inStream, outStream);
            inStream.flush();
            new LineFeedOffObdCommand().run(inStream, outStream);
            inStream.flush();
            new TimeoutObdCommand(30).run(inStream, outStream);
            inStream.flush();
            new SelectProtocolObdCommand(ObdProtocols.AUTO).run(inStream, outStream);
            inStream.flush();

            // Get the VIN
            final VinCommand vinCommand = new VinCommand();
            vinCommand.run(inStream, outStream);
            inStream.flush();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tv = (TextView) mContext.findViewById(R.id.vehicle_id);
                    tv.setText("VIN: " + vinCommand.getFormattedResult());
                }
            });

            while (!Thread.currentThread().isInterrupted()) {
                final StringBuilder resultStr = new StringBuilder();

                for (ObdCommand cmd : mObdCommands) {
                    cmd.run(inStream, outStream);
                    inStream.flush();
                    resultStr.append(cmd.getName() + ": " + cmd.getFormattedResult() + "\n");
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv = (TextView) mContext.findViewById(R.id.main_text);
                        tv.setText(resultStr.toString());
                    }
                });

                Thread.sleep(5000);
            }
        } catch (final Exception e) {
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private static void runOnUiThread(Runnable runnable){
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(runnable);
    }
}
