package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import javax.inject.Inject;

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
