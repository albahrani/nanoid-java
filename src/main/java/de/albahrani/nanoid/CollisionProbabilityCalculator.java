package de.albahrani.nanoid;

/**
 * Utility class for calculating collision probabilities in NanoId generation.
 * This class provides mathematical calculations for determining the likelihood of ID collisions.
 * 
 * @author Alexander Al-Bahrani
 * @since 1.1.0
 */
public final class CollisionProbabilityCalculator {

    /**
     * Private constructor to prevent instantiation.
     */
    private CollisionProbabilityCalculator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Calculates the approximate collision probability for the given alphabet size and ID length.
     * This is useful for determining appropriate ID lengths for your use case.
     * Uses the birthday paradox approximation: P ≈ 1 - e^(-n²/(2N))
     * where n = number of IDs, N = total possible IDs
     * 
     * @param alphabetSize the size of the alphabet
     * @param idLength the length of the ID
     * @param numIds expected number of IDs to generate
     * @return approximate collision probability as a double between 0 and 1
     * @throws IllegalArgumentException if any parameter is not positive
     */
    public static double calculate(int alphabetSize, int idLength, long numIds) {
        NanoIdValidator.validateCollisionParameters(alphabetSize, idLength, numIds);
        
        double totalPossibleIds = Math.pow(alphabetSize, idLength);
        // Birthday paradox approximation: P ≈ 1 - e^(-n²/(2N))
        // where n = number of IDs, N = total possible IDs
        double exponent = -(numIds * numIds) / (2.0 * totalPossibleIds);
        return 1.0 - Math.exp(exponent);
    }
}
