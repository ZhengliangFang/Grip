package com.example.grip;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class SensorController implements SensorEventListener {

    private SensorManager manager;

    public interface SensorCallback{
        //Acceleration Sensor
        void refreshAcc(float[] accs);

        //System Built-in step counter sensor
        void refreshStep(int step);
    }

    private SensorCallback callback;

    public SensorController(SensorManager manager, SensorCallback callback){
        this.manager=manager;
        this.callback=callback;
    }

    //Register sensors
    public boolean registerSensor(int type,int speed){
        if (manager==null) return false;

        Sensor sensor=manager.getDefaultSensor(type);
        manager.registerListener(this,sensor,speed);
        return true;
    }

    //Callback when sensor changes
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (callback==null) return;
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                callback.refreshAcc(event.values.clone());
                break;
            case Sensor.TYPE_STEP_COUNTER:
                callback.refreshStep((int)event.values[0]);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}