package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.core.v1alpha1.Component;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class ComponentDescribeCommandTest extends ComponentCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("component/describe-help.txt")));
    }

    @Test
    public void describeNoComponentSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(catcher.compareToStderr(utils.testResourceAsStringList("component/describe-no-component.txt")));
    }

    @Test
    public void describeSpecificComponent() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Component.class, "namespace", "dummy-component-group-1-4dvh6"))
                .thenReturn(components.get(0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "describe", "dummy-component-group-1-4dvh6");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("component/describe-specific-component.txt")));
    }
}
