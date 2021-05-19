package com.redhat.agogos.cli;

import com.redhat.agogos.cli.commands.AdmCommand;
import com.redhat.agogos.cli.commands.BuildCommand;
import com.redhat.agogos.cli.commands.ComponentCommand;
import com.redhat.agogos.cli.commands.PipelineCommand;
import com.redhat.agogos.cli.commands.RunCommand;
import com.redhat.agogos.cli.commands.TriggerCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import java.io.PrintWriter;
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
        return run(new CommandLine(this, factory), new PrintWriter(System.out, true), new PrintWriter(System.err, true), args);
    }

    public int run(Class<? extends Runnable> command, String... args) {
        return run(new CommandLine(command, factory), new PrintWriter(System.out, true), new PrintWriter(System.err, true),
                args);
    }

    /**
     * Mostly useful in testing.
     * 
     * @param out
     * @param err
     * @param args
     * @return
     */
    public int run(PrintWriter out, PrintWriter err, String... args) {
        return run(new CommandLine(this, factory), out, err, args);
    }

    private int run(CommandLine cli, PrintWriter out, PrintWriter err, String... args) {
        cli.setCaseInsensitiveEnumValuesAllowed(true);
        cli.setOut(out);
        cli.setErr(err);

        return cli.execute(args);
    }
}
