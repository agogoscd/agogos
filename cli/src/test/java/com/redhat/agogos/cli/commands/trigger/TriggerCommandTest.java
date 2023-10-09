package com.redhat.agogos.cli.commands.trigger;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

@QuarkusTest
public class TriggerCommandTest extends AbstractCommandTest {

    @Test
    public void help() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "trigger", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/trigger/help.txt")));
    }
}
