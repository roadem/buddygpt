package com.robotique.aevaweb.buddygpt.models;

public class ApiResponse {
    private String session_id;
    private String Emotion;
    private String Answer;
    private boolean is_finished;
    private String Commandes;

    // Getters and setters
    public String getSessionId() {
        return session_id;
    }

    public void setSessionId(String session_id) {
        this.session_id = session_id;
    }

    public String getEmotion() {
        return Emotion;
    }

    public void setEmotion(String Emotion) {
        this.Emotion = Emotion;
    }

    public String getAnswer() {
        return Answer;
    }

    public void setAnswer(String Answer) {
        this.Answer = Answer;
    }

    public boolean isFinished() {
        return is_finished;
    }

    public void setFinished(boolean is_finished) {
        this.is_finished = is_finished;
    }

    public String getCommandes() {
        return Commandes;
    }

    public void setCommandes(String Commandes) {
        this.Commandes = Commandes;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "session_id='" + session_id + '\'' +
                ", Emotion='" + Emotion + '\'' +
                ", Answer='" + Answer + '\'' +
                ", is_finished=" + is_finished +
                ", Commandes='" + Commandes + '\'' +
                '}';
    }
}

