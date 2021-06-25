package com.redhat.agogos.cli;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public class ExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
        CLI cli = (CLI) cmd.getCommandSpec().root().userObject();

        cmd.getErr().println();
        cmd.getErr().println(cmd.getColorScheme().errorText("🛑 Ooops, an error occurred!"));
        cmd.getErr().println();
        cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));

        if (cli.verbose) {
            cmd.getErr().println();
            cmd.getErr().println("Stacktrace:");
            cmd.getErr().println();
            ex.printStackTrace(cmd.getErr());
        }

        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : cmd.getCommandSpec().exitCodeOnExecutionException();
    }

}
