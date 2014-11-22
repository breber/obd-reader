package com.brianreber.obdreader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import java.util.UUID;

import pt.lighthouselabs.obd.commands.engine.EngineRuntimeObdCommand;
import pt.lighthouselabs.obd.commands.protocol.EchoOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.LineFeedOffObdCommand;
import pt.lighthouselabs.obd.commands.protocol.SelectProtocolObdCommand;
import pt.lighthouselabs.obd.commands.protocol.TimeoutObdCommand;
import pt.lighthouselabs.obd.enums.ObdProtocols;

/**
 * Created by breber on 11/21/14.
 */
public class RunThread implements Runnable {

    private static final String TAG = "RunThread";
    private String mDeviceAddress;
    private Activity mContext;

    public RunThread(Activity activity, String deviceAddress) {
        mContext = activity;
        mDeviceAddress = deviceAddress;
    }

    // See http://en.wikipedia.org/wiki/OBD-II_PIDs#Standard_PIDs
    // See http://blog.lemberg.co.uk/how-guide-obdii-reader-app-development

    @Override
    public void run() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice(mDeviceAddress);

        // UUID for serial device
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();

            // Do some basic initialization
            new EchoOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());
            new LineFeedOffObdCommand().run(socket.getInputStream(), socket.getOutputStream());
            new TimeoutObdCommand(30).run(socket.getInputStream(), socket.getOutputStream());
            new SelectProtocolObdCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());

            // Get the VIN
            final VinCommand vinCommand = new VinCommand();
            vinCommand.run(socket.getInputStream(), socket.getOutputStream());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tv = (TextView) mContext.findViewById(R.id.vehicle_id);
                    tv.setText("VIN: " + vinCommand.getFormattedResult());
                }
            });

            final EngineRuntimeObdCommand engineRuntimeCommand = new EngineRuntimeObdCommand();
            while (!Thread.currentThread().isInterrupted()) {
                engineRuntimeCommand.run(socket.getInputStream(), socket.getOutputStream());

                // TODO handle commands result
                Log.d(TAG, "Runtime: " + engineRuntimeCommand.getFormattedResult());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tv = (TextView) mContext.findViewById(R.id.main_text);
                        tv.setText("Runtime: " + engineRuntimeCommand.getFormattedResult());
                    }
                });

                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runOnUiThread(Runnable runnable){
        final Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(runnable);
    }
}
