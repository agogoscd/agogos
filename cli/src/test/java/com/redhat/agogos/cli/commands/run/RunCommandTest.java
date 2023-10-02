package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

@QuarkusTest
public class RunCommandTest extends AbstractCommandTest {

    @Test
    public void help() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "run", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/run/help.txt")));
    }
}
