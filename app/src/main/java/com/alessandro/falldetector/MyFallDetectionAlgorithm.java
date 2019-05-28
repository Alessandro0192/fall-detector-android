package com.alessandro.falldetector;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

import android.util.Log;

import com.alessandro.falldetector.utils.SharedPreference;

import java.util.Iterator;
import java.util.Vector;

public class MyFallDetectionAlgorithm extends AbstractAlgorithm {


    private static final String tag = "SensorService";
    private static final boolean debug = true;

    private static final float UFT_min = (float) 15.7600;

    private static final float LFT_max = (float) 8.7300;

    private static final float ori_min = (float) 48.3900;

    private static final long tre_max = 313000;
    private static final long tre_min = 40900;

    private static float curr_rss;
    private static float prev_rss;
    private static float curr_x;
    private static float prev_x;
    private static float curr_y;
    private static float prev_y;
    private static float curr_z;
    private static float prev_z;
    private static float curr_ori;
    private static float prev_ori;
    private static int counter;
    private static int counter_2;
    private static long timestamp_fall_detected, LFT_timestamp, UFT_timestamp;
    private static Vector lft_timestamps;
    private static boolean detectedFall = false;

    private static boolean LFT_exceeded = false;
    private static boolean UFT_exceeded = false;

    SharedPreference sharedPreference;

    public MyFallDetectionAlgorithm(Context context) {
        super(context);
        counter = 0;
        counter_2 = 0;
        timestamp_fall_detected = 0;
        curr_rss = 0;
        prev_rss = 0;
        curr_x = 0;
        prev_x = 0;
        curr_y = 0;
        prev_y = 0;
        curr_z = 0;
        prev_z = 0;
        curr_ori = 0;
        prev_ori = 0;
        sharedPreference = new SharedPreference();
    }

    /**
     * Calculates the scalar product of two vectors
     *
     * @param a    x-axis for vector 1
     * @param b    y-axis for vector 1
     * @param c    z-axis for vector 1
     * @param d    x-axis for vector 2
     * @param e    y-axis for vector 2
     * @param f    z-axis for vector 2
     * @param rss1 size of vector 1
     * @param rss2 size of vector 2
     * @return angle in degrees
     */
    float orientation(float a, float b, float c, float d, float e, float f, float rss1, float rss2) {
        float ori = (float) Math.toDegrees(Math.acos(((a * d) + (b * e) + (c * f)) / (rss1 * rss2)));
        if (ori >= 0.0) return ori;
        else return (float) 0.0;
    }

    @Override
    public void algoritmo(SensorEvent se) {
        if (se.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            curr_x = se.values[0];
            curr_y = se.values[1];
            curr_z = se.values[2];
            curr_rss = (float) Math.sqrt((curr_x * curr_x) + (curr_y * curr_y) + (curr_z * curr_z));
            /* Orientation between position at timestamp x and timestamp(x-1) */
            float ori = orientation(curr_x, curr_y, curr_z, prev_x, prev_y, prev_z, curr_rss, prev_rss);
            curr_ori = ori;
            prev_x = curr_x;
            prev_y = curr_y;
            prev_z = curr_z;
            prev_rss = curr_rss;

            if (debug)
                Log.i(tag, "status per tick:" + counter + ":orientation:" + ori + ":RSS:" + (curr_rss));

            /* This part counts the inactivity period */
            if ((ori < 3) && (prev_ori < 3)) {
                counter_2++;
                if (debug) Log.i(tag, "orientation below 3:" + ori);
            } else
                counter_2 = 0;

            prev_ori = curr_ori;

            /* An alarm is issued if a fall was detected followed by inactivity period */
            if ((counter_2 > 3) && (((se.timestamp / 1000) - timestamp_fall_detected) > 520000 && ((se.timestamp / 1000) - timestamp_fall_detected) < 3500000) && detectedFall) {
                Log.e("SensonrSevice", "Sensor FALL DETECTED");

                //start alarm activity
                startAlarmActivity();

                if (debug)
                    Log.i(tag, "new activity:" + ((se.timestamp / 1000) - timestamp_fall_detected) + ":" + counter_2);
                timestamp_fall_detected = 0;
                counter_2 = 0;
                detectedFall = false;

                //stop service
                stopSensorService();
            }

            /* LFT check */
            if (curr_rss <= LFT_max && ori >= ori_min && UFT_exceeded == false) {
                if (debug) Log.i(tag, "LTF Lower fall threshold exceeded");
                if (LFT_exceeded == false) {
                    lft_timestamps = new Vector(1, 1);
                    LFT_timestamp = (se.timestamp) / 1000;
                }
                lft_timestamps.add((long) (se.timestamp) / 1000);
                LFT_exceeded = true;
                if (debug)
                    Log.i(tag, "LFT:" + ((se.timestamp) / 1000) + ":" + ori + "::" + curr_rss);
            }

            /* UFT check */
            if (curr_rss >= UFT_min && LFT_exceeded == true && ori >= 5) {
                if (debug) Log.i(tag, "UTF Upper fall threshold exceeded!");
                UFT_exceeded = true;
                UFT_timestamp = (se.timestamp) / 1000;
                Iterator it = lft_timestamps.iterator();
                while (it.hasNext()) {
                    Long tmp = (Long) it.next();
                    if (tmp != 0 && (UFT_timestamp - tmp) >= tre_min && (UFT_timestamp - tmp) <= tre_max) {
                        detectedFall = true;
                        timestamp_fall_detected = (se.timestamp) / 1000;
                        if (debug)
                            Log.i(tag, "Fall:" + ":" + ori + "::" + curr_rss + "::" + (UFT_timestamp - tmp));
                    }
                }

                counter = 0;
                LFT_exceeded = false;
                UFT_exceeded = false;
                if (debug)
                    Log.i(tag, "UFT:" + ((se.timestamp) / 1000) + ":" + ori + "::" + curr_rss + ":::" + (UFT_timestamp - LFT_timestamp));
            }
        }
    }

    private void stopSensorService(){
        mContext.stopService(new Intent(mContext, SensorService.class));
        sharedPreference.saveSwitchState(mContext, false);
    }

}
