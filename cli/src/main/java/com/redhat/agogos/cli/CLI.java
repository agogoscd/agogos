package com.redhat.agogos.cli;

import com.redhat.agogos.cli.commands.AdmCommand;
import com.redhat.agogos.cli.commands.BuildCommand;
import com.redhat.agogos.cli.commands.BuilderCommand;
import com.redhat.agogos.cli.commands.ComponentCommand;
import com.redhat.agogos.cli.commands.LoadCommand;
import com.redhat.agogos.cli.commands.PipelineCommand;
import com.redhat.agogos.cli.commands.RunCommand;
import com.redhat.agogos.cli.commands.StageCommand;
import com.redhat.agogos.cli.commands.TriggerCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javax.inject.Inject;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

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

    @Option(names = { "--output",
            "-o" }, description = "Output format, valid values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}.", defaultValue = "plain", scope = ScopeType.INHERIT)
    @Getter
    Output output;

    public void usage(Class<? extends Runnable> command) {
        new CommandLine(command, factory).usage(System.out);
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).execute(args);
    }

    public int run(Class<? extends Runnable> command, String... args) {
        return new CommandLine(command, factory).execute(args);
    }
}
