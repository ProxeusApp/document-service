/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.proxeus.xml.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * StreamDecoder is a copy of java source from https://github.com/frohoff/jdk8u-jdk/blob/master/src/share/classes/sun/nio/cs/StreamDecoder.java.
 * The reason for copying was the private access of the constructors.
 *
 * I removed all the synchronized blocks as it will be always touched by one thread in our case and
 * I made modifications to extend the reader with this as soon as we have read the charset.
 */
public class StreamDecoder {
    private volatile boolean isOpen = true;

    private void ensureOpen() throws IOException {
        if (!isOpen)
            throw new IOException("Stream closed");
    }

    // In order to handle surrogates properly we must never try to produce
    // fewer than two characters at a time.  If we're only asked to return one
    // character then the other is saved here to be returned later.
    //
    private boolean haveLeftoverChar = false;
    private char leftoverChar;

    public int read() throws IOException {
        // Return the leftover char, if there is one
        if (haveLeftoverChar) {
            haveLeftoverChar = false;
            return leftoverChar;
        }

        // Convert more bytes
        char cb[] = new char[2];
        int n = read(cb, 0, 2);
        switch (n) {
            case -1:
                return -1;
            case 2:
                leftoverChar = cb[1];
                haveLeftoverChar = true;
                // FALL THROUGH
            case 1:
                return cb[0];
            default:
                assert false : n;
                return -1;
        }
    }

    public int read(char cbuf[], int offset, int length) throws IOException {
        int off = offset;
        int len = length;
        ensureOpen();
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0)
            return 0;

        int n = 0;

        if (haveLeftoverChar) {
            // Copy the leftover char into the buffer
            cbuf[off] = leftoverChar;
            off++;
            len--;
            haveLeftoverChar = false;
            n = 1;
            if ((len == 0) || !implReady())
                // Return now if this is all we can produce w/o blocking
                return n;
        }

        if (len == 1) {
            // Treat single-character array reads just like read()
            int c = read();
            if (c == -1)
                return (n == 0) ? -1 : n;
            cbuf[off] = (char) c;
            return n + 1;
        }

        return n + implRead(cbuf, off, off + len);
    }

    public boolean ready() throws IOException {
        ensureOpen();
        return haveLeftoverChar || implReady();
    }

    public void close() throws IOException {
        if (!isOpen)
            return;
        implClose();
        isOpen = false;
    }

    private boolean isOpen() {
        return isOpen;
    }


    // -- Charset-based stream decoder impl --

    // In the early stages of the build we haven't yet built the NIO native
    // code, so guard against that by catching the first UnsatisfiedLinkError
    // and setting this flag so that later attempts fail quickly.
    //
    private static volatile boolean channelsAvailable = true;

    private static FileChannel getChannel(FileInputStream in) {
        if (!channelsAvailable)
            return null;
        try {
            return in.getChannel();
        } catch (UnsatisfiedLinkError x) {
            channelsAvailable = false;
            return null;
        }
    }

    private Charset cs;
    private CharsetDecoder decoder;
    private ByteBuffer bb;

    // Exactly one of these is non-null
    private InputStream in;
    private ReadableByteChannel ch;

    public StreamDecoder(InputStream in, Charset cs, int maxBufferSize) {
        this(in,
                cs.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE), maxBufferSize);
    }

    StreamDecoder(InputStream in, CharsetDecoder dec, int maxBufferSize) {
        this.cs = dec.charset();
        this.decoder = dec;

        // This path disabled until direct buffers are faster
        if (false && in instanceof FileInputStream) {
            ch = getChannel((FileInputStream) in);
            if (ch != null)
                bb = ByteBuffer.allocateDirect(maxBufferSize);
        }
        if (ch == null) {
            this.in = in;
            this.ch = null;
            bb = ByteBuffer.allocate(maxBufferSize);
        }
        bb.flip();                      // So that bb is initially empty
    }

    private int readBytes() throws IOException {
        bb.compact();
        try {
            if (ch != null) {
                // Read from the channel
                int n = ch.read(bb);
                if (n < 0)
                    return n;
            } else {
                // Read from the input stream, and then update the buffer
                int lim = bb.limit();
                int pos = bb.position();
                assert (pos <= lim);
                int rem = (pos <= lim ? lim - pos : 0);
                assert rem > 0;
                int n = in.read(bb.array(), bb.arrayOffset() + pos, rem);
                if (n < 0)
                    return n;
                if (n == 0)
                    throw new IOException("Underlying input stream returned zero bytes");
                assert (n <= rem) : "n = " + n + ", rem = " + rem;
                bb.position(pos + n);
            }
        } finally {
            // Flip even when an IOException is thrown,
            // otherwise the stream will stutter
            bb.flip();
        }

        int rem = bb.remaining();
        assert (rem != 0) : rem;
        return rem;
    }

    int implRead(char[] cbuf, int off, int end) throws IOException {

        // In order to handle surrogate pairs, this method requires that
        // the invoker attempt to read at least two characters.  Saving the
        // extra character, if any, at a higher level is easier than trying
        // to deal with it here.
        assert (end - off > 1);

        CharBuffer cb = CharBuffer.wrap(cbuf, off, end - off);
        if (cb.position() != 0)
            // Ensure that cb[0] == cbuf[off]
            cb = cb.slice();

        boolean eof = false;
        while(true) {
            CoderResult cr = decoder.decode(bb, cb, eof);
            if (cr.isUnderflow()) {
                if (eof)
                    break;
                if (!cb.hasRemaining())
                    break;
                if ((cb.position() > 0) && !inReady())
                    break;          // Block at most once
                int n = readBytes();
                if (n < 0) {
                    eof = true;
                    if ((cb.position() == 0) && (!bb.hasRemaining()))
                        break;
                    decoder.reset();
                }
                continue;
            }
            if (cr.isOverflow()) {
                assert cb.position() > 0;
                break;
            }
            cr.throwException();
        }

        if (eof) {
            // ## Need to flush decoder
            decoder.reset();
        }

        if (cb.position() == 0) {
            if (eof)
                return -1;
            assert false;
        }
        return cb.position();
    }

    private boolean inReady() {
        try {
            return (((in != null) && (in.available() > 0))
                    || (ch instanceof FileChannel)); // ## RBC.available()?
        } catch (IOException x) {
            return false;
        }
    }

    boolean implReady() {
        return bb.hasRemaining() || inReady();
    }

    void implClose() throws IOException {
        if (ch != null)
            ch.close();
        else
            in.close();
    }

}