package com.redhat.agogos.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ValidationExceptionTest {
    @Test
    void createExceptionTest() {
        try {
            throw new ValidationException("Message with {} parameter", "argument");
        } catch (ValidationException e) {
            assertEquals(e.getMessage(), "Message with argument parameter");
        }

        Exception cause = new Exception();
        try {
            throw new ValidationException("Message with {} {} parameters", "lots of", "argument", cause);
        } catch (ValidationException e) {
            assertEquals(e.getMessage(), "Message with lots of argument parameters");
            assertSame(e.getCause(), cause);
        }
    }
}
