package com.redhat.agogos.cli.commands.base;

import javax.inject.Inject;

import com.redhat.agogos.cli.CLI;

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
