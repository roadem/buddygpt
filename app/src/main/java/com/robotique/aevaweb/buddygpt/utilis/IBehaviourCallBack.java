package com.robotique.aevaweb.buddygpt.utilis;

public interface IBehaviourCallBack {
    void onEnd(boolean hasAborted, String reason);
    void onRun(String s);
}
