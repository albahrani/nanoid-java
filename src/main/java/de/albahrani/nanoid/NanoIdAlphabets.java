package de.albahrani.nanoid;

/**
 * Predefined alphabets for NanoId generation.
 * This class contains commonly used character sets for different ID formats.
 * 
 * @author Alexander Al-Bahrani
 * @since 1.1.0
 */
public final class NanoIdAlphabets {
    
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
     * Private constructor to prevent instantiation.
     */
    private NanoIdAlphabets() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
