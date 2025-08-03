package de.albahrani.nanoid;

import static org.junit.jupiter.api.Assertions.*;
import java.util.regex.Pattern;

/**
 * Utility class for NanoId testing with common constants, assertions, and helper methods.
 * Follows the DRY principle by centralizing reusable test code.
 */
public final class NanoIdTestUtils {
    
    // Test Constants
    public static final String URL_ALPHABET = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLF_GQZbfghjklqvwyzrict";
    public static final Pattern URL_ALPHABET_PATTERN = Pattern.compile("^[" + Pattern.quote(URL_ALPHABET) + "]+$");
    public static final int DEFAULT_ID_LENGTH = 21;
    public static final int ALPHABET_SIZE = 64;
    
    // Performance Test Constants
    public static final int LARGE_ID_COUNT = 100_000;
    public static final int PERFORMANCE_ITERATIONS = 100_000;
    public static final int MEMORY_LIMIT_MB = 50;
    
    // Concurrency Test Constants
    public static final int THREAD_COUNT = 10;
    public static final int IDS_PER_THREAD = 10_000;
    public static final int CONCURRENT_TIMEOUT_SECONDS = 30;
    
    // Distribution Test Constants
    public static final int DISTRIBUTION_SAMPLE_SIZE = 30_000;
    public static final double DISTRIBUTION_TOLERANCE = 0.1;
    
    // Legacy Test Constants
    public static final int LEGACY_NO_COLLISION_COUNT = 50_000;
    public static final int LEGACY_FLAT_DISTRIBUTION_COUNT = 50_000;
    public static final int LEGACY_FLAT_DISTRIBUTION_LENGTH = 30;
    
    // Test Data
    public static final int[] COMMON_ID_SIZES = {1, 5, 10, 32, 100};
    public static final String HEX_ALPHABET = "0123456789abcdef";
    public static final String SIMPLE_ALPHABET = "abcdef";
    public static final String LARGE_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
    
    private NanoIdTestUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Asserts that the given ID is a valid NanoId with default characteristics.
     */
    public static void assertValidDefaultNanoId(String id) {
        assertNotNull(id, "Generated ID should not be null");
        assertEquals(DEFAULT_ID_LENGTH, id.length(), "Default ID should be " + DEFAULT_ID_LENGTH + " characters");
        assertTrue(URL_ALPHABET_PATTERN.matcher(id).matches(), "ID should only contain URL alphabet characters");
    }
    
    /**
     * Asserts that the given ID has the expected size and contains only URL alphabet characters.
     */
    public static void assertValidNanoId(String id, int expectedSize) {
        assertNotNull(id, "Generated ID should not be null");
        assertEquals(expectedSize, id.length(), "ID should have expected size: " + expectedSize);
        assertTrue(URL_ALPHABET_PATTERN.matcher(id).matches(), "ID should only contain URL alphabet characters");
    }
    
    /**
     * Asserts that the given ID contains only characters from the specified alphabet.
     */
    public static void assertAlphabetCompliance(String id, String alphabet) {
        assertNotNull(id, "Generated ID should not be null");
        for (char c : id.toCharArray()) {
            assertTrue(alphabet.indexOf(c) >= 0, 
                "Character '" + c + "' should be in alphabet: " + alphabet);
        }
    }
    
    /**
     * Asserts that the given ID has the expected size and contains only characters from the specified alphabet.
     */
    public static void assertValidCustomNanoId(String id, int expectedSize, String alphabet) {
        assertNotNull(id, "Generated ID should not be null");
        assertEquals(expectedSize, id.length(), "ID should have expected size: " + expectedSize);
        assertAlphabetCompliance(id, alphabet);
    }
    
    /**
     * Asserts that the distribution of characters is within acceptable tolerance.
     */
    public static void assertDistributionUniformity(int[] distribution, int expected, double tolerance) {
        for (int i = 0; i < distribution.length; i++) {
            int count = distribution[i];
            assertTrue(Math.abs(count - expected) <= tolerance,
                String.format("Character at index %d distribution should be within tolerance. Expected: %d, Got: %d", 
                    i, expected, count));
        }
    }
    
    /**
     * Performs memory cleanup to ensure accurate memory measurements.
     */
    public static void performMemoryCleanup() {
        System.gc();
        try {
            Thread.sleep(10); // Give GC a moment
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Calculates the expected count per character for uniform distribution.
     */
    public static int calculateExpectedDistribution(int totalSamples, int alphabetSize) {
        return totalSamples / alphabetSize;
    }
    
    /**
     * Calculates the tolerance for distribution testing.
     */
    public static double calculateDistributionTolerance(int expected, double tolerancePercentage) {
        return expected * tolerancePercentage;
    }
}
