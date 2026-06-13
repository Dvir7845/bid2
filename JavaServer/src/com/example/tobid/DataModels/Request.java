package com.example.tobid.DataModels;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {
	private static final long serialVersionUID = 1L;
	
    private Action action;
    private Map<String, Object> data;
    private Map<String, byte[]> files;

    public Request(Action action) {
        this.action = action;
        this.data = new HashMap<>();
        this.files = new HashMap<>();
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
 
    public void putFile(String key, byte[] bytes) {
        files.put(key, bytes);
    }
    public byte[] getFile(String key) {
        return files.get(key);
    }
    
    public Map<String, byte[]> getFiles() {
    	return files;
    }
}
