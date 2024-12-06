package com.robotique.aevaweb.buddygpt.models;

import java.util.ArrayList;

public class Session {
    private ArrayList<Replica> session;

    public Session() {
    }

    public Session(ArrayList<Replica> session) {
        this.session = session;
    }

    public ArrayList<Replica> getSession() {
        return session;
    }

    public void setSession(ArrayList<Replica> session) {
        this.session = session;
    }
    public void clearSession(){
        session.clear();
    }

    @Override
    public String toString() {
        return "Session{" +
                "session=" + session +
                '}';
    }
}
