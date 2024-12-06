package com.robotique.aevaweb.buddygpt.models;

import androidx.annotation.Nullable;

public class Langue {
    private long id;
    private String nom;
    private boolean isChosen;
    private String languageCode;

    public Langue(long id, String nom, boolean isChosen) {
        this.id = id;
        this.nom = nom;
        this.isChosen = isChosen;
    }
    public Langue(long id, String nom, boolean isChosen,String languageCode){
        this.id = id;
        this.nom = nom;
        this.isChosen = isChosen;
        this.languageCode = languageCode;
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
    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Langue)) return false;
        return ((Langue) obj).id == this.id;
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
