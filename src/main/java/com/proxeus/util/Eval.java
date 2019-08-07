package com.proxeus.util;

public class Eval {
    public static Object me(String code, Object data){
        try{
            return groovy.util.Eval.me("x", data,  "x."+code.trim());
        }catch (Exception e){
            //do not throw an exception, just return null in any error case
            return null;
        }
    }
}