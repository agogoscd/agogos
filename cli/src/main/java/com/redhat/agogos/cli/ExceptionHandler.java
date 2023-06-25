package com.redhat.agogos.cli;

import io.fabric8.kubernetes.client.KubernetesClientException;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

import java.io.PrintWriter;

public class ExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
        CLI cli = (CLI) cmd.getCommandSpec().root().userObject();
        PrintWriter err = cmd.getErr();

        StringBuilder msg = new StringBuilder()
                .append("⛔ Oops, an error occurred!")
                .append(System.getProperty("line.separator"));

        if (ex instanceof KubernetesClientException && ((KubernetesClientException) ex).getStatus() != null) {
            msg.append(((KubernetesClientException) ex).getStatus().getMessage());
        } else {
            msg.append(ex.getMessage());
        }

        err.println();
        err.println(cmd.getColorScheme().errorText(msg.toString()));

        if (cli.verbose) {
            err.println();
            err.println("Stacktrace:");
            err.println();
            ex.printStackTrace(err);
        }

        return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : cmd.getCommandSpec().exitCodeOnExecutionException();
    }
}
