package com.proxeus.error;

public class NotImplementedException extends Exception {
    public NotImplementedException(String msg) {
        super(msg);
    }
    public NotImplementedException(String msg, Throwable t){
        super(msg, t);
    }
}
