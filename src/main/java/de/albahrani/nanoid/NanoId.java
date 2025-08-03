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
     * Default URL-friendly alphabet (A-Za-z0-9_-), optimized for compression.
     * Use for standard NanoId generation.
     */
    public static final String URL_ALPHABET = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLF_GQZbfghjklqvwyzrict";
    /**
     * Hexadecimal alphabet (0-9, a-f) for hex IDs.
     * Use for hexadecimal NanoIds.
     */
    public static final String HEX_ALPHABET = "0123456789abcdef";
    /**
     * Base58 alphabet (no 0, O, I, l) for readability, used in crypto/URLs.
     * Use for Base58 NanoIds.
     */
    public static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    /**
     * Numeric alphabet (0-9) for number-only IDs and PINs.
     * Use for numeric NanoIds.
     */
    public static final String NUMERIC_ALPHABET = "0123456789";
    /**
     * Alphanumeric alphabet (A-Z, a-z, 0-9), no special chars.
     * Use for alphanumeric NanoIds.
     */
    public static final String ALPHANUMERIC_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    /**
     * Thread-local SecureRandom for performance in multi-threaded environments.
     * Used internally for random number generation.
     */
    private static final ThreadLocal<SecureRandom> THREAD_LOCAL_RANDOM = 
        ThreadLocal.withInitial(SecureRandom::new);
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
     * @param size Length of ID (must be positive)
     * @return URL-friendly unique string ID
     * @throws IllegalArgumentException if size is not positive
     */
    public static String nanoid(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("NanoId: size must be positive, got: " + size);
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
        validateAlphabetAndSize(alphabet, size);
        // Calculate bitmask for uniform distribution: ensures all indices are equally likely
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
     * Example: var hexGen = NanoId.customAlphabet(HEX_ALPHABET, HEX_ID_LENGTH); String id = hexGen.get();
     * Example: var base58Gen = NanoId.customAlphabet(BASE58_ALPHABET, BASE58_ID_LENGTH); String id = base58Gen.get();
     * Example: var shortGen = NanoId.customAlphabet(URL_ALPHABET, SHORT_ID_LENGTH); String id = shortGen.get();
     * @param alphabet Alphabet to use
     * @param defaultSize Default size for IDs
     * @return Supplier generating IDs with specified alphabet and size
     */
    public static Supplier<String> customAlphabet(String alphabet, int defaultSize) {
        validateAlphabetAndSize(alphabet, defaultSize);
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
        if (alphabetSize <= 0) {
            throw new IllegalArgumentException("NanoId: alphabetSize must be positive, got: " + alphabetSize);
        }
        if (idLength <= 0) {
            throw new IllegalArgumentException("NanoId: idLength must be positive, got: " + idLength);
        }
        if (numIds <= 0) {
            throw new IllegalArgumentException("NanoId: numIds must be positive, got: " + numIds);
        }
        double totalPossibleIds = Math.pow(alphabetSize, idLength);
        // Birthday paradox approximation: P ≈ 1 - e^(-n²/(2N))
        // where n = number of IDs, N = total possible IDs
        double exponent = -(numIds * numIds) / (2.0 * totalPossibleIds);
        return 1.0 - Math.exp(exponent);
    }

    /**
     * Generates a hexadecimal NanoId (16 chars, HEX_ALPHABET).
     * @return Hexadecimal unique string ID
     */
    public static String nanoidHex() {
        return customNanoid(HEX_ALPHABET, HEX_ID_LENGTH);
    }

    /**
     * Generates a Base58 NanoId (22 chars, BASE58_ALPHABET).
     * @return Base58 unique string ID
     */
    public static String nanoidBase58() {
        return customNanoid(BASE58_ALPHABET, BASE58_ID_LENGTH);
    }

    /**
     * Generates a short NanoId (8 chars, URL_ALPHABET).
     * @return Short unique string ID
     */
    public static String nanoidShort() {
        return customNanoid(URL_ALPHABET, SHORT_ID_LENGTH);
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
     */
    public static java.util.Map<Character, Integer> testDistribution(String alphabet, int idLength, int sampleSize) {
        validateAlphabetAndSize(alphabet, idLength);
        java.util.Map<Character, Integer> freq = new java.util.HashMap<>();
        for (int i = 0; i < sampleSize; i++) {
            String id = customNanoid(alphabet, idLength);
            for (char c : id.toCharArray()) {
                freq.put(c, freq.getOrDefault(c, 0) + 1);
            }
        }
        return freq;
    }

    /**
     * Validates the alphabet and size parameters for NanoId generation.
     * Throws IllegalArgumentException if invalid.
     * Ensures alphabet is not null/empty, has no duplicates, and size is positive.
     *
     * @param alphabet The alphabet to validate
     * @param size The size to validate
     */
    private static void validateAlphabetAndSize(String alphabet, int size) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("NanoId: alphabet cannot be null or empty");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("NanoId: size must be positive, got: " + size);
        }
        // Check for duplicate characters in the alphabet
        java.util.Set<Character> seen = new java.util.HashSet<>();
        for (char c : alphabet.toCharArray()) {
            if (!seen.add(c)) {
                throw new IllegalArgumentException("NanoId: alphabet contains duplicate character '" + c + "'");
            }
        }
    }
}
