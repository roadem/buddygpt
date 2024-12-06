package com.robotique.aevaweb.buddygpt.models;

public class SttModel {
    private long id;
    private String nom;
    private boolean isChosen;

    public SttModel(long id, String nom, boolean isChosen) {
        this.id = id;
        this.nom = nom;
        this.isChosen = isChosen;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public boolean isChosen() {
        return isChosen;
    }

    public void setChosen(boolean chosen) {
        isChosen = chosen;
    }


    @Override
    public String toString() {
        return "Langue{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", isChosen=" + isChosen +
                '}';
    }
}


