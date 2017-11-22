/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;


import java.util.List;

public class BinarySearch {

    public static <T> int lowerBound(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        int found = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<? super T> midVal = list.get(mid);
            int d = midVal.compareTo(key);

            if (d < 0) {
                low = mid + 1;
            } else if (d > 0) {
                high = mid - 1;
            } else {
                found = mid;
                high = mid - 1;
            }
        }

        return found;
    }

    public static <T> int upperBound(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        int found = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<? super T> midVal = list.get(mid);
            int d = midVal.compareTo(key);

            if (d < 0) {
                low = mid + 1;
            } else if (d > 0) {
                high = mid - 1;
            } else {
                found = mid;
                low = mid + 1;
            }
        }

        return found;
    }
}
