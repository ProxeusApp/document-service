package com.proxeus.error;

public class UnavailableException extends Exception {
    public UnavailableException(String msg) {
        super(msg);
    }
    public UnavailableException(String msg, Throwable t){
        super(msg, t);
    }
}
