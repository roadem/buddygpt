package com.robotique.aevaweb.buddygpt.utilis.tracking;

import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {

    private int _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL;
    private int _delegate = PoseLandmarkerHelper.DELEGATE_GPU;
    private float _minPoseDetectionConfidence = PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE;
    private float _minPoseTrackingConfidence = PoseLandmarkerHelper.DEFAULT_POSE_TRACKING_CONFIDENCE;
    private float _minPosePresenceConfidence = PoseLandmarkerHelper.DEFAULT_POSE_PRESENCE_CONFIDENCE;

    public int getCurrentDelegate() {
        return _delegate;
    }

    public int getCurrentModel() {
        return _model;
    }

    public float getMinPoseDetectionConfidence() {
        return _minPoseDetectionConfidence;
    }

    public float getCurrentMinPoseTrackingConfidence() {
        return _minPoseTrackingConfidence;
    }

    public float getCurrentMinPosePresenceConfidence() {
        return _minPosePresenceConfidence;
    }

    public void setDelegate(int delegate) {
        _delegate = delegate;
    }

    public void setMinPoseDetectionConfidence(float confidence) {
        _minPoseDetectionConfidence = confidence;
    }

    public void setMinPoseTrackingConfidence(float confidence) {
        _minPoseTrackingConfidence = confidence;
    }

    public void setMinPosePresenceConfidence(float confidence) {
        _minPosePresenceConfidence = confidence;
    }

    public void set_model(int _model) {
        this._model = _model;
    }

    public void set_delegate(int _delegate) {
        this._delegate = _delegate;
    }

    public void set_minPoseDetectionConfidence(float _minPoseDetectionConfidence) {
        this._minPoseDetectionConfidence = _minPoseDetectionConfidence;
    }

    public void set_minPoseTrackingConfidence(float _minPoseTrackingConfidence) {
        this._minPoseTrackingConfidence = _minPoseTrackingConfidence;
    }

    public void set_minPosePresenceConfidence(float _minPosePresenceConfidence) {
        this._minPosePresenceConfidence = _minPosePresenceConfidence;
    }
    private float currentMinPoseDetectionConfidence;

    // ...


    public void setCurrentMinPoseDetectionConfidence(float value) {
        if (value >= 0 && value <= 0.8) {
            this.currentMinPoseDetectionConfidence = value;
        } else {
            // Gérer les valeurs en dehors de la plage autorisée
            throw new IllegalArgumentException("La valeur doit être comprise entre 0 et 0.8");
        }
    }
    public void setModel(int model) {
        _model = model;
    }
}
