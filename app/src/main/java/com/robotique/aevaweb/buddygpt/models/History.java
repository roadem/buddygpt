package com.robotique.aevaweb.buddygpt.models;

import java.util.ArrayList;

public class History {
    private ArrayList<Session> history;

    public History(ArrayList<Session> history) {
        this.history = history;
    }

    public History() {
    }

    public ArrayList<Session> getHistory() {
        return history;
    }

    public void setHistory(ArrayList<Session> history) {
        this.history = history;
    }

    public void clearHistory(){
        history.clear();
    }

    @Override
    public String toString() {
        return "Hitstory{" +
                "history=" + history +
                '}';
    }
}
