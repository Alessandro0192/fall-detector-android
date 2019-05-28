package com.alessandro.falldetector;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;


public class SensorService extends Service {
    private static final String tag = "SensorService";

    private static SensorManager sensorManager;
    private static List<Sensor> sensorList;
    private static SensorListener sensorListener;

    private static boolean isRunning = false;

    NotificationManager notificationManager;

    private WakeLock wakeLock;

    AbstractAlgorithm abstract_algorithm;

    public SensorService() {
        super();
        if (isRunning == false) {
            isRunning = true;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(tag, "Service Create");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        if (wakeLock != null) {
            wakeLock.acquire();
            Log.e(tag, "Service WakeLock acquire()");
        }

        //notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // build notification
        Notification n = new Notification.Builder(this)
                .setContentTitle("Fall Detector")
                .setContentText("Fall detection service is running")
                .setSmallIcon(R.drawable.ic_fall_app_icon_green)
                .setContentIntent(pIntent)
                .setAutoCancel(false)
                .build();
        n.flags = Notification.FLAG_ONGOING_EVENT;

        notificationManager.notify(0, n);

        abstract_algorithm = new MyFallDetectionAlgorithm(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("SensonrSevice", "Service StartCommand");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        sensorListener = new SensorListener(this);

        if (sensorList.size() > 0) {
            Sensor sensor = sensorList.get(0);
            sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);    //SENSOR_DELAY_NORMAL = 0.2 sec  quindi frequenza =
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null) {
            wakeLock.release();
            Log.e(tag, "Service WakeLock release()");
        }
        Log.e("SensonrSevice", "Service Destroy");

        sensorManager.unregisterListener(sensorListener, sensorList.get(0));
        sensorManager.unregisterListener(sensorListener);
        isRunning = false;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
        super.onDestroy();
    }


    private class SensorListener implements SensorEventListener {

        SensorListener(Context context) {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(final SensorEvent se) {

            abstract_algorithm.algoritmo(se);
        }
    }

    public class MyBinder extends Binder {
        public SensorService getService() {
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
