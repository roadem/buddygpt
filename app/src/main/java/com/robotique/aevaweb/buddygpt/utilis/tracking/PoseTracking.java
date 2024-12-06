package com.robotique.aevaweb.buddygpt.utilis.tracking;

import static com.bfr.buddy.ui.shared.GazePosition.CENTER;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.bfr.buddysdk.BuddySDK;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.List;

public class PoseTracking {
    private static final String TAG_TRACKING = "BuddyGPT_TRACKING_INFO";
    private static final String TAG_TRACKING_DEBUG = "BuddyGPT_DEBUG_TRACKING";
    private float x0, x1, x2, x3, x4, x5, x6, x7, x8, x11, x12, x23, x24, y0, y1, y2, y3, y4, y5, y6, y7, y8, y11, y12, y23, y24, Eod, eog, degx, degy, b1, z7, z8, centreDuVisageX, centreDuVisageY, dLeft, dRight;
    private Handler handler = new Handler();
    private Runnable runnable;
    private IUsbCommadRsp iUsbCommadRsp = new IUsbCommadRsp.Stub() {
        @Override
        public void onSuccess(String success) throws RemoteException {}
        @Override
        public void onFailed(String error) throws RemoteException {}
    };

    public Integer getLandmarksCamera(PoseLandmarkerResult results) {
        int poseIndex = 0, landmarkDansLecran = 0;
        for (List<NormalizedLandmark> poseLandmarks : results.landmarks()) {
            Log.d(TAG_TRACKING_DEBUG, "Pose Index: " + poseIndex++);
            int landmarkIndex = 0;
            for (NormalizedLandmark landmark : poseLandmarks) {
                float x = landmark.x();
                float y = landmark.y();
                if ((x < 0.9) && (y < 0.9)) {
                    ++landmarkDansLecran;
                }
            }
            Log.d(TAG_TRACKING_DEBUG, "landmarkDansLecran: " + landmarkDansLecran);
        }
        return landmarkDansLecran;
    }

    public Float[] suivi(PoseLandmarkerResult results) {
        Float[] res = {(float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0, (float) 0};
        if (results != null) {
            List<List<NormalizedLandmark>> landmarksList = results.landmarks();
            if (landmarksList != null && !landmarksList.isEmpty()) {
                List<NormalizedLandmark> landmarks = landmarksList.get(0);
                // Assurer que la liste de landmarks n'est pas vide
                if (!landmarks.isEmpty()) {
                    // Landmarks extraction
                    NormalizedLandmark landmark0 = landmarks.get(0);
                    x0 = landmark0.x();
                    y0 = landmark0.y();
                    NormalizedLandmark landmark1 = landmarks.get(1);
                    x1 = landmark1.x();
                    y1 = landmark1.y();
                    NormalizedLandmark landmark2 = landmarks.get(2);
                    x2 = landmark2.x();
                    y2 = landmark2.y();
                    NormalizedLandmark landmark3 = landmarks.get(3);
                    x3 = landmark3.x();
                    y3 = landmark3.y();
                    NormalizedLandmark landmark4 = landmarks.get(4);
                    x4 = landmark4.x();
                    y4 = landmark4.y();
                    NormalizedLandmark landmark5 = landmarks.get(5);
                    x5 = landmark5.x();
                    y5 = landmark5.y();
                    NormalizedLandmark landmark6 = landmarks.get(6);
                    x6 = landmark6.x();
                    y6 = landmark6.y();
                    NormalizedLandmark landmark7 = landmarks.get(7);
                    x7 = landmark7.x();
                    y7 = landmark7.y();
                    NormalizedLandmark landmark8 = landmarks.get(8);
                    x8 = landmark8.x();
                    y8 = landmark8.y();
                    NormalizedLandmark landmark11 = landmarks.get(11);
                    x11 = landmark11.x();
                    y11 = landmark11.y();
                    NormalizedLandmark landmark12 = landmarks.get(12);
                    x12 = landmark12.x();
                    y12 = landmark12.y();
                    NormalizedLandmark landmark23 = landmarks.get(23);
                    x23 = landmark23.x();
                    y23 = landmark23.y();
                    NormalizedLandmark landmark24 = landmarks.get(24);
                    x24 = landmark24.x() ;
                    y24 = landmark24.y();
                    // Compute distances and angles
                    float eog = (float) Math.sqrt(Math.pow((x7 - x1), 2) + Math.pow((y7 - y1), 2));
                    float Eod = (float) Math.sqrt(Math.pow((x4 - x8), 2) + Math.pow((y4 - y8), 2));
                    // Distance between shoulders landmarks and frame center
                    float degx = x0 * 60f - 30f;
                    float degy = y0 * 60f - 30f;
                    float dLeft = (float) Math.sqrt(Math.pow((x23 - x11), 2) + Math.pow((y23 - y11), 2));
                    float dRight = (float) Math.sqrt(Math.pow((x24 - x12), 2) + Math.pow((y24 - y12), 2));
                    float b1 = (float) Math.sqrt(Math.pow((x0 - x2), 2) + Math.pow((y0 - y2), 2));

                    res[0] = eog;
                    res[1] = Eod;
                    res[2] = degx;
                    res[3] = degy;
                    res[4] = x0;
                    res[5] = x7;
                    res[6] = x8;
                    res[7] = y0;
                    res[8] = y7;
                    res[9] = y8;
                    res[10] = dLeft*1000;
                    res[11] = dRight;
                    res[12] = b1;

                    // Accédez aux autres landmarks de la même manière
                    Log.i(TAG_TRACKING_DEBUG, "Landmarks list is not empty");
                    // Faites vos calculs avec les landmarks ici
                } else {
                    Log.e(TAG_TRACKING_DEBUG, "Landmarks list is empty");
                }
            } else {
                Log.e(TAG_TRACKING_DEBUG, "Landmarks list is null or empty");
            }
        } else {
            Log.e(TAG_TRACKING_DEBUG, "PoseLandmarkerResult is null");
        }
        return res;
    }

    public String directionVisage(float dxEG, float dyEG, float dxED, float dyED, float dxN, float dyN, float lang) {

        if (dxEG > dxN) {
            if (dxN > dxED) {
                if (lang > 120) {
                    if (dyED < dyN && dyEG < dyN) {
                        Log.i(TAG_TRACKING_DEBUG, "regard haut");
                        return "HAUT";
                    }
                    Log.i(TAG_TRACKING_DEBUG, "cible regaede la camera");
                    return "CAMERA";

                } else {
                    if (dyED > dyN && dyEG > dyN) {
                        Log.i(TAG_TRACKING_DEBUG, "regard haut");
                        return "HAUT";
                    }
                    Log.i(TAG_TRACKING_DEBUG, "cible regaede la camera");
                    return "CAMERA";
                }
            }
            Log.i(TAG_TRACKING_DEBUG, "regarde lun des cotes");
            return "COTE";
        }
        Log.i(TAG_TRACKING_DEBUG, "regarde lun des cotes");
        return "COTE";

    }

    public void Rotation(float degx, boolean boddy) {
        if(handler!=null && runnable!=null){
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        }
        handler.post(runnable = new Runnable() {
            int speedR, speedL;
            @Override
            public void run() {
                try{
                    if (degx > 3 || degx < -3) {
                        speedR = (int) 300;
                        speedL = (int) 300;
                    }
                    if (degx < -3) {
                        speedR = (int) (speedR + (-degx * 20));
                        speedL = -speedR;
                    }
                    if (degx > 3) {
                        speedL = (int) (speedR + (degx * 20));
                        speedR = -speedL;
                    }
                    if (boddy && (speedR == -speedL)) {
                        if (degx < -5) {
                            BuddySDK.USB.setWheelSpeed(speedL, speedR, 0, 0, iUsbCommadRsp);
                        } else {
                            if (degx > 5) {
                                BuddySDK.USB.setWheelSpeed(speedL, speedR, 0, 0, iUsbCommadRsp);
                            } else {
                                BuddySDK.USB.emergencyStopMotors(iUsbCommadRsp);
                            }
                        }
                    } else {
                        if (degx < -5 || degx > 5) {
                            BuddySDK.USB.buddySayNoStraight(degx, iUsbCommadRsp);
                        } else {
                            BuddySDK.USB.buddyStopNoMove(iUsbCommadRsp);
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void yesTracking(float degy) {
        if(handler!=null && runnable!=null){
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        }
        handler.post(runnable = new Runnable() {
            @Override
            public void run() {
                try{
                    if (degy < -5) {
                        BuddySDK.USB.buddySayYesStraight(-degy, iUsbCommadRsp);
                    } else {
                        if (degy > 5) {
                            BuddySDK.USB.buddySayYesStraight(-degy, iUsbCommadRsp);
                        } else {
                            BuddySDK.USB.buddyStopYesMove(iUsbCommadRsp);
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void look_at(float degx, float degy) {
        try{
            float look_X = map(-degx, 0.0f, 1000.0f, -25, 25);
            float look_Y = map(degy, 0.0f, 600.0f, -20, 20);
            BuddySDK.UI.lookAtXY(look_X, look_Y, false);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void centerHead() {
        Log.d(TAG_TRACKING, "centerHead()");
        try{
            BuddySDK.USB.buddySayNo(140, 0, iUsbCommadRsp);
            BuddySDK.USB.buddySayYes(49, 0, iUsbCommadRsp);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void lookAtCenter() {
        Log.d(TAG_TRACKING, "lookAtCenter()");
        try{
            BuddySDK.UI.lookAt(CENTER, true);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void stopMovingAndCancelRunnables(){
        Log.d(TAG_TRACKING, "stopMovingAndCancelRunnables()");
        if(handler!=null && runnable!=null){
            handler.removeCallbacks(runnable);
            handler.removeCallbacksAndMessages(null);
        }
        try{
            BuddySDK.USB.buddyStopYesMove(iUsbCommadRsp);
            BuddySDK.USB.buddyStopNoMove(iUsbCommadRsp);
            BuddySDK.USB.emergencyStopMotors(iUsbCommadRsp);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * La fonction map permet de mapper une valeur Xa de l'intervalle A[fromMin, fromMax] à une valeur Xb de l'intervalle B[toMin, toMax]
     * (normalisation en mathématique)
     *
     * @param input   : Xa
     * @param toMin   : le min de l'intervalle B
     * @param toMax   : le max de l'intervalle B
     * @param fromMin : le min de l'intervalle A
     * @param fromMax : le max de l'intervalle A
     * @return la fonction renvoi le mappage Xb de Xa.
     */
    private float map(float input, float toMin, float toMax, float fromMin, float fromMax) {
        return (float) (((input - fromMin) / (fromMax - fromMin)) * (toMax - toMin) + toMin);
    }

}
