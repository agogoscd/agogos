package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.Helper;
import com.redhat.agogos.core.AgogosEnvironment;
import com.redhat.agogos.core.KubernetesFacade;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import jakarta.inject.Inject;

public abstract class AbstractSubcommand {

    @Inject
    protected CLI cli;

    @Inject
    protected Helper helper;

    @Inject
    protected KubernetesFacade kubernetesFacade;

    @Inject
    protected AgogosEnvironment agogosEnvironment;

    @Inject
    protected KubernetesSerialization objectMapper;
}
