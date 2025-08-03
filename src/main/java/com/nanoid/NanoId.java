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
 * @author Alexander Al-Bahrani
 * @version 1.0.0
 */
public class NanoId {
    
    /**
     * The default URL-friendly alphabet used by nanoid.
     * Contains 64 characters: A-Za-z0-9_-
     */
    public static final String URL_ALPHABET = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLF_GQZbfghjklqvwyzrict";
    
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a NanoId string with the default size of 21 characters.
     * 
     * <p>The default size of 21 was chosen to have a collision probability 
     * similar to UUID v4, but with a more compact representation.
     * 
     * @return a URL-friendly unique string ID of 21 characters
     */
    public static String nanoid() {
        return nanoid(21);
    }

    /**
     * Generates a NanoId string with the specified size.
     * 
     * @param size the length of the ID to generate (must be positive)
     * @return a URL-friendly unique string ID of the specified length
     * @throws IllegalArgumentException if size is not positive
     */
    public static String nanoid(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        
        StringBuilder id = new StringBuilder(size);
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        for (int i = 0; i < size; i++) {
            // Mask to 0-63 range, as in JS implementation
            int idx = bytes[i] & 63;
            id.append(URL_ALPHABET.charAt(idx));
        }
        return id.toString();
    }

    /**
     * Generates a NanoId string with a custom alphabet and size.
     * 
     * <p>This method allows you to use your own alphabet for ID generation.
     * The distribution will be uniform across all characters in the alphabet.
     * 
     * @param alphabet the custom alphabet to use for ID generation (must not be empty)
     * @param size the length of the ID to generate (must be positive)
     * @return a unique string ID using the custom alphabet
     * @throws IllegalArgumentException if alphabet is empty or size is not positive
     */
    public static String customNanoid(String alphabet, int size) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("Alphabet cannot be null or empty");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        
        StringBuilder id = new StringBuilder(size);
        int mask = (2 << (31 - Integer.numberOfLeadingZeros((alphabet.length() - 1) | 1))) - 1;
        int step = (int) Math.ceil((1.6 * mask * size) / alphabet.length());
        
        while (id.length() < size) {
            byte[] bytes = new byte[step];
            random.nextBytes(bytes);
            for (int i = 0; i < step && id.length() < size; i++) {
                int idx = bytes[i] & mask;
                if (idx < alphabet.length()) {
                    id.append(alphabet.charAt(idx));
                }
            }
        }
        return id.toString();
    }
}
