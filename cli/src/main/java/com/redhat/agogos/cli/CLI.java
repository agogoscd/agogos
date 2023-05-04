package com.redhat.agogos.cli;

import com.redhat.agogos.cli.commands.adm.AdmCommand;
import com.redhat.agogos.cli.commands.build.BuildCommand;
import com.redhat.agogos.cli.commands.builder.BuilderCommand;
import com.redhat.agogos.cli.commands.component.ComponentCommand;
import com.redhat.agogos.cli.commands.info.InfoCommand;
import com.redhat.agogos.cli.commands.load.LoadCommand;
import com.redhat.agogos.cli.commands.pipeline.PipelineCommand;
import com.redhat.agogos.cli.commands.run.RunCommand;
import com.redhat.agogos.cli.commands.stage.StageCommand;
import com.redhat.agogos.cli.commands.trigger.TriggerCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;

@QuarkusMain
@CommandLine.Command(name = "agogosctl", mixinStandardHelpOptions = true, subcommands = { //
        AdmCommand.class,
        BuilderCommand.class,
        BuildCommand.class,
        ComponentCommand.class,
        InfoCommand.class,
        LoadCommand.class,
        PipelineCommand.class,
        RunCommand.class,
        StageCommand.class,
        TriggerCommand.class,
})
public class CLI implements QuarkusApplication {

    @Spec
    protected CommandSpec spec;

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
        new CommandLine(command, factory).usage(spec.commandLine().getOut());
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