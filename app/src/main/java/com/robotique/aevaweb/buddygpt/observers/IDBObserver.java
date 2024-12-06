package com.robotique.aevaweb.buddygpt.observers;

import java.io.IOException;

public interface IDBObserver {
    public void update(String message) throws IOException;
}
