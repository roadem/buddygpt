package com.robotique.aevaweb.buddygpt.utilis;

public interface GoogleSTTCallbacks {

    void onRequestSent();

    void onResponse(String text);

    void onResponseError(String error);

}
