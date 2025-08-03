package de.albahrani.nanoid;

import java.util.HashMap;
import java.util.Map;

/**
 * Test utilities for NanoId distribution and uniformity testing.
 * These utilities are useful for advanced users to verify the quality of ID generation.
 * 
 * @author Alexander Al-Bahrani
 * @since 1.1.0
 */
public final class NanoIdTestUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private NanoIdTestUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Utility method to test the uniformity of generated IDs for a given alphabet and length.
     * Returns a map of character frequencies after generating the specified number of IDs.
     * Useful for advanced users to verify distribution.
     *
     * @param alphabet The alphabet to use
     * @param idLength The length of each ID
     * @param sampleSize The number of IDs to generate
     * @return Map of character to frequency
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static Map<Character, Integer> testDistribution(String alphabet, int idLength, int sampleSize) {
        NanoIdValidator.validateAlphabetAndSize(alphabet, idLength);
        if (sampleSize <= 0) {
            throw new IllegalArgumentException("NanoId: sampleSize must be positive, got: " + sampleSize);
        }
        
        Map<Character, Integer> freq = new HashMap<>();
        for (int i = 0; i < sampleSize; i++) {
            String id = NanoId.customNanoid(alphabet, idLength);
            for (char c : id.toCharArray()) {
                freq.put(c, freq.getOrDefault(c, 0) + 1);
            }
        }
        return freq;
    }

    /**
     * Analyzes the distribution uniformity for a given character frequency map.
     * Returns statistics about the distribution quality.
     *
     * @param frequencies Character frequency map from testDistribution
     * @param expectedTotal Expected total number of characters
     * @return DistributionStats containing uniformity metrics
     */
    public static DistributionStats analyzeDistribution(Map<Character, Integer> frequencies, int expectedTotal) {
        if (frequencies == null || frequencies.isEmpty()) {
            throw new IllegalArgumentException("NanoId: frequencies map cannot be null or empty");
        }
        
        int actualTotal = frequencies.values().stream().mapToInt(Integer::intValue).sum();
        double expectedFrequency = (double) actualTotal / frequencies.size();
        
        double variance = frequencies.values().stream()
            .mapToDouble(count -> Math.pow(count - expectedFrequency, 2))
            .average()
            .orElse(0.0);
        
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = standardDeviation / expectedFrequency;
        
        int min = frequencies.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = frequencies.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        
        return new DistributionStats(actualTotal, expectedFrequency, variance, standardDeviation, 
                                   coefficientOfVariation, min, max);
    }

    /**
     * Statistics about character distribution uniformity.
     */
    public static record DistributionStats(
        int totalCharacters,
        double expectedFrequency,
        double variance,
        double standardDeviation,
        double coefficientOfVariation,
        int minFrequency,
        int maxFrequency
    ) {
        /**
         * Returns true if the distribution is considered uniform within acceptable bounds.
         * Uses coefficient of variation as the primary metric.
         * 
         * @param tolerance acceptable coefficient of variation (typically 0.1 for 10% tolerance)
         * @return true if distribution is uniform within tolerance
         */
        public boolean isUniform(double tolerance) {
            return coefficientOfVariation <= tolerance;
        }
        
        /**
         * Returns a human-readable assessment of the distribution quality.
         * 
         * @return distribution quality assessment
         */
        public String getQualityAssessment() {
            if (coefficientOfVariation <= 0.05) {
                return "Excellent";
            } else if (coefficientOfVariation <= 0.1) {
                return "Good";
            } else if (coefficientOfVariation <= 0.15) {
                return "Fair";
            } else {
                return "Poor";
            }
        }
    }
}
