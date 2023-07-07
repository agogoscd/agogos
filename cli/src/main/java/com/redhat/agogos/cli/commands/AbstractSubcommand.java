package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.k8s.client.AgogosClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.tekton.client.TektonClient;
import jakarta.inject.Inject;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public abstract class AbstractSubcommand {

    @Spec
    protected CommandSpec spec;

    @Inject
    protected CLI cli;

    @Inject
    protected AgogosClient agogosClient;

    @Inject
    protected KnativeClient knativeClient;

    @Inject
    protected KubernetesFacade kubernetesFacade;

    @Inject
    protected TektonClient tektonClient;
}
