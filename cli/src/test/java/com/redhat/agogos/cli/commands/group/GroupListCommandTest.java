package com.redhat.agogos.cli.commands.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.redhat.agogos.core.v1alpha1.Group;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class GroupListCommandTest extends GroupCommandBaseTest {

    @Test
    public void listHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "list", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/group/list-help.txt")));
    }

    @Test
    public void list() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Group.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(groups);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "list");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/group/list.txt")));
    }

    @Test
    public void listWithLimit() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Group.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(groups.subList(0, 1));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "list", "--limit", "1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/group/list-with-limit.txt")));
    }
}
