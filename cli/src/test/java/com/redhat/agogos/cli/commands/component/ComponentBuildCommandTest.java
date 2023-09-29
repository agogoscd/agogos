package com.redhat.agogos.cli.commands.component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class ComponentBuildCommandTest extends ComponentCommandBaseTest {

    @Test
    public void buildHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "build", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("component/build-help.txt")));
    }

    @Test
    public void buildNoComponentSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "build");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(catcher.compareToStderr(utils.testResourceAsStringList("component/build-no-component.txt")));
    }

    @Test
    public void buildSpecificComponent() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Component.class, "namespace", "dummy-component-dep-1"))
                .thenReturn(components.get(0));
        Mockito.when(kubernetesFacadeMock.listNotEmpty(eq(Build.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(builds.subList(0, 1));
        Mockito.when(kubernetesFacadeMock.get(Build.class, "namespace", "dummy-component-dep-1-pt9ss"))
                .thenReturn(builds.get(0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "build", "dummy-component-dep-1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("component/build-specific-component.txt")));
    }

    @Test
    public void buildSpecificComponentBuildNotFound() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Component.class, "namespace", "dummy-component-dep-1"))
                .thenReturn(components.get(0));
        Mockito.when(kubernetesFacadeMock.listNotEmpty(eq(Build.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(builds.subList(0, 0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "build", "dummy-component-dep-1");

        Assertions.assertEquals(ExitCode.SOFTWARE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(utils.testResourceAsStringList("component/build-build-not-found.txt")));
    }

    @Test
    public void buildNotFoundComponent() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Component.class, "namespace", "not-found"))
                .thenReturn(null);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "build", "not-found");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(utils.testResourceAsStringList("component/build-not-found-component.txt")));
    }
}
