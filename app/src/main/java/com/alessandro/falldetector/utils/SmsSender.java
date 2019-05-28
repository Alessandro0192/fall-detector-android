package com.alessandro.falldetector.utils;


import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;


import java.util.List;

import com.alessandro.falldetector.SettingFragment;


public class SmsSender extends Service {
    private String TAG = this.getClass().getName();

    private SharedPreference sharedPreference;

    private BroadcastReceiver bcrec;
    private BroadcastReceiver bcrec2;

    @Override
    public void onCreate() {
        Log.e(TAG, "Create");
        sharedPreference = new SharedPreference();
        bcrec = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Log.e(TAG, "sms sent");
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Log.e(TAG, "error sending sms");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Log.e(TAG, "sms sending failed because service is currently unavailable");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Log.e(TAG, "sms sending failed because radio was explicitly turned off");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Log.e(TAG, "sms sending failed because no pdu provided");
                }
            }
        };
        registerReceiver(bcrec, new IntentFilter("SENT_SMS"));

        bcrec2 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Log.e(TAG, "SMS delivered");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e(TAG, "SMS not delivered");
                        break;
                }
            }
        };
        registerReceiver(bcrec2, new IntentFilter("SMS_DELIVERED"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "start");
        Location location = intent.getParcelableExtra("last_location");
        sendSmsMessage(location);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bcrec);
        unregisterReceiver(bcrec2);
    }

    private void sendSmsMessage(Location location) {
        //Retrieve contactsList and sms text
        List<Contact> contacts = sharedPreference.getContacts(getApplicationContext());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String smsMessage = sharedPreferences.getString(SettingFragment.KEY_SMS_MESSAGE, null);
        Boolean userWantLocation = sharedPreferences.getBoolean("pref_location_enabled", false);

        if (contacts == null) {
            Log.e(TAG, "empty contact list");
            return;
        }
        if (contacts.isEmpty()) {
            Log.e(TAG, "null contact list");
            return;
        }
        smsMessage = removeStrangeSymbol(smsMessage);
        SmsManager smsManager = SmsManager.getDefault();

        if (userWantLocation && location != null) {
            String lat = String.valueOf(location.getLatitude());
            String lon = String.valueOf(location.getLongitude());
            smsMessage = smsMessage + " Open this link to find the fall position: " + "www.google.it/maps/place/" + lat + "," + lon + "/" + "@" + lat + "," + lon + ",15z ";
            send(contacts, smsMessage, smsManager);
        } else {
            send(contacts, smsMessage, smsManager);
        }
    }

    private void send(List<Contact> contacts, String message, SmsManager smsManager) {
        for (Contact contact : contacts) {
            String number = removePrefixNumber(contact.getNumber());
            try {
                sendPrecedure(number, message, smsManager);
                Log.e(TAG, "Message Sent to Number: " + number + "\nMessage: " + message);
            } catch (Exception e) {
                Log.e(TAG, "Message send error: " + e);
                e.printStackTrace();
            }
        }
        Log.e(TAG, "stop smsservice");
        stopSelf();
    }

    private void sendPrecedure(String number, String message, SmsManager smsManager) {
        PendingIntent piSend = PendingIntent.getBroadcast(this, 0, new Intent("SENT_SMS").setClass(this, SmsSender.class), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED").setClass(this, SmsSender.class), 0);

        smsManager.sendTextMessage(number, null, message, piSend, deliveredPI);


        //PendingIntent piSend = PendingIntent.getBroadcast(this, 0, new Intent("SENT_SMS"), 0);
        //PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED"), 0);

        //smsManager.sendTextMessage(number, null, message, piSend, piDelivered);
    }

    public class MyBinder extends Binder {
        public SmsSender getService() {
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private String removePrefixNumber(String number) {
        if (number.contains(" "))
            number = number.replaceAll(" ", "");
        if (number.contains("-"))
            number = number.replaceAll("-", "");
        if (number.contains("+39"))
            number = number.substring(3);
        return number;
    }

    private String removeStrangeSymbol(String message) {
        if (message.contains("!"))
            message = message.replace("!", "");
        if (message.contains("@"))
            message = message.replace("@", "");
        if (message.contains("#"))
            message = message.replace("#", "");
        if (message.contains("$"))
            message = message.replace("$", "");
        if (message.contains("/"))
            message = message.replace("/", "");
        if (message.contains("^"))
            message = message.replace("^", "");
        if (message.contains("&"))
            message = message.replace("&", "");
        if (message.contains("*"))
            message = message.replace("*", "");
        if (message.contains("("))
            message = message.replace("(", "");
        if (message.contains(")"))
            message = message.replace(")", "");
        if (message.contains("-"))
            message = message.replace("-", "");
        if (message.contains(";"))
            message = message.replace(";", "");
        if (message.contains("+"))
            message = message.replace("+", "");
        if (message.contains("="))
            message = message.replace("=", "");
        if (message.contains(">"))
            message = message.replace(">", "");
        if (message.contains("<"))
            message = message.replace("<", "");
        if (message.contains("{"))
            message = message.replace("{", "");
        if (message.contains("}"))
            message = message.replace("}", "");
        if (message.contains("€"))
            message = message.replace("€", "");
        if (message.contains("%"))
            message = message.replace("%", "");
        if (message.contains("["))
            message = message.replace("[", "");
        if (message.contains("]"))
            message = message.replace("]", "");
        if (message.contains("|"))
            message = message.replace("|", "");
        return message;
    }


}