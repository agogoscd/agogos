package com.redhat.agogos.cli.commands.build;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Build;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list builds")
public class BuildListCommand extends AbstractListCommand<Build> {
}