package com.redhat.agogos.cli.commands;

import com.redhat.agogos.KubernetesFacade;
import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.k8s.client.AgogosClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import jakarta.inject.Inject;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public abstract class AbstractCommand implements Runnable {

    @Spec
    protected CommandSpec spec;

    @Inject
    protected CLI cli;

    @Inject
    protected AgogosClient agogosClient;

    @Inject
    protected KubernetesClient kubernetesClient;

    @Inject
    protected TektonClient tektonClient;

    @Inject
    protected KnativeClient knativeClient;

    @Inject
    protected KubernetesFacade kubernetesFacade;

    @Override
    public void run() {
        cli.usage(this.getClass());
    }
}
