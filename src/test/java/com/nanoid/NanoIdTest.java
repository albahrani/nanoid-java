package com.nanoid;

import org.junit.Assert;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

public class NanoIdTest {
    @Test
    public void testGeneratesUrlFriendlyIds() {
        for (int i = 0; i < 100; i++) {
            String id = NanoId.nanoid();
            Assert.assertEquals(21, id.length());
            for (char c : id.toCharArray()) {
                Assert.assertTrue(NanoId.URL_ALPHABET.indexOf(c) >= 0);
            }
        }
    }

    @Test
    public void testChangesIdLength() {
        Assert.assertEquals(10, NanoId.nanoid(10).length());
    }

    @Test
    public void testNoCollisions() {
        Set<String> used = new HashSet<>();
        for (int i = 0; i < 50000; i++) {
            String id = NanoId.nanoid();
            Assert.assertFalse(used.contains(id));
            used.add(id);
        }
    }

    @Test
    public void testAlphabetHasNoDuplicates() {
        String alphabet = NanoId.URL_ALPHABET;
        for (int i = 0; i < alphabet.length(); i++) {
            Assert.assertEquals(i, alphabet.lastIndexOf(alphabet.charAt(i)));
        }
    }

    @Test
    public void testCustomAlphabetFlatDistribution() {
        int COUNT = 50000;
        int LENGTH = 30;
        String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
        int[] chars = new int[ALPHABET.length()];
        for (int i = 0; i < COUNT; i++) {
            String id = NanoId.customNanoid(ALPHABET, LENGTH);
            for (char c : id.toCharArray()) {
                int idx = ALPHABET.indexOf(c);
                chars[idx]++;
            }
        }
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int count : chars) {
            double distribution = (double) count * ALPHABET.length() / (COUNT * LENGTH);
            if (distribution > max) max = (int) distribution;
            if (distribution < min) min = (int) distribution;
        }
        Assert.assertTrue((max - min) <= 1); // Java rounding, 0.05 in JS
    }

    @Test
    public void testCustomAlphabetChangesSize() {
        String nanoidA = NanoId.customNanoid("a", 10);
        Assert.assertEquals("aaaaaaaaaa", nanoidA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNanoidThrowsOnNegativeSize() {
        NanoId.nanoid(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNanoidThrowsOnZeroSize() {
        NanoId.nanoid(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCustomNanoidThrowsOnNullAlphabet() {
        NanoId.customNanoid(null, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCustomNanoidThrowsOnEmptyAlphabet() {
        NanoId.customNanoid("", 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCustomNanoidThrowsOnNegativeSize() {
        NanoId.customNanoid("abc", -1);
    }
}
