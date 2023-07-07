package com.redhat.agogos.cli.commands.builder;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Builder;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list builders")
public class BuilderListCommand extends AbstractListCommand<Builder> {
}