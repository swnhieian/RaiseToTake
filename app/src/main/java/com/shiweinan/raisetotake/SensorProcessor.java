package com.shiweinan.raisetotake;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SensorProcessor implements SensorEventListener {
    float[] accelerometerValues = new float[3];
    float[] magneticValues = new float[3];
    float[] gyroscopeValues = new float[3];
    private MainActivity mainActivity;
    private List<SensorData> data = new LinkedList<>();

    public SensorProcessor(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        int type = event.sensor.getType();
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerValues = values.clone();
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeValues = values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = values.clone();
                break;
            default:
                break;
        }
        getCurrentOrientation(event.timestamp);
        raiseStatus();
    }

    private boolean xSet = false;
    private RaiseStatus lastStatus = RaiseStatus.Fail;
    private void getCurrentOrientation(long timestamp) {
        float[] R = new float[9];
        float[] values = new float[3];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticValues);
        SensorManager.getOrientation(R, values);
        //x 0 -> -90; y 0 -> +/- 90
        /*System.out.println(timestamp + ":" + Math.toDegrees(values[1]) + "," +
                           Math.toDegrees(values[2]) + "," +
                           Math.toDegrees(values[0]));*/
        double x = Math.toDegrees(values[1]);
        double y = Math.toDegrees(values[2]);
        double z = Math.toDegrees(values[0]);

        if (data.size() > 0) { //save data at least every 10ms
            long lastTime = data.get(data.size() - 1).timeStamp;
            if (timestamp - lastTime < 10000000) {
                return;
            }
        }
        SensorData d = new SensorData();
        d.timeStamp = timestamp;
        d.xOrientation = x;
        d.yOrientation = y;
        d.zOrientation = z;
        data.add(d);
        while (data.size()> 0 && (timestamp - data.get(0).timeStamp) > 2000000000) { // reserve most recent 2s data
            data.remove(0);
        }
    }
    public enum RaiseStatus { Waiting, Fail, Prepare, Done };
    private RaiseStatus raiseStatus() {
        if (data.size() < 10) { return RaiseStatus.Fail; }
        double lastX = data.get(data.size() - 1).xOrientation;
        for (int i= data.size() - 1; i>=0; i--) {
        }
        System.out.println("data size: " + data.size());

       return RaiseStatus.Fail;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
