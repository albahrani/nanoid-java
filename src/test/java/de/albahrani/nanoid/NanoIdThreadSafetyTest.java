package de.albahrani.nanoid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import static de.albahrani.nanoid.NanoIdTestUtils.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Thread safety and concurrent generation tests for NanoId.
 * @author Alexander Al-Bahrani
 */
public class NanoIdThreadSafetyTest {

    @BeforeEach
    public void setUp() {
        performMemoryCleanup();
    }

    @Nested
    class ConcurrentGenerationTests {
        
        @Test
        void maintainsUniquenessUnderConcurrentAccess() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            Set<String> allIds = Collections.synchronizedSet(new HashSet<>());
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
            try {
                submitConcurrentIdGenerationTasks(executor, allIds, latch);
                
                boolean completed = latch.await(CONCURRENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(completed, "All threads should complete within " + CONCURRENT_TIMEOUT_SECONDS + " seconds");
                
                int expectedTotal = THREAD_COUNT * IDS_PER_THREAD;
                assertEquals(expectedTotal, allIds.size(),
                            "All generated IDs should be globally unique across threads");
            } finally {
                shutdownExecutor(executor);
            }
        }
        
        @Test
        void handlesHighConcurrencyWithCustomAlphabets() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT * 2);
            Set<String> hexIds = Collections.synchronizedSet(new HashSet<>());
            Set<String> simpleIds = Collections.synchronizedSet(new HashSet<>());
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT * 2);
            
            try {
                submitMixedAlphabetTasks(executor, hexIds, simpleIds, latch);
                
                boolean completed = latch.await(CONCURRENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(completed, "All mixed alphabet threads should complete within timeout");
                
                validateConcurrentCustomAlphabetResults(hexIds, simpleIds);
            } finally {
                shutdownExecutor(executor);
            }
        }
        
        @Test
        void maintainsPerformanceUnderConcurrency() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
            long[] threadTimes = new long[THREAD_COUNT];
            
            try {
                submitPerformanceTimingTasks(executor, startLatch, endLatch, threadTimes);
                
                long startTime = System.nanoTime();
                startLatch.countDown(); // Start all threads simultaneously
                
                boolean completed = endLatch.await(CONCURRENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(completed, "Performance test should complete within timeout");
                
                long totalTime = System.nanoTime() - startTime;
                validateConcurrentPerformance(threadTimes, totalTime);
            } finally {
                shutdownExecutor(executor);
            }
        }
        
        private void submitConcurrentIdGenerationTasks(ExecutorService executor, Set<String> allIds, CountDownLatch latch) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        Set<String> threadIds = generateThreadLocalIds();
                        allIds.addAll(threadIds);
                        assertEquals(IDS_PER_THREAD, threadIds.size(),
                                   "Each thread should generate unique IDs");
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        private void submitMixedAlphabetTasks(ExecutorService executor, Set<String> hexIds, 
                                            Set<String> simpleIds, CountDownLatch latch) {
            // Half threads generate hex IDs
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < IDS_PER_THREAD / 2; j++) {
                            String hexId = NanoId.customNanoid(HEX_ALPHABET, 8);
                            hexIds.add(hexId);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Half threads generate simple alphabet IDs
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < IDS_PER_THREAD / 2; j++) {
                            String simpleId = NanoId.customNanoid(SIMPLE_ALPHABET, 6);
                            simpleIds.add(simpleId);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        private void submitPerformanceTimingTasks(ExecutorService executor, CountDownLatch startLatch, 
                                                CountDownLatch endLatch, long[] threadTimes) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for synchronized start
                        long threadStart = System.nanoTime();
                        
                        for (int j = 0; j < IDS_PER_THREAD; j++) {
                            NanoId.nanoid();
                        }
                        
                        threadTimes[threadIndex] = System.nanoTime() - threadStart;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
        }
        
        private Set<String> generateThreadLocalIds() {
            Set<String> threadIds = new HashSet<>();
            for (int j = 0; j < IDS_PER_THREAD; j++) {
                String id = NanoId.nanoid();
                threadIds.add(id);
            }
            return threadIds;
        }
        
        private void validateConcurrentCustomAlphabetResults(Set<String> hexIds, Set<String> simpleIds) {
            int expectedHexCount = THREAD_COUNT * IDS_PER_THREAD / 2;
            int expectedSimpleCount = THREAD_COUNT * IDS_PER_THREAD / 2;
            
            // Allow for some collisions in both sets due to birthday paradox
            assertTrue(hexIds.size() >= expectedHexCount * 0.99,
                      "Hex IDs should have very high uniqueness (allowing minimal collisions): " +
                      "Expected at least " + (expectedHexCount * 0.99) + ", got " + hexIds.size());
            
            // Simple alphabet has higher collision probability, so we allow more tolerance
            assertTrue(simpleIds.size() >= expectedSimpleCount * 0.6,
                      "Simple alphabet IDs should have reasonable uniqueness (allowing for some collisions): " +
                      "Expected at least " + (expectedSimpleCount * 0.6) + ", got " + simpleIds.size());
            
            // Validate alphabet compliance
            for (String hexId : hexIds) {
                assertAlphabetCompliance(hexId, HEX_ALPHABET);
            }
            for (String simpleId : simpleIds) {
                assertAlphabetCompliance(simpleId, SIMPLE_ALPHABET);
            }
        }
        
        private void validateConcurrentPerformance(long[] threadTimes, long totalTime) {
            double totalTimeMs = totalTime / 1_000_000.0;
            double totalOpsPerMs = (THREAD_COUNT * IDS_PER_THREAD) / totalTimeMs;
            
            System.out.printf("Concurrent performance: %d threads generated %d IDs in %.2f ms (%.2f ops/ms)%n",
                             THREAD_COUNT, THREAD_COUNT * IDS_PER_THREAD, totalTimeMs, totalOpsPerMs);
            
            // Verify no thread took excessively long
            for (int i = 0; i < threadTimes.length; i++) {
                double threadTimeMs = threadTimes[i] / 1_000_000.0;
                assertTrue(threadTimeMs < IDS_PER_THREAD, 
                          "Thread " + i + " should complete in reasonable time: " + threadTimeMs + " ms");
            }
        }
        
        private void shutdownExecutor(ExecutorService executor) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }
    
    @Nested
    class CustomGeneratorConcurrencyTests {
        
        @Test
        void customGeneratorsAreThreadSafe() throws InterruptedException {
            var hexGenerator = NanoId.customAlphabet(HEX_ALPHABET, 12);
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            Set<String> allIds = Collections.synchronizedSet(new HashSet<>());
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            
            try {
                for (int i = 0; i < THREAD_COUNT; i++) {
                    executor.submit(() -> {
                        try {
                            Set<String> threadIds = new HashSet<>();
                            for (int j = 0; j < IDS_PER_THREAD / 10; j++) { // Smaller set for custom generators
                                String id = hexGenerator.get();
                                threadIds.add(id);
                                assertAlphabetCompliance(id, HEX_ALPHABET);
                                assertEquals(12, id.length());
                            }
                            allIds.addAll(threadIds);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                
                boolean completed = latch.await(CONCURRENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(completed, "Custom generator concurrency test should complete within timeout");
                
                int expectedTotal = THREAD_COUNT * (IDS_PER_THREAD / 10);
                assertEquals(expectedTotal, allIds.size(),
                            "All custom generator IDs should be unique across threads");
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    executor.shutdownNow();
                }
            }
        }
    }
}
