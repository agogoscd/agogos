package com.redhat.agogos.cli.commands.trigger;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list triggers")
class TriggerListCommand extends AbstractListCommand<Trigger> {
}