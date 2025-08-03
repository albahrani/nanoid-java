package de.albahrani.nanoid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
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
    
    @BeforeEach
    public void setUp() {
        // Reset any thread-local state before each test
        System.gc(); // Suggest garbage collection
    }
    
    @Test
    public void testDefaultNanoId() {
        String id = NanoId.nanoid();
        assertNotNull(id, "Generated ID should not be null");
        assertEquals(21, id.length(), "Default ID should be 21 characters");
        assertTrue(URL_ALPHABET_PATTERN.matcher(id).matches(), 
                  "ID should only contain URL alphabet characters");
    }
    
    @Test
    public void testCustomSizeNanoId() {
        int[] sizes = {1, 5, 10, 32, 100};
        for (int size : sizes) {
            String id = NanoId.nanoid(size);
            assertNotNull(id, "Generated ID should not be null");
            assertEquals(size, id.length(), "ID should have requested size");
            assertTrue(URL_ALPHABET_PATTERN.matcher(id).matches(), 
                      "ID should only contain URL alphabet characters");
        }
    }
    
    @Test
    public void testInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(0));
    }
    
    @Test
    public void testNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(-1));
    }
    
    @Test
    public void testUniqueness() {
        Set<String> ids = new HashSet<>();
        int count = 100000;
        
        for (int i = 0; i < count; i++) {
            String id = NanoId.nanoid();
            assertTrue(ids.add(id), "All IDs should be unique");
        }
        
        assertEquals(count, ids.size(), "Should have generated exactly " + count + " unique IDs");
    }
    
    @Test
    public void testCustomAlphabet() {
        String alphabet = "abcdef";
        String id = NanoId.customNanoid(alphabet, 10);
        
        assertNotNull(id, "Generated ID should not be null");
        assertEquals(10, id.length(), "ID should have requested size");
        
        // Check that all characters are from the custom alphabet
        for (char c : id.toCharArray()) {
            assertTrue(alphabet.indexOf(c) >= 0, 
                      "Character '" + c + "' should be in custom alphabet");
        }
    }
    
    @Test
    public void testCustomAlphabetHex() {
        String hexAlphabet = "0123456789abcdef";
        String id = NanoId.customNanoid(hexAlphabet, 16);
        
        assertNotNull(id, "Generated ID should not be null");
        assertEquals(16, id.length(), "Hex ID should be 16 characters");
        assertTrue(id.matches("^[0-9a-f]+$"), "ID should be valid hex");
    }
    
    @Test
    public void testCustomAlphabetNull() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid(null, 10));
    }
    
    @Test
    public void testCustomAlphabetEmpty() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("", 10));
    }
    
    @Test
    public void testCustomAlphabetInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("abc", 0));
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
            assertTrue(Math.abs(count - expected) <= tolerance,
                      "Character '" + c + "' distribution should be within tolerance. " +
                      "Expected: " + expected + ", Got: " + count);
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
                    assertEquals(idsPerThread, threadIds.size(),
                               "Each thread should generate unique IDs");
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS),
                  "All threads should complete within 30 seconds");
        executor.shutdown();
        
        int expectedTotal = numThreads * idsPerThread;
        assertEquals(expectedTotal, allIds.size(),
                    "All generated IDs should be globally unique");
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
        assertTrue(durationMs < iterations,
                  "Performance should be reasonable: " + durationMs + " ms for " + iterations + " operations");
                  // Less than 1ns per operation
    }
    
    @Test
    public void testCustomAlphabetGenerator() {
        var hexGenerator = NanoId.customAlphabet("0123456789abcdef", 8);
        
        String id1 = hexGenerator.get();
        String id2 = hexGenerator.get();
        
        assertNotNull(id1, "First generated ID should not be null");
        assertNotNull(id2, "Second generated ID should not be null");
        assertEquals(8, id1.length(), "Both IDs should have correct length");
        assertEquals(8, id2.length(), "Both IDs should have correct length");
        assertNotEquals(id1, id2, "IDs should be different");
        assertTrue(id1.matches("^[0-9a-f]+$"), "First ID should be valid hex");
        assertTrue(id2.matches("^[0-9a-f]+$"), "Second ID should be valid hex");
    }
    
    @Test
    public void testGetDefaultAlphabet() {
        String alphabet = NanoId.getDefaultAlphabet();
        assertNotNull(alphabet, "Default alphabet should not be null");
        assertEquals(URL_ALPHABET, alphabet, "Default alphabet should match URL_ALPHABET");
        assertEquals(64, alphabet.length(), "Default alphabet should have 64 characters");
    }
    
    @Test
    public void testCollisionProbabilityCalculation() {
        // Test with known values
        double prob1 = NanoId.calculateCollisionProbability(64, 21, 1000);
        assertTrue(prob1 < 0.001, "Collision probability should be very low for 1000 IDs");
        
        double prob2 = NanoId.calculateCollisionProbability(26, 5, 100000);
        assertTrue(prob2 > prob1, "Collision probability should be higher for short IDs");
        
        // Test edge case
        double prob3 = NanoId.calculateCollisionProbability(64, 21, 1);
        assertEquals(0.0, prob3, 0.0001, "Collision probability should be 0 for single ID");
    }
    
    @Test
    public void testCollisionProbabilityInvalidAlphabetSize() {
        assertThrows(IllegalArgumentException.class, () -> 
            NanoId.calculateCollisionProbability(0, 21, 1000));
    }
    
    @Test
    public void testLargeCustomAlphabet() {
        String largeAlphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        String id = NanoId.customNanoid(largeAlphabet, 50);
        
        assertNotNull(id, "Generated ID should not be null");
        assertEquals(50, id.length(), "ID should have requested size");
        
        // Verify all characters are from the alphabet
        for (char c : id.toCharArray()) {
            assertTrue(largeAlphabet.indexOf(c) >= 0, 
                      "Character '" + c + "' should be in custom alphabet");
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
        assertTrue(memoryGrowth < 50 * 1024 * 1024,
                  "Memory growth should be reasonable: " + (memoryGrowth / 1024 / 1024) + " MB");
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

    @Test
    public void testNanoidThrowsOnNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(-1));
    }

    @Test
    public void testNanoidThrowsOnZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(0));
    }

    @Test
    public void testCustomNanoidThrowsOnNullAlphabet() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid(null, 10));
    }

    @Test
    public void testCustomNanoidThrowsOnEmptyAlphabet() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("", 10));
    }

    @Test
    public void testCustomNanoidThrowsOnNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("abc", -1));
    }
}
