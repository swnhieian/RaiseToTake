package com.shiweinan.raisetotake;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.nio.DoubleBuffer;
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
    double lastShootTime = 0;
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
        d.xAcc = accelerometerValues[0];
        d.yAcc = accelerometerValues[1];
        d.zAcc = accelerometerValues[2];
        d.xDirection = R[2];
        d.yDirection = R[5];
        d.zDirection = R[8];
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

        SensorData curr = data.get(data.size() - 1);

        // Get speed on unit sphere within 400 ms
        ArrayList<Double> speedLists = new ArrayList<>();
        ArrayList<Double> accLists = new ArrayList<>();
        for (int i = 0; i + 1 < data.size(); i++) {
            SensorData ori0 = data.get(i);
            SensorData ori1 = data.get(i + 1);
            if ((curr.timeStamp - ori0.timeStamp) / 1e9 < 0.4) {
                double escapeTime = (ori1.timeStamp - ori0.timeStamp) / 1e9;
                double dX = (ori1.xDirection - ori0.xDirection) / escapeTime;
                double dY = (ori1.yDirection - ori0.yDirection) / escapeTime;
                double dZ = (ori1.zDirection - ori0.zDirection) / escapeTime;
                double speed = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
                speedLists.add(speed);

                double aX = ori1.xAcc - ori0.xAcc;
                double aY = ori1.yAcc - ori0.yAcc;
                double aZ = ori1.zAcc - ori0.zAcc;
                double acc = Math.sqrt(aX * aX + aY * aY + aZ * aZ);
                accLists.add(acc);
            }
        }

        // Mid-num Filtering (window = 5)
        for (int i = speedLists.size() - 1; i >= 4; i--) {
            ArrayList<Double> sortList = new ArrayList<Double>(speedLists.subList(i - 4, i + 1));
            Collections.sort(sortList);
            double mid = sortList.get(sortList.size() / 2);
            speedLists.set(i, mid);
            sortList = new ArrayList<Double>(accLists.subList(i - 4, i + 1));
            Collections.sort(sortList);
            mid = sortList.get(sortList.size() / 2);
            accLists.set(i, mid);
        }
        // Mean-num Filtering (window = 5)
        for (int i = speedLists.size() - 1; i >= 8; i--) {
            double sum = 0;
            for (int j = 0; j < 5; j++) {
                sum += speedLists.get(i - j);
            }
            speedLists.set(i, sum / 5);
            sum = 0;
            for (int j = 0; j < 5; j++) {
                sum += accLists.get(i - j);
            }
            accLists.set(i, sum / 5);
        }
        speedLists = new ArrayList<Double>(speedLists.subList(8, speedLists.size()));
        accLists = new ArrayList<Double>(accLists.subList(8, accLists.size()));

        // Calculate the 20% & 80% minimum speed
        double lastSpeed = speedLists.get(speedLists.size() - 1);
        double lastAcc = accLists.get(accLists.size() - 1);

        /*String myLog = "Curr speed: " + lastSpeed + "\n";
        myLog += "Speed - Time: ";
        for (int i = 1; i <= 9; i++) {
            myLog += speedLists.get((int)(speedLists.size() * 0.1 * i)) + ", ";
        }
        myLog += "\n";
        myLog += "Acc - Time: ";
        for (int i = 1; i <= 9; i++) {
            myLog += accLists.get((int)(speedLists.size() * 0.1 * i)) + ", ";
        }
        myLog += "\n";*/

        Collections.sort(speedLists);
        double minSpeed50 = speedLists.get((int)(speedLists.size() * 0.2));
        double minSpeed90 = speedLists.get((int)(speedLists.size() * 0.8));
        Collections.sort(accLists);
        double minAcc50 = accLists.get((int)(speedLists.size() * 0.5));
        double minAcc90 = accLists.get((int)(speedLists.size() * 0.9));

        /*myLog += "Speed - Sort: ";
        for (int i = 1; i <= 9; i++) {
            myLog += speedLists.get((int)(speedLists.size() * 0.1 * i)) + ", ";
        }
        myLog += "\n";
        myLog += "Acc - Sort: ";
        for (int i = 1; i <= 9; i++) {
            myLog += accLists.get((int)(speedLists.size() * 0.1 * i)) + ", ";
        }
        myLog += "\n";*/

        boolean check = true;
        if (!(-15 <= curr.xOrientation && curr.xOrientation <= 15)) {
            check = false;
        }
        if (!((60 <= curr.yOrientation && curr.yOrientation <= 120) || (-120 <= curr.yOrientation && curr.yOrientation <= -60))) {
            check = false;
        }

        double shootInterval = (curr.timeStamp - lastShootTime) / 1e9;
        if (shootInterval >= 1) {
            mainActivity.showText("");
        }
        if (check && minSpeed90 / minSpeed50 >= 3 && lastSpeed <= 3 && minAcc90 >= 0.5) {
            if (shootInterval >= 1) {
                //System.out.println(myLog);
                mainActivity.showText("Shoot!");
                mainActivity.takePhoto();
                lastShootTime = curr.timeStamp;
            }
        }

        return RaiseStatus.Fail;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}