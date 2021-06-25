package com.redhat.agogos.cli.test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class to deal with test resources.
 */
public class ResourceUtils {

    /**
     * <p>
     * Returns test resource as {@link String}.
     * </p>
     * 
     * @param path
     * @return
     */
    public static String testResourceAsString(String path) {
        try {
            return new String(ResourceUtils.testResourceAsInputStream(path).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Returns test resource as {@link InputStream}.
     * </p>
     * 
     * @param path
     * @return
     */
    public static InputStream testResourceAsInputStream(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }
}
