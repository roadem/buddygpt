package com.robotique.aevaweb.buddygpt.models;

import com.google.gson.annotations.SerializedName;

public class ParametersResponse {
    @SerializedName("parameters")
    private Parameters parameters;

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}

