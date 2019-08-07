package com.proxeus.error;

public class CompilationException extends Exception {
    public CompilationException(String msg) {
        super(msg);
    }
    public CompilationException(String msg, Throwable t){
        super(msg, t);
    }
}
