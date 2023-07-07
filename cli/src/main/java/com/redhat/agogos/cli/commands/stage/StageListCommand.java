package com.redhat.agogos.cli.commands.stage;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Stage;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list stages")
public class StageListCommand extends AbstractListCommand<Stage> {
}