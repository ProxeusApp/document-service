package com.proxeus.xml;

public class NoCopyCharSequence implements CharSequence {
    public StringBuffer buffer;
    public int start;
    public int end;
    protected NoCopyCharSequence(StringBuffer b, int start, int end){
        this.buffer = b;
        this.start = start;
        this.end = end;
    }
    public int length() {
        return end - start;
    }

    public char charAt(int index) {
        return this.buffer.charAt(start+index);
    }

    public CharSequence subSequence(int start, int end) {
        return this.buffer.subSequence(this.start+start, this.start+end);
    }

    public String toString(){
        return this.buffer.substring(start, end);
    }
}