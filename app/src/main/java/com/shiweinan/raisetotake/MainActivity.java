package com.shiweinan.raisetotake;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Picture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetic;
    private SensorProcessor sensorProcessor;

    private final int CAMERA_CODE = 1;
    private boolean showPreview = false;

    private Camera camera;
    private CameraPreview cameraPreview;
    FrameLayout preview;
    private static File mediaFile = null;

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

        checkCameraPermission();
        camera = getCameraInstance();
        preview = (FrameLayout) findViewById(R.id.preview);

        cameraPreview = new CameraPreview(this, camera);
        preview.addView(cameraPreview);
        preview.setVisibility(showPreview ? View.VISIBLE : View.INVISIBLE);
        prepareCamera();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    public void prepareCamera() {
        /*Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(preview.getWidth(), preview.getHeight());
        preview.requestLayout();
        camera.setParameters(parameters);*/
        camera.startPreview();
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
    private void checkCameraPermission() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //check
                } else {
                    //exit
                    finish();
                }
                break;
            default:
                break;
        }
    }
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }
    public void takePhoto(View view) {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            public File pictureFile;
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                String TAG = "Capturing Picture";
                pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null){
                    Log.d(TAG, "Error creating media file, check storage permissions: ");
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(bytes);
                    fos.close();
                    //show photo in the view
                    ImageView imageView = (ImageView)findViewById(R.id.imageView);
                    FileInputStream fis = new FileInputStream(mediaFile);
                    Bitmap bitmap = BitmapFactory.decodeStream(fis);
                    imageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }
                prepareCamera();
            }
        };
        camera.takePicture(null, null, picture);
        //show picture on the list
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        return mediaFile;
    }
}
