package com.example.tobid.ServerCommunicationClasses;

import com.example.tobid.DataModels.Response;

/**
 * Interface for handling server response callbacks.
 */
public interface ServerCallback {
    void onResponseReceived(Response response);
}
