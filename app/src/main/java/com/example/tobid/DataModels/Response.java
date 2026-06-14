package com.example.tobid.DataModels;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Response implements Serializable {
    private boolean success;
    private String message;
    private Map<String, Object> data;



    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = new HashMap<>();
    }



    //////geter and seter
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public void putData(String key, Object value) {
        data.put(key, value);
    }
    public Object getData(String key) {
        return data.get(key);
    }
}
