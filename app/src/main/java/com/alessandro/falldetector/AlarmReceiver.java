package com.alessandro.falldetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent in) {
        Log.d(getClass().getName(), "onReceive()");

        Intent intent = new Intent(context, AlarmActivity.class);
        intent.putExtras(in);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
