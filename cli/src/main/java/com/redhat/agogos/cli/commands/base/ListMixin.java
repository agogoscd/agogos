package com.redhat.agogos.cli.commands.base;

import lombok.Getter;
import picocli.CommandLine.Option;

public class ListMixin {
    @Option(names = "--limit", defaultValue = "5", description = "Number of items to display, default: ${DEFAULT-VALUE}")
    @Getter
    Long limit;

}
