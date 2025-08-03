package de.albahrani.nanoid;

import java.security.SecureRandom;
import java.util.function.Supplier;

/**
 * Tiny, secure, URL-friendly unique string ID generator for Java.
 * Generates compact, secure IDs using customizable alphabets and optimized performance (thread-local SecureRandom, byte pooling, uniform distribution).
 * @author Alexander Al-Bahrani
 * @version 1.1.0
 */
public class NanoId {
    /**
     * Default NanoId length (21).
     * Used for standard NanoId generation.
     */
    public static final int DEFAULT_ID_LENGTH = 21;
    /**
     * Common hexadecimal NanoId length (16).
     */
    public static final int HEX_ID_LENGTH = 16;
    /**
     * Common Base58 NanoId length (22).
     */
    public static final int BASE58_ID_LENGTH = 22;
    /**
     * Common short NanoId length (8).
     */
    public static final int SHORT_ID_LENGTH = 8;

    /**
     * Generates a NanoId string (default 21 chars, URL alphabet).
     * @return URL-friendly unique string ID (21 chars)
     */
    public static String nanoid() {
        return nanoid(DEFAULT_ID_LENGTH);
    }

    /**
     * Generates a NanoId string with custom size (URL alphabet).
     * Uses optimized algorithm for the 64-character URL alphabet with direct byte mapping.
     * 
     * @param size Length of ID (must be positive)
     * @return URL-friendly unique string ID
     * @throws IllegalArgumentException if size is not positive
     */
    public static String nanoid(int size) {
        NanoIdValidator.validateSize(size);
        SecureRandom random = SecureRandomProvider.get();
        char[] chars = new char[size];
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        
        // Optimized for 64-character alphabet: each byte maps to exactly one character
        // (bytes[i] & 0xFF) converts signed byte to unsigned int (0-255)
        // & 63 masks to 6 bits (0-63), perfectly matching our 64-character alphabet
        for (int i = 0; i < size; i++) {
            int index = (bytes[i] & 0xFF) & 63; // Extract 6 bits for 64-char alphabet
            chars[i] = NanoIdAlphabets.URL_ALPHABET.charAt(index);
        }
        return new String(chars);
    }

    /**
     * Generates a NanoId string with custom alphabet and size.
     * Uses uniform distribution algorithm to ensure fair character selection.
     * 
     * Algorithm explanation:
     * 1. Calculate bitmask to extract uniform random bits
     * 2. Generate bytes in batches to minimize SecureRandom calls
     * 3. Use rejection sampling to ensure uniform distribution
     * 
     * @param alphabet Custom alphabet (not null/empty)
     * @param size Length of ID (must be positive)
     * @return Unique string ID using custom alphabet
     * @throws IllegalArgumentException if alphabet is null/empty or size is not positive
     */
    public static String customNanoid(String alphabet, int size) {
        NanoIdValidator.validateAlphabetAndSize(alphabet, size);
        
        // Calculate bitmask for uniform distribution
        // Find the smallest power of 2 that's >= alphabet.length()
        // This ensures we can extract uniform random indices
        int mask = (2 << (31 - Integer.numberOfLeadingZeros(alphabet.length() - 1 | 1))) - 1;
        
        // Estimate bytes needed: 1.6 is a safety factor to reduce rejection probability
        // Higher values = fewer rejections but more random bytes generated
        int step = (int) Math.ceil(1.6 * mask * size / alphabet.length());
        
        char[] chars = new char[size];
        int filled = 0;
        SecureRandom random = SecureRandomProvider.get();
        
        // Rejection sampling loop: generate bytes until we have enough valid indices
        while (filled < size) {
            byte[] bytes = new byte[step];
            random.nextBytes(bytes);
            
            for (int i = 0; i < step && filled < size; i++) {
                int byteValue = bytes[i] & 0xFF; // Convert to unsigned
                int idx = byteValue & mask;      // Extract masked bits
                
                // Reject if index >= alphabet.length (ensures uniform distribution)
                if (idx < alphabet.length()) {
                    chars[filled++] = alphabet.charAt(idx);
                }
            }
        }
        return new String(chars);
    }

    /**
     * Returns a Supplier for NanoId generation with custom alphabet and default size.
     * Example: var hexGen = NanoId.customAlphabet(HEX_ALPHABET, HEX_ID_LENGTH); String id = hexGen.get();
     * Example: var base58Gen = NanoId.customAlphabet(BASE58_ALPHABET, BASE58_ID_LENGTH); String id = base58Gen.get();
     * Example: var shortGen = NanoId.customAlphabet(URL_ALPHABET, SHORT_ID_LENGTH); String id = shortGen.get();
     * @param alphabet Alphabet to use
     * @param defaultSize Default size for IDs
     * @return Supplier generating IDs with specified alphabet and size
     */
    public static Supplier<String> customAlphabet(String alphabet, int defaultSize) {
        NanoIdValidator.validateAlphabetAndSize(alphabet, defaultSize);
        return () -> customNanoid(alphabet, defaultSize);
    }
    
    /**
     * Gets the default alphabet used by nanoid.
     * This is provided for compatibility and debugging purposes.
     * 
     * @return the default URL-friendly alphabet
     */
    public static String getDefaultAlphabet() {
        return NanoIdAlphabets.URL_ALPHABET;
    }
    
    /**
     * Generates a hexadecimal NanoId (16 chars, HEX_ALPHABET).
     * @return Hexadecimal unique string ID
     */
    public static String nanoidHex() {
        return customNanoid(NanoIdAlphabets.HEX_ALPHABET, HEX_ID_LENGTH);
    }

    /**
     * Generates a Base58 NanoId (22 chars, BASE58_ALPHABET).
     * @return Base58 unique string ID
     */
    public static String nanoidBase58() {
        return customNanoid(NanoIdAlphabets.BASE58_ALPHABET, BASE58_ID_LENGTH);
    }

    /**
     * Generates a short NanoId (8 chars, URL_ALPHABET).
     * @return Short unique string ID
     */
    public static String nanoidShort() {
        return customNanoid(NanoIdAlphabets.URL_ALPHABET, SHORT_ID_LENGTH);
    }

    // End of class
}
