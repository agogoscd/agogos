package com.redhat.agogos.cli;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@QuarkusTest
public class CLITest {

    @Test
    public void helpTest() throws Exception {
        CLI cli = new CLI();
        CommandLine cmd = new CommandLine(cli);
        cmd.execute("--help");
    }
}
