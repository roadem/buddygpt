package com.robotique.aevaweb.buddygpt.models;

public class ApiResponse {
    private String sessionId;
    private String emotion;
    private String answer;
    private boolean isFinished;
    private String commandes;

    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean isFinished) {
        this.isFinished = isFinished;
    }

    public String getCommandes() {
        return commandes;
    }

    public void setCommandes(String commandes) {
        this.commandes = commandes;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "session_id='" + sessionId + '\'' +
                ", Emotion='" + emotion + '\'' +
                ", Answer='" + answer + '\'' +
                ", is_finished=" + isFinished +
                ", Commandes='" + commandes + '\'' +
                '}';
    }
}

