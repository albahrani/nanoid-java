package de.albahrani.nanoid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import static de.albahrani.nanoid.NanoIdTestUtils.*;
import java.util.*;

/**
 * Main test suite for NanoId providing a comprehensive overview of all functionality.
 * This class serves as an integration test and quick smoke test for the NanoId library.
 * For detailed testing, see the specialized test classes:
 * - {@link NanoIdFunctionalityTest} for core functionality
 * - {@link NanoIdPerformanceTest} for performance and scalability
 * - {@link NanoIdThreadSafetyTest} for concurrency tests
 * - {@link NanoIdMathematicalTest} for probability calculations
 * - {@link NanoIdLegacyCompatibilityTest} for backward compatibility
 * @author Alexander Al-Bahrani
 */
public class NanoIdTest {

    @BeforeEach
    public void setUp() {
        performMemoryCleanup();
    }

    @Nested
    class SmokeTests {
        
        @Test
        void basicNanoIdGeneration() {
            String id = NanoId.nanoid();
            assertValidDefaultNanoId(id);
        }

        @Test
        void customSizeGeneration() {
            String id = NanoId.nanoid(10);
            assertValidNanoId(id, 10);
        }

        @Test
        void customAlphabetGeneration() {
            String id = NanoId.customNanoid(HEX_ALPHABET, 8);
            assertValidCustomNanoId(id, 8, HEX_ALPHABET);
        }

        @Test
        void basicUniquenessCheck() {
            String id1 = NanoId.nanoid();
            String id2 = NanoId.nanoid();
            assertNotEquals(id1, id2, "Sequential IDs should be different");
        }

        @Test
        void basicErrorHandling() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(0));
            assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("", 10));
        }
    }
    
    @Nested
    class IntegrationTests {
        
        @Test
        void defaultAlphabetIntegrity() {
            String alphabet = NanoId.getDefaultAlphabet();
            assertNotNull(alphabet, "Default alphabet should not be null");
            assertEquals(ALPHABET_SIZE, alphabet.length(), "Default alphabet should have correct size");
            
            // Verify no duplicates
            for (int i = 0; i < alphabet.length(); i++) {
                char c = alphabet.charAt(i);
                assertEquals(i, alphabet.lastIndexOf(c), 
                    "Character '" + c + "' should appear only once");
            }
        }
        
        @Test
        void customGeneratorIntegration() {
            var generator = NanoId.customAlphabet(SIMPLE_ALPHABET, 6);
            String id1 = generator.get();
            String id2 = generator.get();
            
            assertValidCustomNanoId(id1, 6, SIMPLE_ALPHABET);
            assertValidCustomNanoId(id2, 6, SIMPLE_ALPHABET);
            assertNotEquals(id1, id2, "Generator should produce different IDs");
        }
        
        @Test
        void convenienceMethodsIntegration() {
            String hexId = NanoId.nanoidHex();
            String base58Id = NanoId.nanoidBase58();
            String shortId = NanoId.nanoidShort();
            
            assertNotNull(hexId, "Hex ID should not be null");
            assertNotNull(base58Id, "Base58 ID should not be null");
            assertNotNull(shortId, "Short ID should not be null");
            
            assertEquals(16, hexId.length(), "Hex ID should have correct length");
            assertEquals(22, base58Id.length(), "Base58 ID should have correct length");
            assertEquals(8, shortId.length(), "Short ID should have correct length");
        }
        
        @Test
        void moderateVolumeGeneration() {
            Set<String> ids = new HashSet<>();
            int count = 1000;
            
            for (int i = 0; i < count; i++) {
                String id = NanoId.nanoid();
                assertTrue(ids.add(id), "All IDs should be unique in moderate volume test");
            }
            
            assertEquals(count, ids.size(), "Should generate expected number of unique IDs");
        }
    }
    
    @Nested
    class QuickPerformanceCheck {
        
        @Test
        void reasonablePerformance() {
            // Warm up
            for (int i = 0; i < 100; i++) {
                NanoId.nanoid();
            }
            
            long startTime = System.nanoTime();
            int iterations = 10_000;
            for (int i = 0; i < iterations; i++) {
                NanoId.nanoid();
            }
            long endTime = System.nanoTime();
            
            double durationMs = (endTime - startTime) / 1_000_000.0;
            double opsPerMs = iterations / durationMs;
            
            assertTrue(opsPerMs > 10, 
                "Should generate at least 10 IDs per millisecond: " + opsPerMs + " ops/ms");
        }
    }
}
