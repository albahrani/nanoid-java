package de.albahrani.nanoid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static de.albahrani.nanoid.NanoIdTestUtils.*;

/**
 * Mathematical and probability-related tests for NanoId, including collision probability calculations.
 * @author Alexander Al-Bahrani
 */
public class NanoIdMathematicalTest {

    @BeforeEach
    public void setUp() {
        performMemoryCleanup();
    }

    @Nested
    class CollisionProbabilityTests {
        
        @Test
        void calculatesLowCollisionProbabilityForTypicalUsage() {
            double prob = CollisionProbabilityCalculator.calculate(ALPHABET_SIZE, DEFAULT_ID_LENGTH, 1000);
            assertTrue(prob < 0.001, 
                "Collision probability should be very low for 1000 IDs with default settings: " + prob);
        }
        
        @Test
        void calculatesHigherProbabilityForShortIds() {
            double shortIdProb = CollisionProbabilityCalculator.calculate(26, 5, 100000);
            double longIdProb = CollisionProbabilityCalculator.calculate(ALPHABET_SIZE, DEFAULT_ID_LENGTH, 1000);
            
            assertTrue(shortIdProb > longIdProb, 
                "Collision probability should be higher for short IDs: " + shortIdProb + " vs " + longIdProb);
        }
        
        @Test
        void returnsZeroProbabilityForSingleId() {
            double prob = CollisionProbabilityCalculator.calculate(ALPHABET_SIZE, DEFAULT_ID_LENGTH, 1);
            assertEquals(0.0, prob, 0.0001, 
                "Collision probability should be 0 for single ID generation");
        }
        
        @ParameterizedTest
        @CsvSource({
            "64, 21, 1000, 0.001",
            "26, 10, 10000, 0.5",
            "16, 8, 100000, 0.9"
        })
        void calculatesProbabilityWithinExpectedRanges(int alphabetSize, int idLength, int count, double maxExpected) {
            double prob = CollisionProbabilityCalculator.calculate(alphabetSize, idLength, count);
            assertTrue(prob <= maxExpected, 
                String.format("Probability %.6f should be <= %.3f for alphabet=%d, length=%d, count=%d", 
                    prob, maxExpected, alphabetSize, idLength, count));
        }
        
        @Test
        void throwsOnInvalidAlphabetSize() {
            assertThrows(IllegalArgumentException.class, () -> 
                CollisionProbabilityCalculator.calculate(0, DEFAULT_ID_LENGTH, 1000),
                "Should throw exception for zero alphabet size");
            
            assertThrows(IllegalArgumentException.class, () -> 
                CollisionProbabilityCalculator.calculate(-1, DEFAULT_ID_LENGTH, 1000),
                "Should throw exception for negative alphabet size");
        }
        
        @Test
        void handlesEdgeCasesGracefully() {
            // Very large numbers
            double prob1 = CollisionProbabilityCalculator.calculate(ALPHABET_SIZE, DEFAULT_ID_LENGTH, Integer.MAX_VALUE);
            assertTrue(prob1 >= 0.0 && prob1 <= 1.0, 
                "Probability should be between 0 and 1 for large count");
            
            // Very small alphabet
            double prob2 = CollisionProbabilityCalculator.calculate(2, 1, 10);
            assertTrue(prob2 > 0.0, 
                "Probability should be positive for small alphabet and high count");
        }
    }
    
    @Nested
    class StatisticalValidationTests {
        
        @Test
        void validatesBirthdayParadoxBehavior() {
            // Test the birthday paradox with a smaller alphabet for faster computation
            int alphabetSize = 365; // Like days in a year
            int idLength = 1; // Single character from "year"
            
            // 23 people have ~50% chance of sharing a birthday
            double prob23 = CollisionProbabilityCalculator.calculate(alphabetSize, idLength, 23);
            assertTrue(prob23 > 0.4 && prob23 < 0.6, 
                "Birthday paradox should show ~50% probability for 23 samples: " + prob23);
            
            // 70 people have ~99.9% chance
            double prob70 = CollisionProbabilityCalculator.calculate(alphabetSize, idLength, 70);
            assertTrue(prob70 > 0.99, 
                "High sample count should show very high collision probability: " + prob70);
        }
        
        @Test
        void demonstratesScalingBehaviorWithAlphabetSize() {
            int count = 1000;
            int length = 8;
            
            double prob4 = CollisionProbabilityCalculator.calculate(4, length, count);
            double prob16 = CollisionProbabilityCalculator.calculate(16, length, count);
            double prob64 = CollisionProbabilityCalculator.calculate(64, length, count);
            
            assertTrue(prob4 > prob16, 
                "Smaller alphabet should have higher collision probability");
            assertTrue(prob16 > prob64, 
                "Medium alphabet should have higher collision probability than large alphabet");
            
            System.out.printf("Probability scaling: alphabet 4: %.6f, 16: %.6f, 64: %.6f%n", 
                prob4, prob16, prob64);
        }
        
        @Test
        void demonstratesScalingBehaviorWithIdLength() {
            int alphabetSize = 64;
            int count = 1000;
            
            double prob5 = CollisionProbabilityCalculator.calculate(alphabetSize, 5, count);
            double prob10 = CollisionProbabilityCalculator.calculate(alphabetSize, 10, count);
            double prob21 = CollisionProbabilityCalculator.calculate(alphabetSize, 21, count);
            
            assertTrue(prob5 > prob10, 
                "Shorter IDs should have higher collision probability");
            assertTrue(prob10 > prob21, 
                "Medium length IDs should have higher collision probability than long IDs");
            
            System.out.printf("Length scaling: 5 chars: %.6f, 10 chars: %.6f, 21 chars: %.6f%n", 
                prob5, prob10, prob21);
        }
    }
    
    @Nested
    class EntropyAndSecurityTests {
        
        @Test
        void calculatesApproximateEntropyForDefaultSettings() {
            // Entropy = log2(alphabet_size^id_length)
            // For 64^21 ≈ 2^126, so entropy ≈ 126 bits
            double entropyBits = Math.log(Math.pow(ALPHABET_SIZE, DEFAULT_ID_LENGTH)) / Math.log(2);
            
            assertTrue(entropyBits > 125, 
                "Default NanoId should provide high entropy: " + entropyBits + " bits");
            assertTrue(entropyBits < 127, 
                "Entropy calculation should be reasonable: " + entropyBits + " bits");
        }
        
        @Test
        void comparesSecurityOfDifferentConfigurations() {
            // Compare entropy of different configurations
            double defaultEntropy = calculateEntropy(ALPHABET_SIZE, DEFAULT_ID_LENGTH);
            double shortEntropy = calculateEntropy(ALPHABET_SIZE, 10);
            double hexEntropy = calculateEntropy(16, 16);
            
            assertTrue(defaultEntropy > shortEntropy, 
                "Default config should have higher entropy than short IDs");
            assertTrue(defaultEntropy > hexEntropy, 
                "Default config should have higher entropy than hex IDs");
            
            System.out.printf("Entropy comparison - Default: %.1f bits, Short: %.1f bits, Hex: %.1f bits%n",
                defaultEntropy, shortEntropy, hexEntropy);
        }
        
        private double calculateEntropy(int alphabetSize, int length) {
            return Math.log(Math.pow(alphabetSize, length)) / Math.log(2);
        }
    }
    
    @Nested
    class PracticalSecurityTests {
        
        @Test
        void estimatesBruteForceResistance() {
            // Estimate time to brute force with different attack rates
            double possibleIds = Math.pow(ALPHABET_SIZE, DEFAULT_ID_LENGTH);
            
            // Conservative estimates for brute force attacks
            long attemptsPerSecond = 1_000_000; // 1M attempts per second
            double secondsToHalfSpace = possibleIds / (2.0 * attemptsPerSecond);
            long yearsToHalfSpace = (long) (secondsToHalfSpace / (365.0 * 24 * 3600));
            
            assertTrue(yearsToHalfSpace > 100_000, 
                "Default NanoId should resist brute force for hundreds of thousands of years: " + yearsToHalfSpace);
        }
        
        @Test
        void validatesPracticalCollisionResistance() {
            // For practical applications, collision probability should be negligible
            // even with billions of IDs
            double probBillion = CollisionProbabilityCalculator.calculate(ALPHABET_SIZE, DEFAULT_ID_LENGTH, 1_000_000_000);
            
            assertTrue(probBillion < 0.000001, 
                "Collision probability should be negligible even for billion IDs: " + probBillion);
        }
    }
}
