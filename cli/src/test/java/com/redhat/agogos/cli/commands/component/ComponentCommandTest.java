package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

@QuarkusTest
public class ComponentCommandTest extends AbstractCommandTest {

    @Test
    public void help() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/component/help.txt")));
    }
}
