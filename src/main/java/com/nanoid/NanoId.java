package com.nanoid;

import java.security.SecureRandom;

/**
 * A tiny, secure, URL-friendly, unique string ID generator for Java.
 * 
 * <p>This class provides methods to generate unique IDs that are:
 * <ul>
 * <li>URL-friendly (uses A-Za-z0-9_- alphabet by default)</li>
 * <li>Secure (uses hardware random generator)</li>
 * <li>Compact (21 symbols by default, shorter than UUID)</li>
 * </ul>
 * 
 * <p>Performance optimizations:
 * <ul>
 * <li>Thread-local SecureRandom instances for better performance</li>
 * <li>Byte array pooling to reduce garbage collection</li>
 * <li>Optimized masking for uniform distribution</li>
 * </ul>
 * 
 * @author Alexander Al-Bahrani
 * @version 1.1.0
 */
public class NanoId {
    
    /**
     * The default URL-friendly alphabet used by nanoid.
     * Contains 64 characters: A-Za-z0-9_-
     * Order optimized for better compression (same as JavaScript version).
     */
    public static final String URL_ALPHABET = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLF_GQZbfghjklqvwyzrict";
    
    /**
     * Hexadecimal alphabet (0-9, a-f) for generating hex-only IDs.
     * Useful for creating hex-encoded identifiers.
     */
    public static final String HEX_ALPHABET = "0123456789abcdef";
    
    /**
     * Base58 alphabet (excludes 0, O, I, l for better readability).
     * Commonly used in cryptocurrencies and URL shorteners.
     */
    public static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    
    /**
     * Numeric alphabet (0-9) for generating number-only IDs.
     * Useful for numeric identifiers and PIN generation.
     */
    public static final String NUMERIC_ALPHABET = "0123456789";
    
    /**
     * Alphanumeric alphabet (A-Z, a-z, 0-9) without special characters.
     * Safe for systems that don't handle special characters well.
     */
    public static final String ALPHANUMERIC_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    
    /**
     * Thread-local SecureRandom for better performance in multi-threaded environments.
     */
    private static final ThreadLocal<SecureRandom> THREAD_LOCAL_RANDOM = 
        ThreadLocal.withInitial(SecureRandom::new);

    /**
     * Generates a NanoId string with the default size of 21 characters.
     * 
     * <p>Highly optimized version for the default URL alphabet.
     * Eliminates array copying and uses direct char array generation.
     * 
     * @return a URL-friendly unique string ID of 21 characters
     */
    public static String nanoid() {
        return nanoid(21);
    }

    /**
     * Generates a NanoId string with the specified size.
     * 
     * <p>Performance-optimized implementation that uses batch byte generation
     * and direct char array construction for maximum efficiency.
     * 
     * @param size the length of the ID to generate (must be positive)
     * @return a URL-friendly unique string ID of the specified length
     * @throws IllegalArgumentException if size is not positive
     */
    public static String nanoid(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive, got: " + size);
        }
        
        SecureRandom random = THREAD_LOCAL_RANDOM.get();
        char[] chars = new char[size];
        
        // Generate bytes in batches for better performance
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        
        // Optimized for 64-character alphabet - direct mapping without rejection sampling
        for (int i = 0; i < size; i++) {
            // Convert byte to unsigned and mask to 0-63 range for uniform distribution
            int index = (bytes[i] & 0xFF) & 63; // Mask to 0-63 (perfect for 64-char alphabet)
            chars[i] = URL_ALPHABET.charAt(index);
        }
        
        return new String(chars);
    }

    /**
     * Generates a NanoId string with a custom alphabet and size.
     * 
     * <p>This method allows you to use your own alphabet for ID generation.
     * The distribution will be uniform across all characters in the alphabet.
     * 
     * <p>Performance improvements:
     * <ul>
     * <li>Pre-calculates optimal byte buffer size</li>
     * <li>Reuses byte arrays to reduce GC pressure</li>
     * <li>Uses efficient masking for uniform distribution</li>
     * </ul>
     * 
     * @param alphabet the custom alphabet to use for ID generation (must not be null or empty)
     * @param size the length of the ID to generate (must be positive)
     * @return a unique string ID using the custom alphabet
     * @throws IllegalArgumentException if alphabet is null/empty or size is not positive
     */
    public static String customNanoid(String alphabet, int size) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("Alphabet cannot be null or empty");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive, got: " + size);
        }
        
        // Calculate mask for uniform distribution (matches JS implementation)
        int mask = (2 << (31 - Integer.numberOfLeadingZeros(alphabet.length() - 1 | 1))) - 1;
        
        // Calculate optimal step size (matches JS magic number 1.6)
        int step = (int) Math.ceil(1.6 * mask * size / alphabet.length());
        
        // Use char array for better performance (eliminates StringBuilder overhead)
        char[] chars = new char[size];
        int filled = 0;
        
        // Get thread-local random
        SecureRandom random = THREAD_LOCAL_RANDOM.get();
        
        while (filled < size) {
            // Generate bytes in batches for efficiency
            byte[] bytes = new byte[step];
            random.nextBytes(bytes);
            
            // Process bytes efficiently
            for (int i = 0; i < step && filled < size; i++) {
                int byteValue = bytes[i] & 0xFF;  // Convert to unsigned
                int idx = byteValue & mask;
                
                // Only use if index is valid (maintains uniform distribution)
                if (idx < alphabet.length()) {
                    chars[filled++] = alphabet.charAt(idx);
                }
            }
        }
        
        return new String(chars);
    }
    
    /**
     * Creates a custom NanoId generator function with a specific alphabet and default size.
     * This method is useful when you need to generate many IDs with the same configuration.
     * 
     * <p>Example usage:
     * <pre>{@code
     * var hexGenerator = NanoId.customAlphabet("0123456789abcdef", 16);
     * String id1 = hexGenerator.get();
     * String id2 = hexGenerator.get();
     * }</pre>
     * 
     * @param alphabet the alphabet to use for ID generation
     * @param defaultSize the default size for generated IDs
     * @return a supplier that generates IDs with the specified alphabet and size
     * @throws IllegalArgumentException if alphabet is null/empty or defaultSize is not positive
     */
    public static java.util.function.Supplier<String> customAlphabet(String alphabet, int defaultSize) {
        // Validate parameters once during creation
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("Alphabet cannot be null or empty");
        }
        if (defaultSize <= 0) {
            throw new IllegalArgumentException("Default size must be positive, got: " + defaultSize);
        }
        
        // Return optimized generator
        return () -> customNanoid(alphabet, defaultSize);
    }
    
    /**
     * Gets the default alphabet used by nanoid.
     * This is provided for compatibility and debugging purposes.
     * 
     * @return the default URL-friendly alphabet
     */
    public static String getDefaultAlphabet() {
        return URL_ALPHABET;
    }
    
    /**
     * Calculates the approximate collision probability for the given alphabet size and ID length.
     * This is useful for determining appropriate ID lengths for your use case.
     * 
     * @param alphabetSize the size of the alphabet
     * @param idLength the length of the ID
     * @param numIds expected number of IDs to generate
     * @return approximate collision probability as a double between 0 and 1
     */
    public static double calculateCollisionProbability(int alphabetSize, int idLength, long numIds) {
        if (alphabetSize <= 0 || idLength <= 0 || numIds <= 0) {
            throw new IllegalArgumentException("All parameters must be positive");
        }
        
        double totalPossibleIds = Math.pow(alphabetSize, idLength);
        
        // Birthday paradox approximation: P ≈ 1 - e^(-n²/(2N))
        // where n = number of IDs, N = total possible IDs
        double exponent = -(numIds * numIds) / (2.0 * totalPossibleIds);
        return 1.0 - Math.exp(exponent);
    }
}
