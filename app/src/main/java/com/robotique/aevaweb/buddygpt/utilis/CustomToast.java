package com.robotique.aevaweb.buddygpt.utilis;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.robotique.aevaweb.buddygpt.R;


public class CustomToast {

    // private static instance variable to hold the singleton instance
    private static volatile CustomToast INSTANCE = null;

    // private constructor to prevent instantiation of the class
    private CustomToast() {}

    // public static method to retrieve the singleton instance
    public static CustomToast getInstance() {
        // Check if the instance is already created
        if(INSTANCE == null) {
            // synchronize the block to ensure only one thread can execute at a time
            synchronized (CustomToast.class) {
                // check again if the instance is already created
                if (INSTANCE == null) {
                    // create the singleton instance
                    INSTANCE = new CustomToast();
                }
            }
        }
        // return the singleton instance
        return INSTANCE;
    }

    private RelativeLayout custom_toast_info;
    private final Handler handler = new Handler();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            hideToast();
        }
    };

    public void showInfo(Activity context, String info, long delay) {

        custom_toast_info = context.findViewById(R.id.custom_toast_info);
        TextView tv = context.findViewById(R.id.info);

        tv.setText(info + "");
        custom_toast_info.setVisibility(View.VISIBLE);

        handler.removeCallbacks(runnable);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(runnable, delay);
    }

    public void hideToast() {
        handler.removeCallbacks(runnable);
        handler.removeCallbacksAndMessages(null);
        if (custom_toast_info != null) {
            custom_toast_info.setVisibility(View.GONE);
        }
    }
}
