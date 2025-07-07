package com.robotique.aevaweb.buddygpt.models;

import java.util.ArrayList;

public class Session {
    private ArrayList<Replica> sessionList;

    public Session() {
    }

    public Session(ArrayList<Replica> sessionList) {
        this.sessionList = sessionList;
    }

    public ArrayList<Replica> getSession() {
        return sessionList;
    }

    public void setSession(ArrayList<Replica> sessionList) {
        this.sessionList = sessionList;
    }
    public void clearSession(){
        sessionList.clear();
    }

    @Override
    public String toString() {
        return "Session{" +
                "session=" + sessionList +
                '}';
    }
}
