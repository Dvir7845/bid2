package com.example.tobid.DataModels;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {
    private Action action;
    private Map<String, Object> data;

    public Request(Action action) {
        this.action = action;
        this.data = new HashMap<>();
    }

    public Action getAction() {
        return action;
    }
    public void putData(String key, Object value) {
        data.put(key, value);
    }
    public Object getData(String key) {
        return data.get(key);
    }
}



