package com.proxeus.error;

public class BadRequestException extends Exception {
    public BadRequestException(String msg) {
        super(msg);
    }
    public BadRequestException(String msg, Throwable t){
        super(msg, t);
    }
}
