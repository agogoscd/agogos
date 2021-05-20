package com.redhat.agogos.cli.commands.adm.certs;

import java.util.Base64;

public interface CertProvider {
    public void init();

    /**
     * <p>
     * Returns a Base64 encoded single-line string with the CA certificate content.
     * </p>
     */
    public String caBundle();

    /**
     * <p>
     * Returns certificate as a string in PEM format.
     * </p>
     * 
     */
    public String certificate();

    /**
     * <p>
     * Returns private key as a string in PEM format.
     * </p>
     * 
     */
    public String privateKey();

    /**
     * Encodes provided data into Base64 string.
     * 
     * @param data
     * @return
     */
    public static String toBase64(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }
}
