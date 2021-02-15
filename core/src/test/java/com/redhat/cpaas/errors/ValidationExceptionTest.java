package com.redhat.cpaas.errors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ValidationExceptionTest
{
    @Test
    void createExceptionTest() {
        try {
            throw new ValidationException("Message with {} parameter", "argument");
        }
        catch ( ValidationException e ) {
            assertEquals( e.getMessage(), "Message with argument parameter" );
        }

        Exception cause = new Exception();
        try {
            throw new ValidationException("Message with {} {} parameters", "lots of", "argument", cause);
        }
        catch ( ValidationException e ) {
            assertEquals( e.getMessage(), "Message with lots of argument parameters" );
            assertSame( e.getCause(), cause );
        }
    }
}