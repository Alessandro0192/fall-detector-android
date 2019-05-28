package com.alessandro.falldetector;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorEvent;
import android.util.Log;

public abstract class AbstractAlgorithm {

    protected Context mContext;

    public AbstractAlgorithm(Context context){
        this.mContext = context;
    }

    public abstract void algoritmo(SensorEvent se);

    protected void startAlarmActivity(){
        Log.e("FallDetectorAlgh", "create broadcast alarm");

        Intent alarmIntent = new Intent(mContext, AlarmReceiver.class);
        //Broadcast PendingIntent for AlarmReceiver
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, pendingIntent);
    }


}


