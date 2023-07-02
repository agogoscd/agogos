package com.redhat.agogos.cli.commands;

public class AbstractRunnableSubcommand extends AbstractSubcommand implements Runnable {

    @Override
    public void run() {
        cli.usage(this.getClass());
    }
}
