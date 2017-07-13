/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class CompressedInputStream extends FilterInputStream {
    /**
     * Buffer of compressed data read from the stream
     */
    private byte[] inBuf = null;

    /**
     * Length of data in the input data
     */
    private int inLength = 0;

    /**
     * Buffer of uncompressed data
     */
    private byte[] outBuf = null;

    /**
     * Offset and length of uncompressed data
     */
    private int outOffs = 0;
    private int outLength = 0;

    /**
     * Inflater for decompressing
     */
    private Inflater inflater = null;

    public CompressedInputStream(InputStream is) throws IOException {
        super(is);
        inflater = new Inflater();
    }

    private void readAndDecompress() throws IOException {
        inLength = in.available();
        outLength = inLength;

        // Make sure we've got enough space to read the block
        if ((inBuf == null) || (inLength > inBuf.length)) {
            inBuf = new byte[inLength];
        }

        if ((outBuf == null) || (outLength > outBuf.length)) {
            outBuf = new byte[outLength];
        }

        // Read until we're got the entire compressed buffer.
        // read(...) will not necessarily block until all
        // requested data has been read, so we loop until
        // we're done.
        int inOffs = 0;
        while (inOffs < inLength) {
            int inCount = in.read(inBuf, inOffs, inLength - inOffs);
            if (inCount == -1) {
                throw new EOFException();
            }
            inOffs += inCount;
        }

        inflater.setInput(inBuf, 0, inLength);
        try {
            inflater.inflate(outBuf);
        } catch (DataFormatException dfe) {
            throw new IOException(
                    "Data format exception - " +
                            dfe.getMessage());
        }

        // Reset the inflator so we can re-use it for the
        // next block
        inflater.reset();

        outOffs = 0;
    }

    public int read() throws IOException {
        if (outOffs >= outLength) {
            try {
                readAndDecompress();
            } catch (EOFException eof) {
                return -1;
            }
        }

        return outBuf[outOffs++] & 0xff;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int count = 0;
        while (count < len) {
            if (outOffs >= outLength) {
                try {
                    // If we've read at least one decompressed
                    // byte and further decompression would
                    // require blocking, return the count.
                    if ((count > 0) && (in.available() == 0))
                        return count;
                    else
                        readAndDecompress();
                } catch (EOFException eof) {
                    if (count == 0)
                        count = -1;
                    return count;
                }
            }

            int toCopy = Math.min(outLength - outOffs, len - count);
            System.arraycopy(outBuf, outOffs, b, off + count, toCopy);
            outOffs += toCopy;
            count += toCopy;
        }

        return count;
    }

    public int available() throws IOException {
        // This isn't precise, but should be an adequate
        // lower bound on the actual amount of available data
        return (outLength - outOffs) + in.available();
    }

}