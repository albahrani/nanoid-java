package de.albahrani.nanoid;

import java.security.SecureRandom;

/**
 * Tiny, secure, URL-friendly unique string ID generator for Java.
 * Generates compact, secure IDs using customizable alphabets and optimized performance (thread-local SecureRandom, byte pooling, uniform distribution).
 * @author Alexander Al-Bahrani
 * @version 1.1.0
 */
public class NanoId {
    // Default URL-friendly alphabet (A-Za-z0-9_-), optimized for compression.
    public static final String URL_ALPHABET = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLF_GQZbfghjklqvwyzrict";
    // Hexadecimal alphabet (0-9, a-f) for hex IDs.
    public static final String HEX_ALPHABET = "0123456789abcdef";
    // Base58 alphabet (no 0, O, I, l) for readability, used in crypto/URLs.
    public static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    // Numeric alphabet (0-9) for number-only IDs and PINs.
    public static final String NUMERIC_ALPHABET = "0123456789";
    // Alphanumeric alphabet (A-Z, a-z, 0-9), no special chars.
    public static final String ALPHANUMERIC_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    // Thread-local SecureRandom for performance in multi-threaded environments.
    private static final ThreadLocal<SecureRandom> THREAD_LOCAL_RANDOM = 
        ThreadLocal.withInitial(SecureRandom::new);

    /**
     * Generates a NanoId string (default 21 chars, URL alphabet).
     * @return URL-friendly unique string ID (21 chars)
     */
    public static String nanoid() {
        return nanoid(21);
    }

    /**
     * Generates a NanoId string with custom size (URL alphabet).
     * @param size Length of ID (must be positive)
     * @return URL-friendly unique string ID
     * @throws IllegalArgumentException if size is not positive
     */
    public static String nanoid(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive, got: " + size);
        }
        SecureRandom random = THREAD_LOCAL_RANDOM.get();
        char[] chars = new char[size];
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        for (int i = 0; i < size; i++) {
            int index = (bytes[i] & 0xFF) & 63;
            chars[i] = URL_ALPHABET.charAt(index);
        }
        return new String(chars);
    }

    /**
     * Generates a NanoId string with custom alphabet and size.
     * @param alphabet Custom alphabet (not null/empty)
     * @param size Length of ID (must be positive)
     * @return Unique string ID using custom alphabet
     * @throws IllegalArgumentException if alphabet is null/empty or size is not positive
     */
    public static String customNanoid(String alphabet, int size) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("Alphabet cannot be null or empty");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive, got: " + size);
        }
        int mask = (2 << (31 - Integer.numberOfLeadingZeros(alphabet.length() - 1 | 1))) - 1;
        int step = (int) Math.ceil(1.6 * mask * size / alphabet.length());
        char[] chars = new char[size];
        int filled = 0;
        SecureRandom random = THREAD_LOCAL_RANDOM.get();
        while (filled < size) {
            byte[] bytes = new byte[step];
            random.nextBytes(bytes);
            for (int i = 0; i < step && filled < size; i++) {
                int byteValue = bytes[i] & 0xFF;
                int idx = byteValue & mask;
                if (idx < alphabet.length()) {
                    chars[filled++] = alphabet.charAt(idx);
                }
            }
        }
        return new String(chars);
    }

    /**
     * Returns a Supplier for NanoId generation with custom alphabet and default size.
     * Example: var hexGen = NanoId.customAlphabet("0123456789abcdef", 16); String id = hexGen.get();
     * @param alphabet Alphabet to use
     * @param defaultSize Default size for IDs
     * @return Supplier generating IDs with specified alphabet and size
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
