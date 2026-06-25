package com.mycompany.oop.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    @Test
    void hashProducesStableSha256Value() {
        String firstHash = PasswordUtil.hash("motorph123");
        String secondHash = PasswordUtil.hash("motorph123");

        assertEquals(64, firstHash.length());
        assertEquals(firstHash, secondHash);
        assertNotEquals("motorph123", firstHash);
    }

    @Test
    void verifyAcceptsMatchingPasswordOnly() {
        String hash = PasswordUtil.hash("secret");

        assertTrue(PasswordUtil.verify("secret", hash));
        assertFalse(PasswordUtil.verify("wrong", hash));
        assertFalse(PasswordUtil.verify(null, hash));
        assertFalse(PasswordUtil.verify("secret", null));
    }
}
