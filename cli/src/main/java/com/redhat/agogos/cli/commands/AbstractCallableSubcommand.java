package com.redhat.agogos.cli.commands;

import java.util.concurrent.Callable;

public class AbstractCallableSubcommand extends AbstractSubcommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        return cli.usagex(this.getClass());
    }

}
