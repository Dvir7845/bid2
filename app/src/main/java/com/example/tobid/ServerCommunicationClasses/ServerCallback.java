package com.example.tobid.ServerCommunicationClasses;

import com.example.tobid.DataModels.Response;

public interface ServerCallback {
    void onResponseReceived(Response response);
}
