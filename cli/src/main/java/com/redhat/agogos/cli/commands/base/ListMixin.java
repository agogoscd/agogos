package com.redhat.agogos.cli.commands.base;

import lombok.Getter;
import picocli.CommandLine.Option;

public class ListMixin {
    @Option(names = "--limit", defaultValue = "0", description = "Number of items to display, if not provided all resources will be listed")
    @Getter
    Long limit;

}
