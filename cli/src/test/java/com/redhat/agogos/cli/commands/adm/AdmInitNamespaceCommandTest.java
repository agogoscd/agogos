package com.redhat.agogos.cli.commands.adm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import io.fabric8.kubernetes.api.model.APIGroupList;
import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.api.model.APIResourceList;
import io.fabric8.kubernetes.api.model.APIResourceListBuilder;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

import java.io.InputStream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class AdmInitNamespaceCommandTest extends AdmCommandBaseTest {

    @Test
    public void initHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "init", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/adm/init-help.txt")));
    }

    @Test
    public void initNamespaceOnly() throws Exception {
        Mockito.when(kubernetesFacadeMock.getApiGroups())
                .thenReturn(new APIGroupList());
        Mockito.when(kubernetesFacadeMock.getApiResources(Mockito.anyString()))
                .thenReturn(new APIResourceList());
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());
        Mockito.when(kubernetesFacadeMock.update(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());
        Mockito.when(kubernetesFacadeMock.waitForEventListenerRunning(Mockito.any()))
                .thenReturn(el);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "init", "-n", "test-namespace");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/adm/init-namespace-only.txt")));
    }

    @Test
    public void initWithUsers() throws Exception {
        Mockito.when(kubernetesFacadeMock.getApiGroups())
                .thenReturn(new APIGroupList());
        Mockito.when(kubernetesFacadeMock.getApiResources(Mockito.anyString()))
                .thenReturn(new APIResourceList());
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());
        Mockito.when(kubernetesFacadeMock.update(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());
        Mockito.when(kubernetesFacadeMock.waitForEventListenerRunning(Mockito.any()))
                .thenReturn(el);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "init", "-n", "test-namespace", "--admins",
                "user1",
                "--editors", "user2", "--viewers", "user3,user4");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/adm/init-with-users.txt")));
    }

    @Test
    public void initWithQuota() throws Exception {
        Mockito.when(kubernetesFacadeMock.getApiGroups())
                .thenReturn(new APIGroupList());
        Mockito.when(kubernetesFacadeMock.getApiResources(Mockito.anyString()))
                .thenReturn(new APIResourceList());
        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());
        Mockito.when(kubernetesFacadeMock.update(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());
        Mockito.when(kubernetesFacadeMock.waitForEventListenerRunning(Mockito.any()))
                .thenReturn(el);
        Mockito.when(kubernetesFacadeMock.unmarshal(Mockito.any(), Mockito.any(InputStream.class)))
                .thenReturn(quota);

        String quotaPath = getClass().getClassLoader().getResource("commands/adm/quota.yml").getPath();
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "init", "-n", "test-namespace", "--quota-file",
                quotaPath);

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/adm/init-with-quota.txt")));
    }

    @Test
    public void initWithExtensions() throws Exception {
        APIGroupList groups = new APIGroupListBuilder()
                .addNewGroup()
                .withKind("Secret")
                .withName("secret")
                .withApiVersion("agogos.redhat.com/v1alpha1")
                .addNewVersion("agogos.redhat.com/v1alpha1", "v1alpha1")
                .endGroup()
                .build();
        APIResourceList resources = new APIResourceListBuilder()
                .addNewResource()
                .withKind("Secret")
                .withName("secret")
                .endResource()
                .build();

        Mockito.when(kubernetesFacadeMock.serverSideApply(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());
        Mockito.when(kubernetesFacadeMock.waitForEventListenerRunning(Mockito.any()))
                .thenReturn(el);
        Mockito.when(kubernetesFacadeMock.getApiGroups())
                .thenReturn(groups);
        Mockito.when(kubernetesFacadeMock.getApiResources(Mockito.anyString()))
                .thenReturn(resources);
        Mockito.when(kubernetesFacadeMock.update(Mockito.any()))
                .then(AdditionalAnswers.returnsFirstArg());
        Mockito.when(kubernetesFacadeMock.getKubernetesResources(Mockito.anyString(), eq("agogos.redhat.com/v1alpha1"),
                Mockito.anyString(),
                any(ListOptions.class)))
                .thenReturn(generics);
        Mockito.when(kubernetesFacadeMock.get(ServiceAccount.class, "test-namespace", "agogos"))
                .thenReturn(sa);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "adm", "init", "-n", "test-namespace",
                "--extensions",
                "dummy-v1");
        catcher.logStdout();
        catcher.logStderr();

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/adm/init-with-extensions.txt")));
    }
}
