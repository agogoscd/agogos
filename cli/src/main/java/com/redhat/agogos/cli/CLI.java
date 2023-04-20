package com.redhat.agogos.cli;

import com.redhat.agogos.cli.commands.adm.AdmCommand;
import com.redhat.agogos.cli.commands.build.BuildCommand;
import com.redhat.agogos.cli.commands.builder.BuilderCommand;
import com.redhat.agogos.cli.commands.component.ComponentCommand;
import com.redhat.agogos.cli.commands.pipeline.PipelineCommand;
import com.redhat.agogos.cli.commands.load.LoadCommand;
import com.redhat.agogos.cli.commands.run.RunCommand;
import com.redhat.agogos.cli.commands.stage.StageCommand;
import com.redhat.agogos.cli.commands.trigger.TriggerCommand;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

import javax.inject.Inject;

import java.io.PrintWriter;

@QuarkusMain
@CommandLine.Command(name = "agogosctl", mixinStandardHelpOptions = true, subcommands = { //
        AdmCommand.class,
        BuildCommand.class,
        ComponentCommand.class,
        PipelineCommand.class,
        RunCommand.class,
        TriggerCommand.class,
        StageCommand.class,
        BuilderCommand.class,
        LoadCommand.class,
})
public class CLI implements QuarkusApplication {
    @Inject
    CommandLine.IFactory factory;

    public static enum Output {
        plain,
        yaml,
        json
    }

    @Option(names = { "-v", "--verbose" }, scope = ScopeType.INHERIT)
    boolean verbose = false;

    @Option(names = { "--output",
            "-o" }, description = "Output format, valid values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}.", defaultValue = "plain", scope = ScopeType.INHERIT)
    @Getter
    Output output;

    @Getter
    CommandLine commandLine;

    public void usage(Class<? extends Runnable> command) {
        new CommandLine(command, factory).usage(System.out);
    }

    @Override
    public int run(String... args) throws Exception {
        commandLine = new CommandLine(this, factory);
        return run(null, null, commandLine, args);
    }

    public int run(Class<? extends Runnable> command, String... args) {
        commandLine = new CommandLine(command, factory);
        return run(null, null, commandLine, args);
    }

    public int run(PrintWriter out, PrintWriter err, String... args) {
        commandLine = new CommandLine(this, factory);
        return run(out, err, commandLine, args);
    }

    private int run(PrintWriter out, PrintWriter err, CommandLine cl, String... args) {
        if (out != null) {
            cl.setOut(out);
        }
        if (err != null) {
            cl.setErr(err);
        }
        return cl.setExecutionExceptionHandler(new ExceptionHandler()).execute(args);
    }
}