package com.robotique.aevaweb.buddygpt.models;

import java.util.Objects;

public class Setting {
    private String duration;
    private String attempt;

    private String header;
    private String apiKey;

    private String chatbot;
    private String langue;
    private String vitesse;
    private String volume;
    private String switchVisibility;
    private String switchEmotion;
    private String switchBIDisplay;
    private String switchLanguageDetection;
    private String switchModeStream;
    private String switchCommande;

    private String chatbotUrl;
    private String chatbotApi;
    private String chatbotToken;
    private String projectID;
    public Setting() {
        // Method left empty intentionally because no specific action is needed for this update.
    }

    public String getSwitchEmotion() {
        return switchEmotion;
    }

    public void setSwitchEmotion(String switchEmotion) {
        this.switchEmotion = switchEmotion;
    }

    public String getSwitchLanguageDetection() {
        return switchLanguageDetection;
    }

    public void setSwitchLanguageDetection(String switchLanguageDetection) {
        this.switchLanguageDetection = switchLanguageDetection;
    }

    public String getSwitchModeStream() {
        return switchModeStream;
    }

    public void setSwitchModeStream(String switchModeStream) {
        this.switchModeStream = switchModeStream;
    }
    public String getSwitchCommande() {
        return switchCommande;
    }

    public void setSwitchCommande(String switchCommande) {
        this.switchCommande = switchCommande;
    }
    public String getChatbotToken() {
        return chatbotToken;
    }

    public void setChatbotToken(String chatbotToken) {
        this.chatbotToken = chatbotToken;
    }

    public String getDuration() {
        return duration;
    }

    public String getChatbotUrl() {
        return chatbotUrl;
    }

    public void setChatbotUrl(String chatbotUrl) {
        this.chatbotUrl = chatbotUrl;
    }

    public String getChatbotApi() {
        return chatbotApi;
    }

    public void setChatbotApi(String chatbotApi) {
        this.chatbotApi = chatbotApi;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getAttempt() {
        return attempt;
    }

    public void setAttempt(String attempt) {
        this.attempt = attempt;
    }

    public String getChatbot() {
        return chatbot;
    }

    public void setChatbot(String chatbot) {
        this.chatbot = chatbot;
    }




    public String getLangue() {
        return langue;
    }

    public void setLangue(String langue) {
        this.langue = langue;
    }

    public String getVitesse() {
        return vitesse;
    }

    public void setVitesse(String vitesse) {
        this.vitesse = vitesse;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getSwitchVisibility() {
        return switchVisibility;
    }

    public void setSwitchVisibility(String switchVisibility) {
        this.switchVisibility = switchVisibility;
    }

    public String getSwitchBIDisplay() {
        return switchBIDisplay;
    }

    public void setSwitchBIDisplay(String switchBIDisplay) {
        this.switchBIDisplay = switchBIDisplay;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Setting setting = (Setting) obj;
        return this.duration == setting.getDuration()
                && this.attempt == setting.getAttempt()
                && this.langue.equals(setting.getLangue())
                && this.vitesse == setting.getVitesse()
                && this.volume == setting.getVolume()
                && this.switchVisibility == setting.getSwitchVisibility()
                && this.switchEmotion == setting.getSwitchEmotion()
                && this.header == setting.getHeader()
                && this.apiKey == setting.getApiKey()
                && this.switchLanguageDetection == setting.getSwitchLanguageDetection()
                && this.switchModeStream == setting.getSwitchModeStream()
                && this.switchBIDisplay == setting.getSwitchBIDisplay()
                && this.switchCommande == setting.getSwitchCommande()
                && this.projectID == setting.getProjectID();
    }
    @Override
    public int hashCode() {
        return Objects.hash(
                duration, attempt, chatbot, langue, vitesse, volume, switchVisibility, switchEmotion,switchLanguageDetection,switchModeStream,switchCommande
        );
    }

    @Override
    public String toString() {
        return "Setting{" +
                "duration='" + duration + '\'' +
                ", chatbot='" + chatbot + '\'' +
                ", langue='" + langue + '\'' +
                ", vitesse='" + vitesse + '\'' +
                ", volume='" + volume + '\'' +
                '}';
    }
}
