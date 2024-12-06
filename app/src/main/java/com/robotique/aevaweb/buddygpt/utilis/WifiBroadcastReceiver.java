package com.robotique.aevaweb.buddygpt.utilis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.robotique.aevaweb.buddygpt.application.BuddyGPTApplication;

@RequiresApi(api = Build.VERSION_CODES.M)
public class WifiBroadcastReceiver extends BroadcastReceiver {
    private Context mContext;
    private ConnectivityManager cm;
    private Context contextAct;
    private int mWifiLevel;
    private final String isConnected = "isConnected";
    BuddyGPTApplication application;

    public void onReceive(final Context context, final Intent intent) {
        mContext = context;
        cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        forceCheckConnexState(mContext);
        if (application.isConnectedToInternet()) {
            application.notifyObservers("isConnected");
        } else {
            application.notifyObservers("isNotConnected");
        }
    }
    /*
     * Vérifier si connecter à internet
     */
    /*
     * Récuperer la vitesse de la connexion wifi
     */

    public int getWifiLevel()
    {
        WifiManager wifiManger = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManger.getConnectionInfo();
        return (wifiInfo.getLinkSpeed());
    }
    /*
     * Récuperer la vitesse de la connexion de données mobile
     */

    public int getMobileConnectionLevel(){
        NetworkCapabilities nc = null;
        nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if(nc != null)
            return ((nc.getLinkUpstreamBandwidthKbps())/1024);
        return 0;
    }
    public  boolean isConnectedToInternet() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;


        final Network n = cm.getActiveNetwork();
        if (n != null) {
            final NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            if( nc != null) {
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    haveConnectedWifi = true;
                    int levelWifi = getWifiLevel();
                    setSignalQualite(levelWifi);
                    application.notifyObservers("mWifiLevel:wifi:"+levelWifi);


                } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    haveConnectedMobile = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int levelMobile = getMobileConnectionLevel();
                        setSignalQualite(levelMobile);
                        application.notifyObservers("mWifiLevel:mobile:"+levelMobile);

                    }

                }
            }

            return haveConnectedWifi || haveConnectedMobile;
        }



        return false;
    }
    public void setAct(Context context) {
        contextAct = context;

    }
    public void setSignalQualite(int lev){
        mWifiLevel = lev;
    }
    private Context getmContext(){
        return contextAct;
    }
    public void forceCheckConnexState(Context context) {
        mContext = context;
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(getmContext()!=null) {
            application = (BuddyGPTApplication) getmContext();
            if(isConnectedToInternet() ){
                application.notifyObservers(isConnected);
                application.notifyObservers("connectChat");


            }
            else {
                application.notifyObservers("isNotConnected");

            }
        }
    }
}

