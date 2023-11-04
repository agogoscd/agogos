package com.redhat.agogos.cli.commands.adm;

import static org.mockito.ArgumentMatchers.eq;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class AdmInstallCommandTest extends AdmCommandBaseTest {

    @Test
    public void installHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/adm/install-help.txt")));
    }

    @Test
    public void installDevProfile() throws Exception {
        Mockito.when(kubernetesFacadeMock.get(eq(ConfigMap.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new ConfigMapBuilder().withNewMetadata().endMetadata().build());
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "dev");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(utils.testResourceAsStringList("commands/adm/install-dev-profile.txt")));
    }

    @Test
    public void installDevProfileWithoutKnative() throws Exception {
        Mockito.when(kubernetesFacadeMock.get(eq(ConfigMap.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new ConfigMapBuilder().withNewMetadata().endMetadata().build());
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "dev", "--skip-knative");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/adm/install-dev-profile-without-knative.txt")));
    }

    @Test
    public void installDevProfileWithoutTekton() throws Exception {
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "dev", "--skip-tekton");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/adm/install-dev-profile-without-tekton.txt")));
    }

    @Test
    public void installDevProfileWithoutEither() throws Exception {
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "dev", "--skip-knative",
                "--skip-tekton");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/adm/install-dev-profile-without-either.txt")));
    }

    @Test
    public void installLocalProfile() throws Exception {
        Mockito.when(kubernetesFacadeMock.get(eq(ConfigMap.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new ConfigMapBuilder().withNewMetadata().endMetadata().build());
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "local");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/adm/install-local-profile.txt")));
    }

    @Test
    public void installLocalProfileWithoutKnative() throws Exception {
        Mockito.when(kubernetesFacadeMock.get(eq(ConfigMap.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new ConfigMapBuilder().withNewMetadata().endMetadata().build());
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "local", "--skip-knative");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/adm/install-local-profile-without-knative.txt")));
    }

    @Test
    public void installLocalProfileWithoutTekton() throws Exception {
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "local", "--skip-tekton");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/adm/install-local-profile-without-tekton.txt")));
    }

    @Test
    public void installLocalProfileWithoutEither() throws Exception {
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "local", "--skip-knative",
                "--skip-tekton");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/adm/install-local-profile-without-either.txt")));
    }

    @Test
    public void installProdProfile() throws Exception {
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "install", "--profile", "prod");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/adm/install-prod-profile.txt")));
    }
}
