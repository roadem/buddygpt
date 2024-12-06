package com.robotique.aevaweb.buddygpt.utilis.tracking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.Arrays;
import java.util.List;

public class PoseLandmarkerHelper {

    private static final String TAG = "PoseLandmarkerHelper"; // Déclaration de la constante TAG utilisée pour la journalisation
    public static final int DELEGATE_CPU = 0; // Déclaration de la constante pour le délégué CPU
    public static final int DELEGATE_GPU = 1; // Déclaration de la constante pour le délégué GPU
    public static final float DEFAULT_POSE_DETECTION_CONFIDENCE = 0.85F; // Déclaration de la confiance par défaut pour la détection de pose
    public static final float DEFAULT_POSE_TRACKING_CONFIDENCE = 0.8F; // Déclaration de la confiance par défaut pour le suivi de pose
    public static final float DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.8F; // Déclaration de la confiance par défaut pour la présence de pose
    public static final int GPU_ERROR = 1; // Déclaration de la constante pour une erreur GPU
    public static final int MODEL_POSE_LANDMARKER_FULL = 0; // Déclaration de la constante pour le modèle complet de détection de pose
    public static final int MODEL_POSE_LANDMARKER_LITE = 1; // Déclaration de la constante pour le modèle léger de détection de pose
    public static final int MODEL_POSE_LANDMARKER_HEAVY = 2; // Déclaration de la constante pour le modèle lourd de détection de pose

    private PoseLandmarker poseLandmarker; // Déclaration de l'objet PoseLandmarker pour la détection de pose
    private float minPoseDetectionConfidence; // Déclaration de la confiance minimale de détection de pose
    private float minPoseTrackingConfidence; // Déclaration de la confiance minimale de suivi de pose
    private float minPosePresenceConfidence; // Déclaration de la confiance minimale de présence de pose
    private int currentModel; // Déclaration du modèle actuel utilisé
    private int currentDelegate; // Déclaration du délégué actuel utilisé
    private RunningMode runningMode; // Déclaration du mode d'exécution actuel
    private Context context; // Déclaration du contexte
    private LandmarkerListener poseLandmarkerHelperListener; // Déclaration de l'écouteur d'aide au Pose Landmarker

    // Constructeur de la classe PoseLandmarkerHelper
    public PoseLandmarkerHelper(Context context, RunningMode runningMode, float minPoseDetectionConfidence, float minPoseTrackingConfidence, float minPosePresenceConfidence, int currentDelegate, int currentModel, LandmarkerListener poseLandmarkerHelperListener) {
        this.context = context;
        this.runningMode = runningMode;
        this.minPoseDetectionConfidence = minPoseDetectionConfidence;
        this.minPoseTrackingConfidence = minPoseTrackingConfidence;
        this.minPosePresenceConfidence = minPosePresenceConfidence;
        this.currentDelegate = currentDelegate;
        this.currentModel = currentModel;
        this.poseLandmarkerHelperListener = poseLandmarkerHelperListener;
        setupPoseLandmarker(); // Appel de la méthode pour configurer le Pose Landmarker
    }

    // Méthode pour libérer le Pose Landmarker
    public void clearPoseLandmarker() {
        if (poseLandmarker != null) {
            poseLandmarker.close();
            poseLandmarker = null;
        }
    }

    // Méthode pour vérifier si le Pose Landmarker est libéré
    public boolean isClose() {
        return poseLandmarker == null;
    }

    // Méthode pour configurer le Pose Landmarker
    public void setupPoseLandmarker() {
        BaseOptions.Builder baseOptionBuilder = BaseOptions.builder(); // Création du constructeur d'options de base

        // Configuration du délégué en fonction de la valeur actuelle
        if (currentDelegate == DELEGATE_CPU) {
            baseOptionBuilder.setDelegate(Delegate.CPU); // Utilisation du délégué CPU
        } else if (currentDelegate == DELEGATE_GPU) {
            baseOptionBuilder.setDelegate(Delegate.GPU); // Utilisation du délégué GPU
        }

        String modelName;
        // Sélection du modèle en fonction de la valeur actuelle
        switch (currentModel) {
            case MODEL_POSE_LANDMARKER_FULL:
                modelName = "pose_landmarker_full.task";
                break;
            case MODEL_POSE_LANDMARKER_LITE:
                modelName = "pose_landmarker_lite.task";
                break;
            case MODEL_POSE_LANDMARKER_HEAVY:
                modelName = "pose_landmarker_heavy.task";
                break;
            default:
                modelName = "pose_landmarker_full.task";
                break;
        }

        baseOptionBuilder.setModelAssetPath(modelName); // Définition du chemin d'accès au modèle dans les options de base

        // Vérification des préconditions en fonction du mode d'exécution
        if (runningMode == RunningMode.LIVE_STREAM && poseLandmarkerHelperListener == null) {
            throw new IllegalStateException("poseLandmarkerHelperListener must be set when runningMode is LIVE_STREAM.");
        }

        try {
            BaseOptions baseOptions = baseOptionBuilder.build(); // Construction des options de base
            PoseLandmarker.PoseLandmarkerOptions.Builder optionsBuilder =
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions) // Configuration des options de base
                            .setMinPoseDetectionConfidence(minPoseDetectionConfidence) // Définition de la confiance minimale de détection de pose
                            .setMinTrackingConfidence(minPoseTrackingConfidence) // Définition de la confiance minimale de suivi de pose
                            .setMinPosePresenceConfidence(minPosePresenceConfidence) // Définition de la confiance minimale de présence de pose
                            .setRunningMode(runningMode); // Définition du mode d'exécution

            // Configuration des écouteurs de résultats et d'erreurs pour le mode LIVE_STREAM
            if (runningMode == RunningMode.LIVE_STREAM) {
                optionsBuilder
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError);
            }

            PoseLandmarker.PoseLandmarkerOptions options = optionsBuilder.build(); // Construction des options du Pose Landmarker
            poseLandmarker = PoseLandmarker.createFromOptions(context, options); // Création du Pose Landmarker à partir des options
        } catch (IllegalStateException e) {
            if (poseLandmarkerHelperListener != null) {
                poseLandmarkerHelperListener.onError("Pose Landmarker failed to initialize. See error logs for details", 1);
            }
            Log.e(TAG, "MediaPipe failed to load the task with error: " + e.getMessage());
        } catch (RuntimeException e) {
            if (poseLandmarkerHelperListener != null) {
                poseLandmarkerHelperListener.onError("Pose Landmarker failed to initialize. See error logs for details", GPU_ERROR);
            }
            Log.e(TAG, "Image classifier failed to load model with error: " + e.getMessage());
        }
    }

    // Méthode pour détecter une diffusion en direct
    public void detectLiveStream(ImageProxy imageProxy) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw new IllegalArgumentException("Attempting to call detectLiveStream while not using RunningMode.LIVE_STREAM");
        }
        long frameTime = SystemClock.uptimeMillis(); // Récupération du temps de trame

        // Création d'un tampon Bitmap à partir de l'image capturée par la caméra
        Bitmap bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
        imageProxy.getPlanes()[0].getBuffer().rewind();
        bitmapBuffer.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
        imageProxy.close();

        // Création d'une matrice de rotation
        Matrix matrix = new Matrix();
        matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());

        // Mise à l'échelle de l'image si la caméra est orientée vers l'avant
        //matrix.postScale(-1f, 1f, imageProxy.getWidth(), imageProxy.getHeight());

        // Création d'un nouveau tampon Bitmap en fonction de la rotation et de l'orientation de la caméra
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.getWidth(), bitmapBuffer.getHeight(), matrix, true);

        // Construction de l'image MPImage à partir du tampon Bitmap
        MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();

        // Détection asynchrone de la pose
        detectAsync(mpImage, frameTime);
    }


    // Méthode pour extraire les repères de pose à partir du résultat de la détection
    public static Boolean extractLandmarks(PoseLandmarkerResult results) {
        int poseIndex = 0;
        Boolean isPdetected= false;
        for (List<NormalizedLandmark> poseLandmarks : results.landmarks()) {
            Log.d("zam", "Pose Index: " + poseIndex++);
            int landmarkIndex = 0;
            for (NormalizedLandmark landmark : poseLandmarks) {
                float x = landmark.x();
                float y = landmark.y();
                if(x!=0||y!=0){
                    isPdetected=true;
                }else {
                    isPdetected=false;
                }
                //Log.d("zam", "Landmark Index: " + landmarkIndex++ + " - X: " + x + " Y: " + y);
            }
        }
        return isPdetected;
    }

    // Méthode pour détecter la pose de manière asynchrone
    @VisibleForTesting
    public void detectAsync(MPImage mpImage, long frameTime) {
        if (poseLandmarker != null) {
            poseLandmarker.detectAsync(mpImage, frameTime);
        }
    }

    // Méthode pour retourner le résultat de la détection en mode LIVE_STREAM
    private void returnLivestreamResult(PoseLandmarkerResult result, MPImage input) {
        long finishTimeMs = SystemClock.uptimeMillis();
        long inferenceTime = finishTimeMs - result.timestampMs();
        if (poseLandmarkerHelperListener != null) {
            poseLandmarkerHelperListener.onResults(new ResultBundle(Arrays.asList(result), inferenceTime, input.getHeight(), input.getWidth()));
        }
    }

    // Méthode pour retourner une erreur en mode LIVE_STREAM
    private void returnLivestreamError(RuntimeException error) {
        if (poseLandmarkerHelperListener != null) {
            poseLandmarkerHelperListener.onError(error.getMessage() != null ? error.getMessage() : "An unknown error has occurred", 1);
        }
    }

    // Interface pour écouter les événements du Pose Landmarker
    public interface LandmarkerListener {
        void onError(String error, int errorCode);
        void onResults(ResultBundle resultBundle);
    }

    // Classe pour regrouper les résultats de la détection de pose
    public static class ResultBundle {
        public List<PoseLandmarkerResult> results;
        public long inferenceTime;
        public int inputImageHeight;
        public int inputImageWidth;

        public ResultBundle(List<PoseLandmarkerResult> results, long inferenceTime, int inputImageHeight, int inputImageWidth) {
            this.results = results;
            this.inferenceTime = inferenceTime;
            this.inputImageHeight = inputImageHeight;
            this.inputImageWidth = inputImageWidth;
        }
    }
}

