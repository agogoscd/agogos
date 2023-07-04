package com.redhat.agogos.cli.commands.adm.certs;

import com.redhat.agogos.errors.ApplicationException;
import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

@ApplicationScoped
public class SelfSignedCertProvider implements CertProvider {
    private static final String KEY_ALGORITHM = "RSA";
    private static final String CERT_SIGNATURE_ALGORITHM = "SHA256withRSA";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private X500Name caCn = new X500Name("CN=agogos.svc");
    private X500Name cn = new X500Name("CN=system:node:agogos-webhooks.default.svc");
    private KeyPair caKeyPair;
    private KeyPair keyPair;
    X509Certificate caCert;
    X509Certificate cert;

    @Override
    public void init() {
        caKeyPair = SelfSignedCertProvider.generateKeyPair();
        keyPair = SelfSignedCertProvider.generateKeyPair();
        caCert = SelfSignedCertProvider.generateCaCert(caCn, caKeyPair);
        cert = SelfSignedCertProvider.generateCert(caCn, cn, caKeyPair, keyPair);
    }

    @Override
    public String caBundle() {
        return Base64.getEncoder().encodeToString(SelfSignedCertProvider.toPEM(caCert).getBytes()).replace("\n", "")
                .replace("\r", "");
    }

    @Override
    public String certificate() {
        return SelfSignedCertProvider.toPEM(cert);
    }

    @Override
    public String privateKey() {
        return SelfSignedCertProvider.toPEM(keyPair.getPrivate());
    }

    public static KeyPair generateKeyPair() {
        return generateKeyPair(2048);
    }

    public static KeyPair generateKeyPair(int keySize) {
        KeyPairGenerator keyPairGenerator = null;

        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new ApplicationException("Error while generating key pair; cannot find {} algorithm",
                    KEY_ALGORITHM, e);
        }

        keyPairGenerator.initialize(keySize);

        // Generate keys and return them
        return keyPairGenerator.generateKeyPair();
    }

    public static X509Certificate generateCaCert(X500Name cn, KeyPair caKeyPair) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 0); // Today
        Date startDate = calendar.getTime();

        calendar.add(Calendar.YEAR, 10); // 10 years
        Date endDate = calendar.getTime();

        BigInteger caSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));

        ContentSigner caCertContentSigner = null;

        try {
            caCertContentSigner = new JcaContentSignerBuilder(CERT_SIGNATURE_ALGORITHM)
                    .build(caKeyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new ApplicationException("Error while signing CA certificate", e);
        }
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(cn, caSerialNum, startDate,
                endDate, cn, caKeyPair.getPublic());

        try {
            rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        } catch (CertIOException e) {
            throw new ApplicationException("Error while signing CA certificate; unable to set it as CA", e);
        }

        X509CertificateHolder rootCertHolder = rootCertBuilder.build(caCertContentSigner);
        X509Certificate rootCert = null;

        try {
            rootCert = new JcaX509CertificateConverter().getCertificate(rootCertHolder);
        } catch (CertificateException e) {
            throw new ApplicationException("Error while generating the CA certificate", e);
        }

        return rootCert;
    }

    public static X509Certificate generateCert(X500Name caCn, X500Name cn, KeyPair caKeyPair, KeyPair keyPair) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 0); // Today
        Date startDate = calendar.getTime();

        calendar.add(Calendar.YEAR, 10); // 10 years
        Date endDate = calendar.getTime();

        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(cn,
                keyPair.getPublic());
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(CERT_SIGNATURE_ALGORITHM);

        ContentSigner csrContentSigner = null;

        try {
            csrContentSigner = csrBuilder.build(caKeyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new ApplicationException("Error while signing CA certificate", e);
        }
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(caCn, issuedCertSerialNum,
                startDate, endDate, csr.getSubject(), csr.getSubjectPublicKeyInfo());

        // Add DNS name is cert is to used for SSL
        try {
            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false,
                    new DERSequence(new ASN1Encodable[] {
                            new GeneralName(GeneralName.dNSName, "agogos-webhooks.agogos.svc.cluster.local"),
                            new GeneralName(GeneralName.dNSName, "agogos-webhooks.agogos.svc"),
                            new GeneralName(GeneralName.dNSName, "host.minikube.internal") }));
        } catch (CertIOException e) {
            throw new ApplicationException("Error while generating the certificate", e);
        }

        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);
        X509Certificate issuedCert = null;

        try {
            issuedCert = new JcaX509CertificateConverter()
                    .getCertificate(issuedCertHolder);
        } catch (CertificateException e) {
            throw new ApplicationException("Error while generating the certificate", e);
        }

        return issuedCert;
    }

    /**
     * Converts provided object into a PEM format.
     * 
     * @param o
     * @return
     */
    public static String toPEM(Object o) {
        final StringWriter stringWriter = new StringWriter();
        final JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);

        try {
            pemWriter.writeObject(o);
            pemWriter.flush();
            pemWriter.close();
        } catch (IOException e) {
            throw new ApplicationException("Cannot convert to PEM");
        }

        return stringWriter.toString();
    }

}
