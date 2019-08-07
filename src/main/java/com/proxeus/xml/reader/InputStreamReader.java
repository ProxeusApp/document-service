package com.proxeus.xml.reader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * XmlStreamReader makes it possible to read with the plain inputStream without a charset.
 * After the charset is read from the file it can be set to this reader to extend readability.
 */
public class InputStreamReader {
    private StreamDecoder sd;
    private boolean correctCharsetHasBeenSet = false;
    private Charset charset;
    private InputStream in;
    private int bufferSize;

    public InputStreamReader(InputStream in) {
        this.in = in;
        this.bufferSize = 1024;
    }
    public InputStreamReader(InputStream in, int bufferSize) {
        this.in = in;
        this.bufferSize = bufferSize;
    }

    public InputStreamReader(InputStream in, Charset charset, int bufferSize) {
        this.in = in;
        this.charset = charset;
        this.correctCharsetHasBeenSet = true;
        this.bufferSize = bufferSize;
        sd = new StreamDecoder(in, charset, bufferSize);
    }

    public Charset getCharset() {
        return charset;
    }

    /**
     * Set the charset after it was read from the content header.
     * This method can be called only once.
     */
    public void initCharset(Charset chrset) {
        if (correctCharsetHasBeenSet) {
            return;
        }
        correctCharsetHasBeenSet = true;
        charset = chrset;
        sd = new StreamDecoder(in, charset, bufferSize);
    }

    public int read() throws IOException {
        if (correctCharsetHasBeenSet) {
            return sd.read();
        }
        return in.read();
    }

    public int read(char cbuf[]) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    public int read(char cbuf[], int offset, int length) throws IOException {
        if (correctCharsetHasBeenSet) {
            return sd.read(cbuf, offset, length);
        }
        //just read one byte as long as we don't have the correct charset
        length = read();
        if(length == -1){
            return -1;
        }
        cbuf[0] = (char) length;
        return 1;
    }

    public boolean ready() throws IOException {
        return sd.ready();
    }

    public void close() throws IOException {
        sd.close();
    }
}
