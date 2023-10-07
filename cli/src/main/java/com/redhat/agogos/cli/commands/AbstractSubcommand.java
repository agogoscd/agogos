package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.core.AgogosEnvironment;
import com.redhat.agogos.core.KubernetesFacade;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import jakarta.inject.Inject;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public abstract class AbstractSubcommand {

    @Spec
    protected CommandSpec spec;

    @Inject
    protected CLI cli;

    @Inject
    protected KubernetesFacade kubernetesFacade;

    @Inject
    protected AgogosEnvironment agogosEnvironment;

    @Inject
    protected KubernetesSerialization objectMapper;
}
