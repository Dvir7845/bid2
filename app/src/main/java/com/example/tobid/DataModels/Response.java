package com.example.tobid.DataModels;

import java.io.Serializable;

public class Response implements Serializable {
    private boolean success;
    private String message;
    private Object data;
}
