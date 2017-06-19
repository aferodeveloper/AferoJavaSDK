/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

public class CompressedOutputStream extends FilterOutputStream {
    /**
     * Buffer for input data
     */
    private byte[] inBuf = null;

    /**
     * Buffer for compressed data to be written
     */
    private byte[] outBuf = null;

    /**
     * Number of bytes in the buffer
     */
    private int len = 0;

    /**
     * Deflater for compressing data
     */
    private Deflater deflater = null;

    /**
     * Constructs a CompressedBlockOutputStream that writes to
     * the given underlying output stream 'os' and sends a compressed
     * block once 'size' byte have been written. The default
     * compression strategy and level are used.
     */
    public CompressedOutputStream(OutputStream os, int size)
            throws IOException {
        this(os, size,
                Deflater.BEST_COMPRESSION, Deflater.DEFAULT_STRATEGY);
    }

    /**
     * Constructs a CompressedBlockOutputStream that writes to the
     * given underlying output stream 'os' and sends a compressed
     * block once 'size' byte have been written. The compression
     * level and strategy should be specified using the constants
     * defined in java.util.zip.Deflator.
     */
    public CompressedOutputStream(OutputStream os, int size,
                                       int level, int strategy) throws IOException {
        super(os);
        this.inBuf = new byte[size];
        this.outBuf = new byte[size + 64];
        this.deflater = new Deflater(level);
        this.deflater.setStrategy(strategy);
    }

    protected void compressAndSend() throws IOException {
        if (len > 0) {
            deflater.setInput(inBuf, 0, len);
            deflater.finish();
            int size = deflater.deflate(outBuf);

            out.write(outBuf, 0, size);
            out.flush();

            len = 0;
            deflater.reset();
        }
    }

    public void write(int b) throws IOException {
        inBuf[len++] = (byte) b;
        if (len == inBuf.length) {
            compressAndSend();
        }
    }

    public void write(byte[] b, int boff, int blen)
            throws IOException {
        while ((len + blen) > inBuf.length) {
            int toCopy = inBuf.length - len;
            System.arraycopy(b, boff, inBuf, len, toCopy);
            len += toCopy;
            compressAndSend();
            boff += toCopy;
            blen -= toCopy;
        }
        System.arraycopy(b, boff, inBuf, len, blen);
        len += blen;
    }

    public void flush() throws IOException {
        compressAndSend();
        out.flush();
    }

    public void close() throws IOException {
        compressAndSend();
        out.close();
    }
}