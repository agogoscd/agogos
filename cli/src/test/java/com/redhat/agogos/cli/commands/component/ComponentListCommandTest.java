package com.redhat.agogos.cli.commands.component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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
public class ComponentListCommandTest extends ComponentCommandBaseTest {

    @Test
    public void listHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "components", "list", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/component/list-help.txt")));
    }

    @Test
    public void list() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Component.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(components);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "components", "list");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/component/list.txt")));
    }

    @Test
    public void listWithLimit() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Component.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(components.subList(0, 3));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "components", "list", "--limit", "1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions
                .assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/component/list-with-limit.txt")));
    }
}
