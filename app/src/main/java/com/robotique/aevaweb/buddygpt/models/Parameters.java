package com.robotique.aevaweb.buddygpt.models;

import com.google.gson.annotations.SerializedName;

public class Parameters {
    @SerializedName("Mail_sender")
    private String mailSender;

    @SerializedName("Smtp_host")
    private String smtpHost;

    @SerializedName("Stream_mode")
    private String streamMode;

    @SerializedName("TeamGPT_Key")
    private String teamGptKey;

    @SerializedName("Header")
    private String header;

    @SerializedName("IMEI_ID_Device")
    private String imeiDevice;

    @SerializedName("IdCompte")
    private String idCompte;

    @SerializedName("Password_mail_sender")
    private String passwordMailSender;

    @SerializedName("Smtp_port")
    private String smtpPort;

    @SerializedName("show_price")
    private String showPrice;

    @SerializedName("Entete")
    private String entete;

    @SerializedName("NomCompte")
    private String nomCompte;

    @SerializedName("Email")
    private String email;

    @SerializedName("CustomGPT_model")
    private String customGptModel;

    @SerializedName("Modele_Mistral")
    private String modeleMistral;

    @SerializedName("selected_chatbot")
    private String selectedChatbot;

    @SerializedName("Modele_Openai")
    private String modeleOpenai;

    @SerializedName("STT")
    private String stt;

    @SerializedName("TTS")
    private String tts;

    @SerializedName("Modele_gemini")
    private String modeleGemini;

    @SerializedName("email_support")
    private String emailSupport;

    public String getMailSender() {
        return mailSender;
    }

    public void setMailSender(String mailSender) {
        this.mailSender = mailSender;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public String getStreamMode() {
        return streamMode;
    }

    public void setStreamMode(String streamMode) {
        this.streamMode = streamMode;
    }

    public String getImeiDevice() {
        return imeiDevice;
    }

    public void setImeiDevice(String imeiDevice) {
        this.imeiDevice = imeiDevice;
    }

    public String getIdCompte() {
        return idCompte;
    }

    public void setIdCompte(String idCompte) {
        this.idCompte = idCompte;
    }


    public String getTeamGptKey() {
        return teamGptKey;
    }

    public void setTeamGptKey(String teamGptKey) {
        this.teamGptKey = teamGptKey;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPasswordMailSender() {
        return passwordMailSender;
    }

    public void setPasswordMailSender(String passwordMailSender) {
        this.passwordMailSender = passwordMailSender;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getShowPrice() {
        return showPrice;
    }

    public void setShowPrice(String showPrice) {
        this.showPrice = showPrice;
    }

    public String getEntete() {
        return entete;
    }

    public void setEntete(String entete) {
        this.entete = entete;
    }

    public String getNomCompte() {
        return nomCompte;
    }

    public void setNomCompte(String nomCompte) {
        this.nomCompte = nomCompte;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCustomGptModel() {
        return customGptModel;
    }

    public void setCustomGptModel(String customGptModel) {
        this.customGptModel = customGptModel;
    }

    public String getModeleMistral() {
        return modeleMistral;
    }

    public void setModeleMistral(String modeleMistral) {
        this.modeleMistral = modeleMistral;
    }

    public String getSelectedChatbot() {
        return selectedChatbot;
    }

    public void setSelectedChatbot(String selectedChatbot) {
        this.selectedChatbot = selectedChatbot;
    }

    public String getModeleOpenai() {
        return modeleOpenai;
    }

    public void setModeleOpenai(String modeleOpenai) {
        this.modeleOpenai = modeleOpenai;
    }

    public String getStt() {
        return stt;
    }

    public void setStt(String stt) {
        this.stt = stt;
    }

    public String getTts() {
        return tts;
    }

    public void setTts(String tts) {
        this.tts = tts;
    }

    public String getModeleGemini() {
        return modeleGemini;
    }
    public String getEmailSupport() {
        return emailSupport;
    }

    public void setEmailSupport(String emailSupport) {
        this.emailSupport = emailSupport;
    }
    public void setModeleGemini(String modeleGemini) {
        this.modeleGemini = modeleGemini;
    }
}
