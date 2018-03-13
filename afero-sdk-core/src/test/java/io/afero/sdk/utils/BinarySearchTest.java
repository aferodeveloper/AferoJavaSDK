/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;


public class BinarySearchTest {
    @Test
    public void lowerBound() throws Exception {
        startTest()
                .verifyLowerBoundFindsEachKey()
                .verifyLowerBoundNotFound();
    }

    @Test
    public void upperBound() throws Exception {
        startTest()
                .verifyUpperBoundFindsEachKey()
                .verifyUpperBoundNotFound();
    }

    private BinarySearchTester startTest() {
        return new BinarySearchTester();
    }

    private static class BinarySearchTester {
        final List<String> list = Arrays.asList("aaa", "ccc", "eee", "fff", "fff", "fff", "ggg", "ggg", "kkk", "rrr", "rrr", "rrr", "xxx", "zzz");
        final Integer[] lowerIndices;
        final Integer[] upperIndices;

        BinarySearchTester() {
            Collections.sort(list);

            lowerIndices = getFirstIndexOfEachValue(list);
            upperIndices = getLastIndexOfEachValue(list);
        }

        private Integer[] getFirstIndexOfEachValue(List<String> values) {
            String lastKey = "";
            ArrayList<Integer> indices = new ArrayList<>(values.size());
            for (String key : values) {
                if (!key.equals(lastKey)) {
                    lastKey = key;
                    indices.add(values.indexOf(key));
                }
            }

            return indices.toArray(new Integer[indices.size()]);
        }

        private Integer[] getLastIndexOfEachValue(List<String> values) {
            String lastKey = "";
            ArrayList<Integer> indices = new ArrayList<>(values.size());
            for (String key : values) {
                if (!key.equals(lastKey)) {
                    lastKey = key;
                    indices.add(values.lastIndexOf(key));
                }
            }

            return indices.toArray(new Integer[indices.size()]);
        }

        /**
         * Searches for each key in the list and verifies that the correct result
         * is returned from {@link BinarySearch#lowerBound(List, Object)}.
         */
        BinarySearchTester verifyLowerBoundFindsEachKey() {

            String lastKey = "";
            Iterator<Integer> expectedResultIter = Arrays.asList(lowerIndices).iterator();

            Integer expectedSearchResult = null;

            for (String key : list) {
                int actualSearchResult = BinarySearch.lowerBound(list, key);

                if (!key.equals(lastKey)) {
                    lastKey = key;
                    expectedSearchResult = expectedResultIter.next();
                }

                assertEquals(expectedSearchResult, Integer.valueOf(actualSearchResult));
            }

            return this;
        }

        /**
         * Searches for each key in the list and verifies that the correct result
         * is returned from {@link BinarySearch#upperBound(List, Object)}.
         */
        BinarySearchTester verifyUpperBoundFindsEachKey() {

            String lastKey = "";
            Iterator<Integer> expectedResultIter = Arrays.asList(upperIndices).iterator();

            Integer expectedSearchResult = null;

            for (String key : list) {
                int actualSearchResult = BinarySearch.upperBound(list, key);

                if (!key.equals(lastKey)) {
                    lastKey = key;
                    expectedSearchResult = expectedResultIter.next();
                }

                assertEquals(expectedSearchResult, Integer.valueOf(actualSearchResult));
            }

            return this;
        }

        BinarySearchTester verifyLowerBoundNotFound() {
            int actualSearchResult = BinarySearch.lowerBound(list, "bogus");
            assertEquals(-1, actualSearchResult);
            return this;
        }

        BinarySearchTester verifyUpperBoundNotFound() {
            int actualSearchResult = BinarySearch.upperBound(list, "bogus");
            assertEquals(-1, actualSearchResult);
            return this;
        }
    }

}