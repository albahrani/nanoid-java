package de.albahrani.nanoid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import static de.albahrani.nanoid.NanoIdTestUtils.*;
import java.util.*;

/**
 * Performance and scalability tests for NanoId focusing on speed, memory efficiency, and distribution quality.
 * @author Alexander Al-Bahrani
 */
public class NanoIdPerformanceTest {

    @BeforeEach
    public void setUp() {
        performMemoryCleanup();
    }

    @Nested
    class PerformanceBenchmarkTests {
        
        @Test
        void measuresDefaultNanoIdPerformance() {
            // Warm up the JVM
            warmUpGenerator();
            
            long startTime = System.nanoTime();
            for (int i = 0; i < PERFORMANCE_ITERATIONS; i++) {
                NanoId.nanoid();
            }
            long endTime = System.nanoTime();
            
            double durationMs = (endTime - startTime) / 1_000_000.0;
            double operationsPerMs = PERFORMANCE_ITERATIONS / durationMs;
            
            System.out.printf("Generated %d nanoids in %.2f ms (%.2f ops/ms)%n", 
                             PERFORMANCE_ITERATIONS, durationMs, operationsPerMs);
            
            assertTrue(durationMs < PERFORMANCE_ITERATIONS,
                      "Performance should be reasonable: " + durationMs + " ms for " + PERFORMANCE_ITERATIONS + " operations");
        }
        
        private void warmUpGenerator() {
            for (int i = 0; i < 1000; i++) {
                NanoId.nanoid();
            }
        }
    }
    
    @Nested
    class MemoryEfficiencyTests {
        
        @Test
        void maintainsReasonableMemoryUsage() {
            Runtime runtime = Runtime.getRuntime();
            performMemoryCleanup();
            
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            generateIdsWithPeriodicCleanup();
            
            performMemoryCleanup();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryGrowth = finalMemory - initialMemory;
            long memoryGrowthMB = memoryGrowth / (1024 * 1024);
            
            assertTrue(memoryGrowth < MEMORY_LIMIT_MB * 1024 * 1024,
                      "Memory growth should be reasonable: " + memoryGrowthMB + " MB (limit: " + MEMORY_LIMIT_MB + " MB)");
        }
        
        private void generateIdsWithPeriodicCleanup() {
            for (int i = 0; i < LARGE_ID_COUNT; i++) {
                NanoId.nanoid();
                if (i % 10_000 == 0) {
                    System.gc(); // Periodic cleanup
                }
            }
        }
    }
    
    @Nested
    class DistributionQualityTests {
        
        @Test
        void achievesUniformCharacterDistribution() {
            String alphabet = SIMPLE_ALPHABET;
            Map<Character, Integer> distribution = generateDistributionSample(alphabet);
            
            validateDistributionUniformity(distribution, alphabet);
        }
        
        @Test
        void maintainsFlatDistributionWithCustomAlphabet() {
            String alphabet = "abcdefghijklmnopqrstuvwxyz";
            int[] charCounts = new int[alphabet.length()];
            
            generateCharacterDistributionData(alphabet, charCounts);
            
            int expectedPerChar = calculateExpectedDistribution(
                LEGACY_FLAT_DISTRIBUTION_COUNT * LEGACY_FLAT_DISTRIBUTION_LENGTH, 
                alphabet.length()
            );
            
            validateFlatDistribution(charCounts, expectedPerChar);
        }
        
        private Map<Character, Integer> generateDistributionSample(String alphabet) {
            Map<Character, Integer> distribution = new HashMap<>();
            for (int i = 0; i < DISTRIBUTION_SAMPLE_SIZE; i++) {
                String id = NanoId.customNanoid(alphabet, 1);
                char c = id.charAt(0);
                distribution.put(c, distribution.getOrDefault(c, 0) + 1);
            }
            return distribution;
        }
        
        private void validateDistributionUniformity(Map<Character, Integer> distribution, String alphabet) {
            int expected = calculateExpectedDistribution(DISTRIBUTION_SAMPLE_SIZE, alphabet.length());
            double tolerance = calculateDistributionTolerance(expected, DISTRIBUTION_TOLERANCE);
            
            for (char c : alphabet.toCharArray()) {
                int count = distribution.getOrDefault(c, 0);
                assertTrue(Math.abs(count - expected) <= tolerance,
                          "Character '" + c + "' distribution should be within tolerance. " +
                          "Expected: " + expected + ", Got: " + count);
            }
        }
        
        private void generateCharacterDistributionData(String alphabet, int[] charCounts) {
            for (int i = 0; i < LEGACY_FLAT_DISTRIBUTION_COUNT; i++) {
                String id = NanoId.customNanoid(alphabet, LEGACY_FLAT_DISTRIBUTION_LENGTH);
                for (char c : id.toCharArray()) {
                    int idx = alphabet.indexOf(c);
                    charCounts[idx]++;
                }
            }
        }
        
        private void validateFlatDistribution(int[] charCounts, int expectedPerChar) {
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            
            for (int count : charCounts) {
                double distribution = (double) count * charCounts.length / 
                    (LEGACY_FLAT_DISTRIBUTION_COUNT * LEGACY_FLAT_DISTRIBUTION_LENGTH);
                max = Math.max(max, (int) distribution);
                min = Math.min(min, (int) distribution);
            }
            
            assertTrue((max - min) <= 1, 
                "Distribution variance should be minimal. Max: " + max + ", Min: " + min);
        }
    }
    
    @Nested
    class ScalabilityTests {
        
        @Test
        void handlesLargeVolumeGeneration() {
            Set<String> ids = new HashSet<>();
            int batchSize = 10_000;
            
            for (int batch = 0; batch < LARGE_ID_COUNT / batchSize; batch++) {
                for (int i = 0; i < batchSize; i++) {
                    String id = NanoId.nanoid();
                    assertTrue(ids.add(id), "All IDs should remain unique even at scale");
                }
                
                // Periodic validation
                if (batch % 2 == 0) {
                    performMemoryCleanup();
                }
            }
            
            assertEquals(LARGE_ID_COUNT, ids.size(), 
                "Should maintain uniqueness across " + LARGE_ID_COUNT + " IDs");
        }
    }
}
