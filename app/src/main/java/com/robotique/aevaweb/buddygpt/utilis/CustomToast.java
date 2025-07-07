package com.robotique.aevaweb.buddygpt.utilis;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.robotique.aevaweb.buddygpt.R;


public class CustomToast {

    // private static instance variable to hold the singleton instance
    private static CustomToast instance = null;
    private final Handler handler = new Handler();
    private RelativeLayout customToastInfo;

    // private constructor to prevent instantiation of the class
    private CustomToast() {
    }

    // public static method to retrieve the singleton instance
    public static CustomToast getInstance() {
        // Check if the instance is already created
        if (instance == null) {
            // synchronize the block to ensure only one thread can execute at a time
            synchronized (CustomToast.class) {
                // check again if the instance is already created
                    instance = new CustomToast();
            }
        }
        // return the singleton instance
        return instance;
    }

    public void showInfo(Activity context, String info, long delay) {

        customToastInfo = context.findViewById(R.id.custom_toast_info);
        TextView tv = context.findViewById(R.id.info);

        tv.setText(info);
        customToastInfo.setVisibility(View.VISIBLE);

        handler.removeCallbacks(runnable);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(runnable, delay);
    }    private final Runnable runnable = this::hideToast;

    public void hideToast() {
        handler.removeCallbacks(runnable);
        handler.removeCallbacksAndMessages(null);
        if (customToastInfo != null) {
            customToastInfo.setVisibility(View.GONE);
        }
    }


}
