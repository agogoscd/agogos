package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import jakarta.inject.Inject;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public abstract class AbstractCommand implements Runnable {

    @Spec
    protected CommandSpec spec;

    @Inject
    protected CLI cli;

    @Override
    public void run() {
        cli.usage(this.getClass());
    }
}
