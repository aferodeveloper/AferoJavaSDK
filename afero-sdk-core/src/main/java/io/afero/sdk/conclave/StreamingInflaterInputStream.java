/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.conclave;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

final class StreamingInflaterInputStream extends InflaterInputStream {

    private final InputStream wrapped;

    public StreamingInflaterInputStream(InputStream is) throws IOException {
        super(is);
        wrapped = is;
    }

    /**
     * Overrides behavior of InflaterInputStream which assumes we have all the data available
     * which is not true for streaming. We instead rely on the underlying stream to tell us
     * how much data is available.
     *
     * Programs should not count on this method to return the actual number
     * of bytes that could be read without blocking.
     *
     * @return - whatever the wrapped InputStream returns
     * @exception  IOException  if an I/O error occurs.
     */
    public int available() throws IOException {
        return wrapped.available();
    }
}
