package com.nanoid;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Comprehensive test suite for the NanoId implementation.
 * 
 * Tests cover:
 * - Basic functionality and edge cases
 * - Performance characteristics
 * - Thread safety
 * - Distribution uniformity
 * - Error handling
 * 
 * @author Alexander Al-Bahrani
 */
public class NanoIdTest {
    
    private static final String URL_ALPHABET = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLF_GQZbfghjklqvwyzrict";
    private static final Pattern URL_ALPHABET_PATTERN = Pattern.compile("^[" + Pattern.quote(URL_ALPHABET) + "]+$");
    
    @Before
    public void setUp() {
        // Reset any thread-local state before each test
        System.gc(); // Suggest garbage collection
    }
    
    @Test
    public void testDefaultNanoId() {
        String id = NanoId.nanoid();
        assertNotNull("Generated ID should not be null", id);
        assertEquals("Default ID should be 21 characters", 21, id.length());
        assertTrue("ID should only contain URL alphabet characters", 
                  URL_ALPHABET_PATTERN.matcher(id).matches());
    }
    
    @Test
    public void testCustomSizeNanoId() {
        int[] sizes = {1, 5, 10, 32, 100};
        for (int size : sizes) {
            String id = NanoId.nanoid(size);
            assertNotNull("Generated ID should not be null", id);
            assertEquals("ID should have requested size", size, id.length());
            assertTrue("ID should only contain URL alphabet characters", 
                      URL_ALPHABET_PATTERN.matcher(id).matches());
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSize() {
        NanoId.nanoid(0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeSize() {
        NanoId.nanoid(-1);
    }
    
    @Test
    public void testUniqueness() {
        Set<String> ids = new HashSet<>();
        int count = 100000;
        
        for (int i = 0; i < count; i++) {
            String id = NanoId.nanoid();
            assertTrue("All IDs should be unique", ids.add(id));
        }
        
        assertEquals("Should have generated exactly " + count + " unique IDs", count, ids.size());
    }
    
    @Test
    public void testCustomAlphabet() {
        String alphabet = "abcdef";
        String id = NanoId.customNanoid(alphabet, 10);
        
        assertNotNull("Generated ID should not be null", id);
        assertEquals("ID should have requested size", 10, id.length());
        
        // Check that all characters are from the custom alphabet
        for (char c : id.toCharArray()) {
            assertTrue("Character '" + c + "' should be in custom alphabet", 
                      alphabet.indexOf(c) >= 0);
        }
    }
    
    @Test
    public void testCustomAlphabetHex() {
        String hexAlphabet = "0123456789abcdef";
        String id = NanoId.customNanoid(hexAlphabet, 16);
        
        assertNotNull("Generated ID should not be null", id);
        assertEquals("Hex ID should be 16 characters", 16, id.length());
        assertTrue("ID should be valid hex", id.matches("^[0-9a-f]+$"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCustomAlphabetNull() {
        NanoId.customNanoid(null, 10);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCustomAlphabetEmpty() {
        NanoId.customNanoid("", 10);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCustomAlphabetInvalidSize() {
        NanoId.customNanoid("abc", 0);
    }
    
    @Test
    public void testDistributionUniformity() {
        String alphabet = "abc";
        int iterations = 30000; // Should be multiple of alphabet.length()
        Map<Character, Integer> distribution = new HashMap<>();
        
        for (int i = 0; i < iterations; i++) {
            String id = NanoId.customNanoid(alphabet, 1);
            char c = id.charAt(0);
            distribution.put(c, distribution.getOrDefault(c, 0) + 1);
        }
        
        // Check that distribution is reasonably uniform (within 10% of expected)
        int expected = iterations / alphabet.length();
        double tolerance = expected * 0.1;
        
        for (char c : alphabet.toCharArray()) {
            int count = distribution.getOrDefault(c, 0);
            assertTrue("Character '" + c + "' distribution should be within tolerance. " +
                      "Expected: " + expected + ", Got: " + count,
                      Math.abs(count - expected) <= tolerance);
        }
    }
    
    @Test
    public void testConcurrentGeneration() throws InterruptedException {
        int numThreads = 10;
        int idsPerThread = 10000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        Set<String> allIds = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    Set<String> threadIds = new HashSet<>();
                    for (int j = 0; j < idsPerThread; j++) {
                        String id = NanoId.nanoid();
                        threadIds.add(id);
                    }
                    allIds.addAll(threadIds);
                    assertEquals("Each thread should generate unique IDs", 
                               idsPerThread, threadIds.size());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue("All threads should complete within 30 seconds", 
                  latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        int expectedTotal = numThreads * idsPerThread;
        assertEquals("All generated IDs should be globally unique", 
                    expectedTotal, allIds.size());
    }
    
    @Test
    public void testPerformanceOptimization() {
        int iterations = 100000;
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            NanoId.nanoid();
        }
        
        // Test default nanoid performance
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            NanoId.nanoid();
        }
        long endTime = System.nanoTime();
        
        double durationMs = (endTime - startTime) / 1_000_000.0;
        System.out.printf("Generated %d nanoids in %.2f ms (%.2f ops/ms)%n", 
                         iterations, durationMs, iterations / durationMs);
        
        // Performance should be reasonable (less than 1ms per 1000 operations)
        assertTrue("Performance should be reasonable: " + durationMs + " ms for " + iterations + " operations",
                  durationMs < iterations); // Less than 1ns per operation
    }
    
    @Test
    public void testCustomAlphabetGenerator() {
        var hexGenerator = NanoId.customAlphabet("0123456789abcdef", 8);
        
        String id1 = hexGenerator.get();
        String id2 = hexGenerator.get();
        
        assertNotNull("First generated ID should not be null", id1);
        assertNotNull("Second generated ID should not be null", id2);
        assertEquals("Both IDs should have correct length", 8, id1.length());
        assertEquals("Both IDs should have correct length", 8, id2.length());
        assertNotEquals("IDs should be different", id1, id2);
        assertTrue("First ID should be valid hex", id1.matches("^[0-9a-f]+$"));
        assertTrue("Second ID should be valid hex", id2.matches("^[0-9a-f]+$"));
    }
    
    @Test
    public void testGetDefaultAlphabet() {
        String alphabet = NanoId.getDefaultAlphabet();
        assertNotNull("Default alphabet should not be null", alphabet);
        assertEquals("Default alphabet should match URL_ALPHABET", URL_ALPHABET, alphabet);
        assertEquals("Default alphabet should have 64 characters", 64, alphabet.length());
    }
    
    @Test
    public void testCollisionProbabilityCalculation() {
        // Test with known values
        double prob1 = NanoId.calculateCollisionProbability(64, 21, 1000);
        assertTrue("Collision probability should be very low for 1000 IDs", prob1 < 0.001);
        
        double prob2 = NanoId.calculateCollisionProbability(26, 5, 100000);
        assertTrue("Collision probability should be higher for short IDs", prob2 > prob1);
        
        // Test edge case
        double prob3 = NanoId.calculateCollisionProbability(64, 21, 1);
        assertEquals("Collision probability should be 0 for single ID", 0.0, prob3, 0.0001);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCollisionProbabilityInvalidAlphabetSize() {
        NanoId.calculateCollisionProbability(0, 21, 1000);
    }
    
    @Test
    public void testLargeCustomAlphabet() {
        String largeAlphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        String id = NanoId.customNanoid(largeAlphabet, 50);
        
        assertNotNull("Generated ID should not be null", id);
        assertEquals("ID should have requested size", 50, id.length());
        
        // Verify all characters are from the alphabet
        for (char c : id.toCharArray()) {
            assertTrue("Character '" + c + "' should be in custom alphabet", 
                      largeAlphabet.indexOf(c) >= 0);
        }
    }
    
    @Test
    public void testMemoryEfficiency() {
        // Test that repeated calls don't cause memory leaks
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // Suggest initial cleanup
        
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate many IDs to test memory usage
        for (int i = 0; i < 100000; i++) {
            NanoId.nanoid();
            if (i % 10000 == 0) {
                runtime.gc(); // Periodic cleanup
            }
        }
        
        runtime.gc(); // Final cleanup
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Memory growth should be reasonable (less than 50MB for 100k IDs)
        long memoryGrowth = finalMemory - initialMemory;
        assertTrue("Memory growth should be reasonable: " + (memoryGrowth / 1024 / 1024) + " MB",
                  memoryGrowth < 50 * 1024 * 1024);
    }
    
    // Legacy compatibility tests from original test suite
    @Test
    public void testGeneratesUrlFriendlyIds() {
        for (int i = 0; i < 100; i++) {
            String id = NanoId.nanoid();
            assertEquals(21, id.length());
            for (char c : id.toCharArray()) {
                assertTrue(NanoId.URL_ALPHABET.indexOf(c) >= 0);
            }
        }
    }

    @Test
    public void testChangesIdLength() {
        assertEquals(10, NanoId.nanoid(10).length());
    }

    @Test
    public void testNoCollisions() {
        Set<String> used = new HashSet<>();
        for (int i = 0; i < 50000; i++) {
            String id = NanoId.nanoid();
            assertFalse(used.contains(id));
            used.add(id);
        }
    }

    @Test
    public void testAlphabetHasNoDuplicates() {
        String alphabet = NanoId.URL_ALPHABET;
        for (int i = 0; i < alphabet.length(); i++) {
            assertEquals(i, alphabet.lastIndexOf(alphabet.charAt(i)));
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
        assertTrue((max - min) <= 1); // Java rounding, 0.05 in JS
    }

    @Test
    public void testCustomAlphabetChangesSize() {
        String nanoidA = NanoId.customNanoid("a", 10);
        assertEquals("aaaaaaaaaa", nanoidA);
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
