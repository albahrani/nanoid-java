package de.albahrani.nanoid;

import java.security.SecureRandom;

/**
 * Internal utility class for providing thread-local SecureRandom instances.
 * This class manages SecureRandom instances for optimal performance in multi-threaded environments.
 * 
 * @author Alexander Al-Bahrani
 * @since 1.1.0
 */
final class SecureRandomProvider {

    /**
     * Thread-local SecureRandom for performance in multi-threaded environments.
     * Each thread gets its own SecureRandom instance to avoid contention.
     */
    private static final ThreadLocal<SecureRandom> THREAD_LOCAL_RANDOM = 
        ThreadLocal.withInitial(SecureRandom::new);

    /**
     * Private constructor to prevent instantiation.
     */
    private SecureRandomProvider() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Gets the SecureRandom instance for the current thread.
     * 
     * @return SecureRandom instance for the current thread
     */
    static SecureRandom get() {
        return THREAD_LOCAL_RANDOM.get();
    }
}
