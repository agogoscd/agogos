package com.redhat.agogos.cli;

import com.redhat.agogos.cli.commands.adm.AdmCommand;
import com.redhat.agogos.cli.commands.build.BuildCommand;
import com.redhat.agogos.cli.commands.builder.BuilderCommand;
import com.redhat.agogos.cli.commands.component.ComponentCommand;
import com.redhat.agogos.cli.commands.execution.ExecutionCommand;
import com.redhat.agogos.cli.commands.group.GroupCommand;
import com.redhat.agogos.cli.commands.info.InfoCommand;
import com.redhat.agogos.cli.commands.pipeline.PipelineCommand;
import com.redhat.agogos.cli.commands.run.RunCommand;
import com.redhat.agogos.cli.commands.stage.StageCommand;
import com.redhat.agogos.cli.commands.trigger.TriggerCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

@QuarkusMain
@CommandLine.Command(name = "agogosctl", mixinStandardHelpOptions = true)
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

    public static enum Profile {
        admin,
        user
    }

    @Setter
    @ConfigProperty(name = "agogos.cli-profile", defaultValue = "user")
    Profile profile;

    @Option(names = { "-v", "--verbose" }, scope = ScopeType.INHERIT)
    boolean verbose = false;

    @Option(names = { "--output",
            "-o" }, description = "Output format, valid values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}.", defaultValue = "plain", scope = ScopeType.INHERIT)
    @Getter
    Output output;

    @Getter
    CommandLine commandLine;

    public Integer usage(Class<? extends Callable<Integer>> command) {
        initCommandLine(command, factory).usage(spec.commandLine().getOut());
        return CommandLine.ExitCode.OK;
    }

    @Override
    public int run(String... args) throws Exception {
        commandLine = initCommandLine(factory);
        return run(null, null, commandLine, args);
    }

    public int run(PrintWriter out, PrintWriter err, Class<? extends Callable<Integer>> command, String... args) {
        commandLine = initCommandLine(command, factory);
        return run(out, err, commandLine, args);
    }

    public int run(Class<? extends Callable<Integer>> command, String... args) {
        commandLine = initCommandLine(command, factory);
        return run(null, null, commandLine, args);
    }

    public int run(PrintWriter out, PrintWriter err, String... args) {
        commandLine = initCommandLine(factory);
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

    private CommandLine initCommandLine(Class<? extends Callable<Integer>> command, IFactory factory) {
        commandLine = new CommandLine(command == null ? this : command, factory);

        if (profile == Profile.admin) {
            commandLine.addSubcommand(AdmCommand.class);
        }

        commandLine.addSubcommand(BuildCommand.class)
                .addSubcommand(BuilderCommand.class)
                .addSubcommand(ComponentCommand.class)
                .addSubcommand(ExecutionCommand.class)
                .addSubcommand(GroupCommand.class)
                .addSubcommand(InfoCommand.class)
                .addSubcommand(PipelineCommand.class)
                .addSubcommand(RunCommand.class)
                .addSubcommand(StageCommand.class)
                .addSubcommand(TriggerCommand.class);

        return commandLine;
    }

    private CommandLine initCommandLine(IFactory factory) {
        return initCommandLine(null, factory);
    }
}
