package de.albahrani.nanoid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import static de.albahrani.nanoid.NanoIdTestUtils.*;
import java.util.*;

/**
 * Legacy compatibility tests ensuring backward compatibility with previous NanoId versions.
 * These tests preserve the exact behavior expected from the original test suite.
 * @author Alexander Al-Bahrani
 */
public class NanoIdLegacyCompatibilityTest {

    @BeforeEach
    public void setUp() {
        performMemoryCleanup();
    }

    @Nested
    class OriginalTestSuiteCompatibility {
        
        @Test
        void testGeneratesUrlFriendlyIds() {
            for (int i = 0; i < 100; i++) {
                String id = NanoId.nanoid();
                assertEquals(DEFAULT_ID_LENGTH, id.length());
                assertAlphabetCompliance(id, NanoId.getDefaultAlphabet());
            }
        }

        @Test
        void testChangesIdLength() {
            assertEquals(10, NanoId.nanoid(10).length());
        }

        @Test
        void testNoCollisions() {
            Set<String> used = new HashSet<>();
            for (int i = 0; i < LEGACY_NO_COLLISION_COUNT; i++) {
                String id = NanoId.nanoid();
                assertFalse(used.contains(id), "ID should be unique: " + id);
                used.add(id);
            }
        }

        @Test
        void testAlphabetHasNoDuplicates() {
            String alphabet = NanoId.getDefaultAlphabet();
            for (int i = 0; i < alphabet.length(); i++) {
                char currentChar = alphabet.charAt(i);
                int firstIndex = alphabet.indexOf(currentChar);
                int lastIndex = alphabet.lastIndexOf(currentChar);
                assertEquals(firstIndex, lastIndex,
                    "Character '" + currentChar + "' should appear only once in alphabet");
            }
        }

        @Test
        void testCustomAlphabetFlatDistribution() {
            String alphabet = "abcdefghijklmnopqrstuvwxyz";
            int[] chars = new int[alphabet.length()];
            
            generateLegacyDistributionData(alphabet, chars);
            validateLegacyFlatDistribution(chars, alphabet.length());
        }

        @Test
        void testCustomAlphabetChangesSize() {
            String nanoidA = NanoId.customNanoid("a", 10);
            assertEquals("aaaaaaaaaa", nanoidA,
                "Single character alphabet should repeat the character");
        }

        @Test
        void testNanoidThrowsOnNegativeSize() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(-1));
        }

        @Test
        void testNanoidThrowsOnZeroSize() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(0));
        }

        @Test
        void testCustomNanoidThrowsOnNullAlphabet() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid(null, 10));
        }

        @Test
        void testCustomNanoidThrowsOnEmptyAlphabet() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("", 10));
        }

        @Test
        void testCustomNanoidThrowsOnNegativeSize() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("abc", -1));
        }
        
        private void generateLegacyDistributionData(String alphabet, int[] chars) {
            for (int i = 0; i < LEGACY_FLAT_DISTRIBUTION_COUNT; i++) {
                String id = NanoId.customNanoid(alphabet, LEGACY_FLAT_DISTRIBUTION_LENGTH);
                for (char c : id.toCharArray()) {
                    int idx = alphabet.indexOf(c);
                    chars[idx]++;
                }
            }
        }
        
        private void validateLegacyFlatDistribution(int[] chars, int alphabetLength) {
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            
            for (int count : chars) {
                double distribution = (double) count * alphabetLength / 
                    (LEGACY_FLAT_DISTRIBUTION_COUNT * LEGACY_FLAT_DISTRIBUTION_LENGTH);
                max = Math.max(max, (int) distribution);
                min = Math.min(min, (int) distribution);
            }
            
            assertTrue((max - min) <= 1,
                "Legacy distribution test: variance should be <= 1. Max: " + max + ", Min: " + min);
        }
    }
    
    @Nested
    class DeprecatedApiCompatibility {
        
        @Test
        void deprecatedConstantsStillWork() {
            // Verify deprecated constants still work for backward compatibility
            @SuppressWarnings("deprecation")
            String urlAlphabet = NanoId.URL_ALPHABET;
            assertNotNull(urlAlphabet, "Deprecated URL_ALPHABET should still work");
            assertEquals(NanoId.getDefaultAlphabet(), urlAlphabet,
                "Deprecated URL_ALPHABET should match getDefaultAlphabet()");
            
            @SuppressWarnings("deprecation")
            String hexAlphabet = NanoId.HEX_ALPHABET;
            assertNotNull(hexAlphabet, "Deprecated HEX_ALPHABET should still work");
            assertEquals("0123456789abcdef", hexAlphabet,
                "Deprecated HEX_ALPHABET should have expected value");
        }
        
        @Test
        void deprecatedCollisionCalculationStillWorks() {
            @SuppressWarnings("deprecation")
            double prob = NanoId.calculateCollisionProbability(ALPHABET_SIZE, DEFAULT_ID_LENGTH, 1000);
            assertTrue(prob >= 0.0 && prob <= 1.0,
                "Deprecated collision probability calculation should still work");
            
            // Compare with new API to ensure consistency
            double newProb = CollisionProbabilityCalculator.calculate(ALPHABET_SIZE, DEFAULT_ID_LENGTH, 1000);
            assertEquals(newProb, prob, 0.0001,
                "Deprecated and new collision calculation should produce same results");
        }
    }
    
    @Nested
    class BehavioralCompatibility {
        
        @Test
        void maintainsOriginalPerformanceCharacteristics() {
            // Ensure performance hasn't degraded compared to original implementation
            long startTime = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                NanoId.nanoid();
            }
            long endTime = System.nanoTime();
            
            double durationMs = (endTime - startTime) / 1_000_000.0;
            assertTrue(durationMs < 1000, // Very generous limit for compatibility
                "Performance should remain reasonable: " + durationMs + " ms for 10k operations");
        }
        
        @Test
        void maintainsOriginalRandomnessQuality() {
            // Verify that randomness quality hasn't changed
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                String id = NanoId.nanoid();
                assertTrue(ids.add(id), "Should maintain high uniqueness");
            }
            assertEquals(1000, ids.size(), "All generated IDs should be unique");
        }
        
        @Test
        void maintainsOriginalAlphabetBehavior() {
            // Ensure alphabet handling remains consistent
            String customId = NanoId.customNanoid("abc", 100);
            assertEquals(100, customId.length(), "Custom alphabet size should work as before");
            
            for (char c : customId.toCharArray()) {
                assertTrue("abc".indexOf(c) >= 0, 
                    "Custom alphabet should only use specified characters");
            }
        }
        
        @Test
        void maintainsOriginalErrorHandling() {
            // Verify error conditions still throw the same exceptions
            Class<IllegalArgumentException> expected = IllegalArgumentException.class;
            
            assertThrows(expected, () -> NanoId.nanoid(0), "Zero size should throw IllegalArgumentException");
            assertThrows(expected, () -> NanoId.nanoid(-1), "Negative size should throw IllegalArgumentException");
            assertThrows(expected, () -> NanoId.customNanoid(null, 10), "Null alphabet should throw IllegalArgumentException");
            assertThrows(expected, () -> NanoId.customNanoid("", 10), "Empty alphabet should throw IllegalArgumentException");
            assertThrows(expected, () -> NanoId.customNanoid("abc", -1), "Negative size with custom alphabet should throw IllegalArgumentException");
        }
    }
}
