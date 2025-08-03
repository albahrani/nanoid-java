package de.albahrani.nanoid;

import java.util.HashSet;
import java.util.Set;

/**
 * Internal utility class for validating NanoId parameters.
 * This class provides validation methods for alphabets and sizes used in NanoId generation.
 * 
 * @author Alexander Al-Bahrani
 * @since 1.1.0
 */
final class NanoIdValidator {

    /**
     * Private constructor to prevent instantiation.
     */
    private NanoIdValidator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Validates the alphabet and size parameters for NanoId generation.
     * Throws IllegalArgumentException if invalid.
     * Ensures alphabet is not null/empty, has no duplicates, and size is positive.
     *
     * @param alphabet The alphabet to validate
     * @param size The size to validate
     * @throws IllegalArgumentException if validation fails
     */
    static void validateAlphabetAndSize(String alphabet, int size) {
        validateAlphabet(alphabet);
        validateSize(size);
        validateNoDuplicateCharacters(alphabet);
    }

    /**
     * Validates that the alphabet is not null or empty.
     *
     * @param alphabet The alphabet to validate
     * @throws IllegalArgumentException if alphabet is null or empty
     */
    static void validateAlphabet(String alphabet) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("NanoId: alphabet cannot be null or empty");
        }
    }

    /**
     * Validates that the size is positive.
     *
     * @param size The size to validate
     * @throws IllegalArgumentException if size is not positive
     */
    static void validateSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("NanoId: size must be positive, got: " + size);
        }
    }

    /**
     * Validates that the alphabet contains no duplicate characters.
     *
     * @param alphabet The alphabet to validate
     * @throws IllegalArgumentException if alphabet contains duplicate characters
     */
    static void validateNoDuplicateCharacters(String alphabet) {
        Set<Character> seen = new HashSet<>();
        for (char c : alphabet.toCharArray()) {
            if (!seen.add(c)) {
                throw new IllegalArgumentException("NanoId: alphabet contains duplicate character '" + c + "'");
            }
        }
    }

    /**
     * Validates parameters for collision probability calculation.
     *
     * @param alphabetSize The alphabet size to validate
     * @param idLength The ID length to validate  
     * @param numIds The number of IDs to validate
     * @throws IllegalArgumentException if any parameter is not positive
     */
    static void validateCollisionParameters(int alphabetSize, int idLength, long numIds) {
        if (alphabetSize <= 0) {
            throw new IllegalArgumentException("NanoId: alphabetSize must be positive, got: " + alphabetSize);
        }
        if (idLength <= 0) {
            throw new IllegalArgumentException("NanoId: idLength must be positive, got: " + idLength);
        }
        if (numIds <= 0) {
            throw new IllegalArgumentException("NanoId: numIds must be positive, got: " + numIds);
        }
    }
}
