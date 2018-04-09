package com.shiweinan.raisetotake;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetic;
    private SensorProcessor sensorProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorProcessor = new SensorProcessor(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        registerSensors();
    }

    public void showText(String str) {
        System.out.println(str);
        ((TextView)findViewById(R.id.text)).setText(str);
    }

    private void registerSensors() {
        sensorManager.registerListener(sensorProcessor, accelerometer,
            SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(sensorProcessor, gyroscope,
            SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(sensorProcessor, magnetic,
            SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onResume() {
        super.onResume();
        registerSensors();
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorProcessor);
    }
}
