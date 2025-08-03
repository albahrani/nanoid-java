package de.albahrani.nanoid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static de.albahrani.nanoid.NanoIdTestUtils.*;
import java.util.*;

/**
 * Core functionality tests for NanoId focusing on ID generation, custom alphabets, and basic validation.
 * @author Alexander Al-Bahrani
 */
public class NanoIdFunctionalityTest {

    @BeforeEach
    public void setUp() {
        performMemoryCleanup();
    }

    @Nested
    class DefaultNanoIdTests {
        
        @Test
        void generatesDefaultNanoId() {
            String id = NanoId.nanoid();
            assertValidDefaultNanoId(id);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 10, 32, 100})
        void generatesCustomSizeNanoId(int size) {
            String id = NanoId.nanoid(size);
            assertValidNanoId(id, size);
        }

        @Test
        void throwsOnInvalidSize() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(0));
        }

        @Test
        void throwsOnNegativeSize() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.nanoid(-1));
        }
    }

    @Nested
    class CustomAlphabetTests {
        
        @Test
        void generatesWithSimpleCustomAlphabet() {
            String id = NanoId.customNanoid(SIMPLE_ALPHABET, 10);
            assertValidCustomNanoId(id, 10, SIMPLE_ALPHABET);
        }

        @Test
        void generatesHexNanoId() {
            String id = NanoId.customNanoid(HEX_ALPHABET, 16);
            assertValidCustomNanoId(id, 16, HEX_ALPHABET);
            assertTrue(id.matches("^[0-9a-f]+$"), "ID should be valid hex");
        }

        @Test
        void generatesWithLargeCustomAlphabet() {
            String id = NanoId.customNanoid(LARGE_ALPHABET, 50);
            assertValidCustomNanoId(id, 50, LARGE_ALPHABET);
        }

        @Test
        void throwsOnNullAlphabet() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid(null, 10));
        }

        @Test
        void throwsOnEmptyAlphabet() {
            assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("", 10));
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 0})
        void throwsOnInvalidSize(int invalidSize) {
            assertThrows(IllegalArgumentException.class, () -> NanoId.customNanoid("abc", invalidSize));
        }
    }
    
    @Nested
    class UniquenessTests {
        
        @Test
        void ensuresUniquenessAcrossLargeSet() {
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < LARGE_ID_COUNT; i++) {
                String id = NanoId.nanoid();
                assertTrue(ids.add(id), "All IDs should be unique");
            }
            assertEquals(LARGE_ID_COUNT, ids.size(), 
                "Should have generated exactly " + LARGE_ID_COUNT + " unique IDs");
        }
        
        @Test
        void ensuresNoCollisionsInReasonableSet() {
            Set<String> used = new HashSet<>();
            for (int i = 0; i < LEGACY_NO_COLLISION_COUNT; i++) {
                String id = NanoId.nanoid();
                assertFalse(used.contains(id), "ID should be unique: " + id);
                used.add(id);
            }
        }
    }

    @Nested
    class AlphabetValidationTests {
        
        @Test
        void generatesUrlFriendlyIds() {
            for (int i = 0; i < 100; i++) {
                String id = NanoId.nanoid();
                assertValidDefaultNanoId(id);
                assertAlphabetCompliance(id, NanoId.getDefaultAlphabet());
            }
        }

        @Test
        void alphabetHasNoDuplicates() {
            String alphabet = NanoId.getDefaultAlphabet();
            for (int i = 0; i < alphabet.length(); i++) {
                assertEquals(i, alphabet.lastIndexOf(alphabet.charAt(i)),
                    "Character '" + alphabet.charAt(i) + "' should appear only once in alphabet");
            }
        }

        @Test
        void customAlphabetWithSingleCharacter() {
            String nanoidA = NanoId.customNanoid("a", 10);
            assertEquals("aaaaaaaaaa", nanoidA, "Single character alphabet should repeat the character");
        }
    }

    @Nested
    class CustomGeneratorTests {
        
        @Test
        void customAlphabetGeneratorProducesValidIds() {
            var hexGenerator = NanoId.customAlphabet(HEX_ALPHABET, 8);
            
            String id1 = hexGenerator.get();
            String id2 = hexGenerator.get();
            
            assertValidCustomNanoId(id1, 8, HEX_ALPHABET);
            assertValidCustomNanoId(id2, 8, HEX_ALPHABET);
            assertNotEquals(id1, id2, "Generated IDs should be different");
            assertTrue(id1.matches("^[0-9a-f]+$"), "First ID should be valid hex");
            assertTrue(id2.matches("^[0-9a-f]+$"), "Second ID should be valid hex");
        }
    }

    @Nested
    class DefaultAlphabetTests {
        
        @Test
        void getDefaultAlphabetReturnsCorrectValue() {
            String alphabet = NanoId.getDefaultAlphabet();
            assertNotNull(alphabet, "Default alphabet should not be null");
            assertEquals(URL_ALPHABET, alphabet, "Default alphabet should match URL_ALPHABET");
            assertEquals(ALPHABET_SIZE, alphabet.length(), 
                "Default alphabet should have " + ALPHABET_SIZE + " characters");
        }
    }
    
    @Nested
    class SizeVariationTests {
        
        @Test
        void changesIdLength() {
            assertEquals(10, NanoId.nanoid(10).length(), "ID should have specified length");
        }
        
        @ParameterizedTest
        @ValueSource(ints = {1, 5, 10, 21, 32, 64, 100})
        void generatesIdsWithVariousLengths(int length) {
            String id = NanoId.nanoid(length);
            assertValidNanoId(id, length);
        }
    }
}
