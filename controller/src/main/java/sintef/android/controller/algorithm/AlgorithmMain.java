package sintef.android.controller.algorithm;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import sintef.android.controller.EventTypes;
import sintef.android.controller.sensor.RemoteSensorManager;
import sintef.android.controller.sensor.SensorData;
import sintef.android.controller.sensor.SensorSession;
import sintef.android.controller.sensor.data.AccelerometerData;
import sintef.android.controller.sensor.data.LinearAccelerationData;
import sintef.android.controller.sensor.data.MagneticFieldData;
import sintef.android.controller.sensor.data.RotationVectorData;
import sintef.android.controller.utils.PreferencesHelper;

//import org.apache.commons.collections.bag.SynchronizedSortedBag;

/**
 * Created by samyboy89 on 05/02/15.
 */
public class AlgorithmMain {

    private static AlgorithmMain sAlgorithmMain;
    private Context mContext;

    public static void initializeAlgorithmMaster(Context context)
    {
        sAlgorithmMain = new AlgorithmMain(context);
    }

    private AlgorithmMain(Context context)
    {
        mContext = context;
        EventBus.getDefault().registerSticky(this);
    }

    private boolean phoneAlgorithm(List<AccelerometerData> accData, List<RotationVectorData> rotData, List<MagneticFieldData> geoRotVecData, List<LinearAccelerationData> linearAccelerationData, SensorAlgorithmPack pack, boolean hasWatch)
    {
        //TODO: Find out if the watch is connected. Done, but not sure if it works or not
        int numberOfIterations;
        float[] degs = new float[3];
        float[] rotationMatrix = new float[9];
        double tetaY;
        double tetaZ;
        //System.out.println(rotData.size() + " " + geoRotVecData.size() + " was here");
        if (accData.size() <= rotData.size() && accData.size() <= geoRotVecData.size()) numberOfIterations = accData.size();
        else if (geoRotVecData.size() <= accData.size() && geoRotVecData.size() <= rotData.size()) numberOfIterations = geoRotVecData.size();
        else numberOfIterations = rotData.size();
        for (int i=0; i < numberOfIterations; i++){
            SensorManager.getRotationMatrix(rotationMatrix, null, rotData.get(i).getValues(), geoRotVecData.get(i).getValues());
            SensorManager.getOrientation(rotationMatrix, degs);
            tetaY = degs[2];
            tetaZ = degs[0];
            if (AlgorithmPhone.isFall(accData.get(i).getX(), accData.get(i).getY(), accData.get(i).getZ(), tetaY, tetaZ))
            {
                if (hasWatch) return watchAlgorithm(pack);
                return true;
            }
        }
        return false;
    }

    private boolean watchAlgorithm(SensorAlgorithmPack pack)
    {
        List <AccelerometerData> accData = new ArrayList<>();

        RemoteSensorManager mRemoteSensorManager = RemoteSensorManager.getInstance(mContext);
        mRemoteSensorManager.getBuffer();

        accData = getWatchData(pack);

        return AlgorithmWatch.patternRecognition(accData);
    }


    private List <AccelerometerData> getWatchData (SensorAlgorithmPack pack) {
        List<AccelerometerData> accData = new ArrayList<>();
        for (Map.Entry<SensorSession, List<SensorData>> entry : pack.getSensorData().entrySet()) {
            if (entry.getKey().getSensorDevice().equals(BluetoothClass.Device.WEARABLE_WRIST_WATCH)) {
                if (entry.getKey().getSensorType() == Sensor.TYPE_ACCELEROMETER) {
                    for (int i = 0; i < entry.getValue().size(); i++)
                    {
                        accData.add((AccelerometerData) entry.getValue().get(i).getSensorData());
                    }
                }
            }
        }
        return accData;
    }

    public void onEvent(SensorAlgorithmPack pack)
    {
        boolean hasWatch = false;
        List<AccelerometerData> accelerometerData = new ArrayList<>();
        List<RotationVectorData> rotationVectorData = new ArrayList<>();
        List<MagneticFieldData> geoRotVecData = new ArrayList<>();
        List<LinearAccelerationData> linearAccelerationData = new ArrayList<>();
        for (Map.Entry<SensorSession, List<SensorData>> entry : pack.getSensorData().entrySet()) {
            if (!hasWatch && entry.getKey().getSensorDevice().equals(BluetoothClass.Device.WEARABLE_WRIST_WATCH)) {hasWatch = true;}
            switch (entry.getKey().getSensorType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    for (int i = 0; i < entry.getValue().size(); i++)
                    {
                        accelerometerData.add((AccelerometerData) entry.getValue().get(i).getSensorData());
                    }
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    for (int i = 0; i < entry.getValue().size(); i++)
                    {
                        rotationVectorData.add((RotationVectorData) entry.getValue().get(i).getSensorData());
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    for (int i = 0; i < entry.getValue().size(); i++)
                    {
                        geoRotVecData.add((MagneticFieldData) entry.getValue().get(i).getSensorData());
                    }
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    for (int i = 0; i < entry.getValue().size(); i++)
                    {
                        linearAccelerationData.add((LinearAccelerationData) entry.getValue().get(i).getSensorData());
                    }
                    break;
            }

        }
        boolean isFall = phoneAlgorithm(accelerometerData, rotationVectorData, geoRotVecData, linearAccelerationData, pack, hasWatch);
        if (isFall) {
            if (PreferencesHelper.isFallDetectionEnabled()) {
                EventBus.getDefault().post(EventTypes.FALL_DETECTED);
            }

            EventBus.getDefault().post(EventTypes.FALL_DETECTED_FOR_RECORDING);
        }
    }

    public static AlgorithmMain getsAlgorithmMain() {
        return sAlgorithmMain;
    }

}
