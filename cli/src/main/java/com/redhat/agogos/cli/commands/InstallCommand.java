package com.redhat.agogos.cli.commands;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.redhat.agogos.errors.ApplicationException;

import org.apache.commons.lang3.StringUtils;
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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhook;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfigurationBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.RuleWithOperations;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.RuleWithOperationsBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhook;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfigurationBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true, name = "install", description = "Install Agogos")
public class InstallCommand implements Runnable {

    private static final String BC_PROVIDER = "BC";
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    public static enum Profile {
        DEV
    }

    @Inject
    KubernetesClient kubernetesClient;

    @Option(names = { "--profile",
            "-p" }, description = "Selected installation profile, valid values: ${COMPLETION-CANDIDATES}")
    Profile profile = Profile.DEV;

    @Override
    public void run() {
        System.out.println(String.format("Selected profile: %s", profile));

        try {
            switch (profile) {
                case DEV:
                    installDev();
                    break;

                default:
                    break;
            }
        } catch (ApplicationException e) {
            System.err.println(String.format("An error occurred: %s", e.getMessage()));
            //System.exit(-1);
        }

    }

    private void installAgogos() {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("deployment/agogos.yaml");

        String resource = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        loadKubernetesResources(new ByteArrayInputStream(resource.getBytes(StandardCharsets.UTF_8)), "agogos");
    }

    private void installDev() {
        installTekton("v0.22.0");
        installTektonTriggers("v0.12.1");
        installKnativeEventing("v0.21.3");
        installAgogos();
        generateCertificates();

        System.out.println("Restarting Webhooks service");
        kubernetesClient.apps().deployments().inNamespace("agogos").withName("agogos-webhooks").rolling().restart();

    }

    private String runCommand(String... command) {
        return runCommand(new File(System.getProperty("user.home")), command);
    }

    private String runCommand(File directory, String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder().directory(directory).command(command);
        StringBuilder stringBuilder = new StringBuilder();

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                stringBuilder.append(line);
                stringBuilder.append(System.getProperty("line.separator"));
            }

            return stringBuilder.toString();
        } catch (IOException e) {
            throw new ApplicationException("Command '{}' failed", command, e);
        }
    }

    private void generateCertificates() {
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyPairGenerator = null;

        try {
            keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER);
        } catch (NoSuchAlgorithmException e) {
            throw new ApplicationException("Error while generating key pair; cannot find {} algorithm", KEY_ALGORITHM);
        } catch (NoSuchProviderException e) {
            throw new ApplicationException("Error while generating key pair; cannot find BouncyCastle provider", e);
        }

        keyPairGenerator.initialize(2048);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 0); // Today
        Date startDate = calendar.getTime();

        calendar.add(Calendar.YEAR, 10); // 10 years
        Date endDate = calendar.getTime();

        // Generate keys for CA
        KeyPair caKeyPair = keyPairGenerator.generateKeyPair();

        BigInteger caSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));

        X500Name caCertIssuer = new X500Name("CN=agogos.svc");
        X500Name caCertSubject = caCertIssuer;

        ContentSigner caCertContentSigner = null;

        try {
            caCertContentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER)
                    .build(caKeyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new ApplicationException("Error while signing CA certificate", e);
        }
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(caCertIssuer, caSerialNum, startDate,
                endDate, caCertSubject, caKeyPair.getPublic());

        try {
            rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        } catch (CertIOException e) {
            throw new ApplicationException("Error while signing CA certificate; unable to set it as CA", e);
        }

        X509CertificateHolder rootCertHolder = rootCertBuilder.build(caCertContentSigner);
        X509Certificate rootCert = null;

        try {
            rootCert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER)
                    .getCertificate(rootCertHolder);
        } catch (CertificateException e) {
            throw new ApplicationException("Error while generating the CA certificate", e);
        }

        X500Name issuedCertSubject = new X500Name("CN=system:node:agogos-webhooks.default.svc");
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));
        KeyPair issuedCertKeyPair = keyPairGenerator.generateKeyPair();

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject,
                issuedCertKeyPair.getPublic());
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER);

        ContentSigner csrContentSigner = null;

        try {
            csrContentSigner = csrBuilder.build(caKeyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new ApplicationException("Error while signing CA certificate", e);
        }
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(caCertIssuer, issuedCertSerialNum,
                startDate, endDate, csr.getSubject(), csr.getSubjectPublicKeyInfo());

        // Add DNS name is cert is to used for SSL
        try {
            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                    new GeneralName(GeneralName.dNSName, "agogos-webhooks.agogos.svc.cluster.local"),
                    new GeneralName(GeneralName.dNSName, "agogos-webhooks.agogos.svc")
            }));
        } catch (CertIOException e) {
            throw new ApplicationException("Error while generating the certificate", e);
        }

        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);
        X509Certificate issuedCert = null;

        try {
            issuedCert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER)
                    .getCertificate(issuedCertHolder);
        } catch (CertificateException e) {
            throw new ApplicationException("Error while generating the certificate", e);
        }

        Map<String, String> a = new HashMap<>();
        a.put("tls.crt", Base64.getEncoder().encodeToString(toPEM(issuedCert).getBytes()));
        a.put("tls.key", Base64.getEncoder().encodeToString(toPEM(issuedCertKeyPair.getPrivate()).getBytes()));

        Secret s = new SecretBuilder().withNewMetadata().withName("webhooks-cert").endMetadata()
                .withNewType("kubernetes.io/tls")
                .withData(a).build();

        kubernetesClient.secrets().inNamespace("agogos").createOrReplace(s);

        String caBundle = Base64.getEncoder().encodeToString(toPEM(rootCert).getBytes()).replace("\n", "").replace("\r", "");

        RuleWithOperations validationRules = new RuleWithOperationsBuilder().withOperations("CREATE", "UPDATE")
                .withApiGroups("agogos.redhat.com").withApiVersions("v1alpha1").withResources("builds", "components")
                .withScope("*").build();

        RuleWithOperations mutationRules = new RuleWithOperationsBuilder().withOperations("CREATE")
                .withApiGroups("agogos.redhat.com").withApiVersions("v1alpha1").withResources("builds", "runs").withScope("*")
                .build();

        ValidatingWebhook validatingWebhook = new ValidatingWebhookBuilder().withNewName("validate.webhook.agogos.redhat.com")
                .withNewSideEffects("None").withNewFailurePolicy("Fail").withAdmissionReviewVersions("v1").withNewClientConfig()
                .withNewService().withNamespace("agogos").withName("agogos-webhooks").withPath("/webhooks/validate")
                .withPort(443)
                .endService().withCaBundle(caBundle).endClientConfig().withRules(validationRules).build();

        ValidatingWebhookConfiguration validatingWebhookConfiguration = new ValidatingWebhookConfigurationBuilder()
                .withNewMetadata().withName("webhook.agogos.redhat.com").endMetadata().withWebhooks(validatingWebhook).build();

        kubernetesClient.admissionRegistration().v1()
                .validatingWebhookConfigurations().createOrReplace(validatingWebhookConfiguration);

        MutatingWebhook mutatingWebhook = new MutatingWebhookBuilder().withNewName("mutate.webhook.agogos.redhat.com")
                .withNewSideEffects("None").withNewFailurePolicy("Fail").withAdmissionReviewVersions("v1").withNewClientConfig()
                .withNewService().withNamespace("agogos").withName("agogos-webhooks").withPath("/webhooks/mutate")
                .withPort(443)
                .endService().withCaBundle(caBundle).endClientConfig().withRules(mutationRules).build();

        MutatingWebhookConfiguration mutatingWebhookConfiguration = new MutatingWebhookConfigurationBuilder()
                .withNewMetadata().withName("webhook.agogos.redhat.com").endMetadata().withWebhooks(mutatingWebhook).build();

        kubernetesClient.admissionRegistration().v1()
                .mutatingWebhookConfigurations().createOrReplace(mutatingWebhookConfiguration);

    }

    /**
     * Converts provided object into a PEM format.
     * 
     * @param o
     * @return
     */
    private String toPEM(Object o) {
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

    private void installTekton(String version) {
        System.out.println(String.format("Installing Tekton %s", version));

        String url = String.format("https://storage.googleapis.com/tekton-releases/pipeline/previous/%s/release.yaml", version);

        loadKubernetesResources(urlToStream(url), "tekton-pipelines");

        System.out.println(String.format("Tekton %s installed", version));
    }

    private void installTektonTriggers(String version) {
        System.out.println(String.format("Installing Tekton Triggers %s", version));

        String url = String.format("https://storage.googleapis.com/tekton-releases/triggers/previous/%s/release.yaml", version);

        loadKubernetesResources(urlToStream(url), "tekton-pipelines");

        System.out.println(String.format("Tekton Triggers %s installed", version));
    }

    private void installKnativeEventing(String version) {
        System.out.println(String.format("Installing Knative Eventing %s", version));
        String namespace = "knative-eventing";

        String[] files = new String[] { "eventing-crds.yaml", "eventing-core.yaml", "in-memory-channel.yaml",
                "mt-channel-broker.yaml" };

        for (String name : files) {
            loadKubernetesResources(urlToStream(
                    String.format("https://github.com/knative/eventing/releases/download/%s/%s", version, name)),
                    namespace);
        }

        System.out.println(String.format("Knative Eventing %s installed", version));
    }

    private InputStream urlToStream(String url) {
        URL resource = null;

        try {
            resource = new URL(url);
        } catch (MalformedURLException e) {
            throw new ApplicationException("Could not parse resource url: {}", url, e);
        }

        try {
            return resource.openStream();
        } catch (IOException e) {
            throw new ApplicationException("Could not load resource from url: {}", url, e);
        }
    }

    private List<HasMetadata> loadKubernetesResources(InputStream stream, String namespace) {
        List<HasMetadata> k8sResources = kubernetesClient.load(stream).inNamespace(namespace)
                .createOrReplace();

        k8sResources.forEach(o -> {
            String prefix = HasMetadata.getGroup(o.getClass());

            if (StringUtils.isEmpty(prefix)) {
                prefix = o.getKind().toLowerCase();
            }

            System.out.println(String.format("  -> %s/%s", prefix, o.getMetadata().getName()));
        });

        return k8sResources;
    }
}
