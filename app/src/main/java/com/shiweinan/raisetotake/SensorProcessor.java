package com.shiweinan.raisetotake;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.Collections;
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

        if (data.size() > 0) { //save data at least every 1ms
            long lastTime = data.get(data.size() - 1).timeStamp;
            if (timestamp - lastTime < 1000000) {
                return;
            }
        }
        SensorData d = new SensorData();
        d.timeStamp = timestamp;
        d.xOrientation = x;
        d.yOrientation = y;
        d.zOrientation = z;
        data.add(d);
        while (data.size()> 0 && (timestamp - data.get(0).timeStamp) > 500000000) { // reserve most recent 0.5s data
            data.remove(0);
        }
    }
    public enum RaiseStatus { Waiting, Fail, Prepare, Done };
    private RaiseStatus raiseStatus() {
        if (data.size() < 10) {
            return RaiseStatus.Fail;
        }

        SensorData currOri = data.get(data.size() - 1);

        // Get speed on unit sphere within 200 ms
        ArrayList<Double> speedLists = new ArrayList<>();
        for (int i = 0; i + 1 < data.size(); i++) {
            SensorData ori0 = data.get(i);
            SensorData ori1 = data.get(i + 1);
            if ((currOri.timeStamp - ori0.timeStamp) / 1e9 < 0.2) {
                double escapeTime = (ori1.timeStamp - ori0.timeStamp) / 1e9;
                double dX = (Math.cos(Math.toRadians(ori1.xOrientation))  - Math.cos(Math.toRadians(ori0.xOrientation))) / escapeTime;
                double dY = (Math.cos(Math.toRadians(ori1.yOrientation))  - Math.cos(Math.toRadians(ori0.yOrientation))) / escapeTime;
                double dZ = (Math.cos(Math.toRadians(ori1.zOrientation))  - Math.cos(Math.toRadians(ori0.zOrientation))) / escapeTime;
                double speed = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
                speedLists.add(speed);
            }
        }

        // Mid-num Filtering (window = 5)
        for (int i = speedLists.size() - 1; i >= 4; i--) {
            ArrayList<Double> sortList = new ArrayList<Double>(speedLists.subList(i - 4, i + 1));
            Collections.sort(sortList);
            double mid = sortList.get(sortList.size() / 2);
            speedLists.set(i, mid);
        }
        // Mean-num Filtering (window = 5)
        for (int i = speedLists.size() - 1; i >= 8; i--) {
            double sum = 0;
            for (int j = 0; j < 5; j++) {
                sum += speedLists.get(i - j);
            }
            speedLists.set(i, sum / 5);
        }
        speedLists = new ArrayList<Double>(speedLists.subList(8, speedLists.size()));

        // Calculate the 20% & 80% minimum speed
        double lastSpeed = speedLists.get(speedLists.size() - 1);
        Collections.sort(speedLists);
        double minSpeed50 = speedLists.get((int)(speedLists.size() * 0.5));
        double minSpeed90 = speedLists.get((int)(speedLists.size() * 0.9));

        boolean check = true;
        if (!(-15 <= currOri.xOrientation && currOri.xOrientation <= 15)) {
            check = false;
        }
        if (!((45 <= currOri.yOrientation && currOri.yOrientation <= 135) || (-135 <= currOri.yOrientation && currOri.yOrientation <= -45))) {
            check = false;
        }

        String output = "";
        if (check && minSpeed50 >= 2 && minSpeed90 >= 4 && lastSpeed <= 2) {
            //output = "x= " + (int)currOri.xOrientation + "\ny= " + (int)currOri.yOrientation + "\nSpeed50= " + minSpeed50 + "\nSpeed90= " + minSpeed90 + "\nLastSpeed= " + lastSpeed;
            output = "Shoot!";
        }

        System.out.println(output);
        mainActivity.showText(output);

        return RaiseStatus.Fail;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}