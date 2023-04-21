package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.v1alpha1.Component;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list components")
public class ComponentListCommand extends AbstractListCommand<Component> {
}
