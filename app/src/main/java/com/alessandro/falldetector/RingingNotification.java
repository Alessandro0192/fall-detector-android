package com.alessandro.falldetector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
/**
 * When the AlarmRingingActivity is sent to background, a notification will be shown in status bar
 * for user to go back AlarmRingingActivity and dismiss the alarm.
 */
public class RingingNotification {
    private final int NOTIFICATION_ID = 18;

    private Context mContext;
    private NotificationManager mNotificationManager;

    public RingingNotification(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void send() {
        Notification.Builder builder = new Notification.Builder(mContext)
                .setSmallIcon(R.drawable.ic_fall_app_icon_green)
                .setContentTitle("Fall Alarm is ringing")
                .setContentText("Click to dismiss the alarm");

        Intent intent = new Intent(mContext, AlarmActivity.class);
        intent.setAction("com.alessandro.falldetector.TO_FRONT");       // matches intent-filter in Manifest
        PendingIntent notifyIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notifyIntent);

        Notification notification = builder.build();

        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void cancel() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }
}