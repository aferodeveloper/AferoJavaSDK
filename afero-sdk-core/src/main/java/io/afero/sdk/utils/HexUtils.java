/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HexUtils {
    private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

    public static byte[] parseHexBinary(String s) {
        final int len = s.length();

        // "111" is not a valid hex encoding.
        if( len%2 != 0 )
            throw new IllegalArgumentException("hexBinary needs to be even-length: "+s);

        byte[] out = new byte[len/2];

        for( int i=0; i<len; i+=2 ) {
            int h = hexToBin(s.charAt(i  ));
            int l = hexToBin(s.charAt(i+1));
            if( h==-1 || l==-1 )
                throw new IllegalArgumentException("contains illegal character for hexBinary: "+s);

            out[i/2] = (byte)(h*16+l);
        }

        return out;
    }

    private static int hexToBin(char ch ) {
        if( '0'<=ch && ch<='9' )    return ch-'0';
        if( 'A'<=ch && ch<='F' )    return ch-'A'+10;
        if( 'a'<=ch && ch<='f' )    return ch-'a'+10;
        return -1;
    }

    public static String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length*2);
        for ( byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }

    public static String printHexBinary(byte[] data, int offset, int length) {
        StringBuilder r = new StringBuilder(length*2);
        for (int i = offset; i < length; ++i) {
            byte b = data[i];
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }

    public static byte[] toBytes(ByteBuffer bb) {
        byte[] bytes;
        if (bb.hasArray()) {
            bytes = bb.array();
        } else {
            int pos = bb.position();
            bytes = new byte[bb.remaining()];
            bb.get(bytes);
            bb.position(pos);
        }
        return bytes;
    }

    public static ByteBuffer hexDecode(String hex) {
        return ByteBuffer.wrap(parseHexBinary(hex))
            .order(ByteOrder.LITTLE_ENDIAN);
    }

    public static String hexEncode(ByteBuffer bb) {
        byte[] bytes = toBytes(bb);
        return printHexBinary(bytes);
    }
}
