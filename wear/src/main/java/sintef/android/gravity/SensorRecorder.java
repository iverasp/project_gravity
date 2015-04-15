package sintef.android.gravity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.HashMap;

import sintef.android.controller.Controller;
import sintef.android.controller.common.Constants;
import sintef.android.controller.sensor.SensorDevice;
import sintef.android.controller.sensor.SensorLocation;
import sintef.android.controller.sensor.SensorSession;

public class SensorRecorder implements SensorEventListener {

    private static final String TAG = "G:WEAR:SR";
    private static boolean DBG_RATE = true;

    private HashMap<Integer, SensorSession> mSensorGroup = new HashMap<>();
    private SensorManager mSensorManager;
    private DeviceClient mClient;
    private static SensorRecorder instance;

    private static long time = System.currentTimeMillis();
    private static int times_in_sek = 0;

    public static synchronized SensorRecorder getInstance(Context context) {
        if (instance == null) {
            instance = new SensorRecorder(context.getApplicationContext());
        }

        return instance;
    }

    private SensorRecorder(Context context) {
        if (Controller.DBG) Log.w(TAG, "Started sensor service");

        mClient = DeviceClient.getInstance(context);
        mSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        for (int key : Constants.SENSORS_WEAR.keySet()) {
            addSensorToSystem("watch:" + Constants.SENSORS_WEAR.get(key), key, SensorDevice.WATCH, Constants.WEAR_SENSOR_LOCATION);
        }
    }

    private void addSensorToSystem(String id, int type, SensorDevice device, SensorLocation location) {
        SensorSession sensorSession = new SensorSession(id, type, device, location);
        mSensorGroup.put(type, sensorSession);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(type), Constants.SENSOR_PULL_FREQ);
    }

    public void stopMeasurement() {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (DBG_RATE) {
            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) times_in_sek += 1;
            if (time + 1000 <= System.currentTimeMillis()) {
                Log.wtf(TAG, String.format("%d @ %d", times_in_sek, time));

                time = System.currentTimeMillis();
                times_in_sek = 0;
            }
        }

        mClient.addSensorData(mSensorGroup.get(event.sensor.getType()).getStringFromSession(), event.sensor.getType(), event.accuracy, event.timestamp, event.values);
    }
}
