package com.robotique.aevaweb.buddygpt.models;
import com.google.gson.annotations.SerializedName;
public class Request {

    @SerializedName("Text_input")
    private String textInput;

    @SerializedName("IMEI_ID_Device")
    private String imeiIdDevice;

    @SerializedName("EMOTION")
    private boolean emotion;

    @SerializedName("Commandes")
    private boolean commandes;

    @SerializedName("LANGUE")
    private String langue;

    @SerializedName("session_id")
    private String sessionId;

    // Getters and setters...

    public String getTextInput() {
        return textInput;
    }

    public void setTextInput(String textInput) {
        this.textInput = textInput;
    }

    public String getImeiIdDevice() {
        return imeiIdDevice;
    }

    public void setImeiIdDevice(String imeiIdDevice) {
        this.imeiIdDevice = imeiIdDevice;
    }

    public boolean isEmotion() {
        return emotion;
    }

    public void setEmotion(boolean emotion) {
        this.emotion = emotion;
    }

    public boolean isCommandes() {
        return commandes;
    }

    public void setCommandes(boolean commandes) {
        this.commandes = commandes;
    }

    public String getLangue() {
        return langue;
    }

    public void setLangue(String langue) {
        this.langue = langue;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
