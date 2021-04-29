package com.redhat.agogos.cli;

import javax.inject.Inject;

import com.redhat.agogos.cli.commands.BuildCommand;
import com.redhat.agogos.cli.commands.ComponentCommand;
import com.redhat.agogos.cli.commands.InstallCommand;
import com.redhat.agogos.cli.commands.PipelineCommand;
import com.redhat.agogos.cli.commands.RunCommand;
import com.redhat.agogos.cli.commands.TriggerCommand;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@QuarkusMain
@CommandLine.Command(name = "agogosctl", mixinStandardHelpOptions = true, subcommands = { //
        InstallCommand.class,
        BuildCommand.class,
        ComponentCommand.class,
        PipelineCommand.class,
        RunCommand.class,
        TriggerCommand.class,
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

    public void run(Class<? extends Runnable> command, String... args) {
        new CommandLine(command, factory).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).setCaseInsensitiveEnumValuesAllowed(true).execute(args);
    }
}
