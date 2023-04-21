package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.v1alpha1.Run;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list runs")
public class RunListCommand extends AbstractListCommand<Run> {
}