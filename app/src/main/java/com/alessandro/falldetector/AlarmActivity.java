package com.alessandro.falldetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.alessandro.falldetector.utils.MailSender;
import com.alessandro.falldetector.utils.SmsSender;

public class AlarmActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();
    private static final int RINGING_EXPIRED_TIMEOUT = 101 * 1000;

    private PowerManager.WakeLock mWakeLock;
    private RingtonePlayer mRingtonePlayer;
    private RingingNotification mRingingNotification;
    private Vibrator mVibrator;

    private Location lastLocation;

    private MailSender mailSender;

    private TextView secondRemaining;

    private boolean cancelled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_alarm);

        Log.e(TAG, "OnCreate() Alarm");

        cancelled = false;
        lastLocation = null;

        mailSender = new MailSender(getApplicationContext());

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        Button send_alarm_button = (Button) findViewById(R.id.send_alarm);
        Button false_fall_button = (Button) findViewById(R.id.false_fall);
        secondRemaining = (TextView) findViewById(R.id.second_remaining);

        send_alarm_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelled = true;
                //startSmsService();
                mailSender.sendMail(lastLocation);
                Log.e(TAG, "Send messages");
                finish();
            }
        });

        false_fall_button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                cancelled = true;
                Log.e(TAG, "False alarm");
                finish();
            }
        });

        // Hide status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }

        wakeupScreen();
        startVibrating();
        playRingtone();

        setIntentFilter();
        startLocationService();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final long timeToDismiss = Integer.parseInt(sharedPreferences.getString("pref_alarm_timer", "20"));
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              new CountDownTimer(timeToDismiss * 1000, 1000) {
                                  @Override
                                  public void onFinish() {
                                      if (cancelled) {
                                          stopVibrating();
                                          Log.e(TAG, "Chronometer finish cancelled " + cancelled);
                                          finish();
                                      } else {
                                          stopVibrating();
                                          Log.e(TAG, "Chronometer finish cancelled" + cancelled + "...So SEND MESSAGE!");
                                          //startSmsService();
                                          mailSender.sendMail(lastLocation);
                                          finish();
                                      }
                                  }

                                  @Override
                                  public void onTick(long millisUntilFinished) {
                                      secondRemaining.setText(String.valueOf(millisUntilFinished / 1000));
                                  }
                              }.start();
                          }
                      }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "OnResume() Alarm");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "OnPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "OnStop()");

        // Send notification to allow user to dismiss the alarm later
        mRingingNotification = new RingingNotification(this);
        mRingingNotification.send();

        releaseScreen();

    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "OnDestroy()");
        super.onDestroy();

        stopRingtone();
        stopVibrating();

        unregisterReceivers();

        if (mRingingNotification != null) {
            mRingingNotification.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "Back button pressed");
        //no action for back button (don't dismiss alarm)
    }

    private void wakeupScreen() {
        /* Ensure wakelock release */
        Runnable releaseWakelock = new Runnable() {
            @Override
            public void run() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

                if (mWakeLock != null && mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        };
        new Handler().postDelayed(releaseWakelock, RINGING_EXPIRED_TIMEOUT);

        /* Set the window to keep screen on */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        /* Acquire wakelock */
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (mWakeLock == null) {
            mWakeLock = pm.newWakeLock((PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), getClass().getName());
        }

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    private void releaseScreen() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void startVibrating() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean vibration = sharedPreferences.getBoolean("pref_vibration", false);
        if (vibration) {
            if (mVibrator == null) {
                mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }
            mVibrator.vibrate(new long[]{0, 200, 500}, 1);
            Log.i(getClass().getName(), "Vibrating");
        }
    }

    private void stopVibrating() {
        if (mVibrator != null) {
            mVibrator.cancel();
        }
    }

    private void playRingtone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String ringtone = sharedPreferences.getString("pref_alarm_ringtone", null);
        Uri uri = null;
        if (ringtone == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            uri = Uri.parse(ringtone);
        }
        if (mRingtonePlayer == null) {
            mRingtonePlayer = new RingtonePlayer(this, uri);
            mRingtonePlayer.play();
        }
    }

    private void stopRingtone() {
        if (mRingtonePlayer != null) {
            mRingtonePlayer.stop();
        }
    }

    private void setIntentFilter() {
        IntentFilter intentFilterFound = new IntentFilter();

        intentFilterFound.addAction(LocationService.LOCATION_FOUND);

        registerReceiver(locationFoundReceiver, intentFilterFound);

        IntentFilter intentFilterNotFound = new IntentFilter();

        intentFilterNotFound.addAction(LocationService.LOCATION_NOT_FOUND);

        registerReceiver(locationNotFoundReceiver, intentFilterNotFound);
    }

    private void unregisterReceivers() {
        unregisterReceiver(locationFoundReceiver);
        unregisterReceiver(locationNotFoundReceiver);
    }

    private void startLocationService() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean userWantLocation = sharedPreferences.getBoolean("pref_location_enabled", false);

        if (userWantLocation) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            startService(intent);
        }
    }

    //BroadCastReceiver for LocationService
    private BroadcastReceiver locationFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            lastLocation = intent.getParcelableExtra(LocationService.CURRENT_LOCATION);
            Log.e("AlarmActivity", "Location found");
        }
    };
    private BroadcastReceiver locationNotFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            lastLocation = null;
            Log.e("AlarmActivity", "Location not found");
        }
    };

    private void startSmsService() {
        startService(new Intent(AlarmActivity.this, SmsSender.class).putExtra("last_location", lastLocation));
    }
}