package com.redhat.agogos.test;

import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Group;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import com.redhat.agogos.v1alpha1.SourceHandler;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

/**
 * <p>
 * Special initialization of the mock server that registers CRDs within the cluster so that Operator can be started.
 * </p>
 */
public class CRDTestServerSetup extends KubernetesTestServerSetup {

    @SuppressWarnings("rawtypes")
    @Override
    public void accept(KubernetesServer server) {
        super.accept(server);

        Class<? extends CustomResource>[] classes = new Class[] { Build.class, Component.class, Run.class, Pipeline.class,
                Group.class, Trigger.class, SourceHandler.class };

        for (Class<? extends CustomResource> clazz : classes) {
            server.getClient().apiextensions().v1().customResourceDefinitions()
                    .create(CustomResourceDefinitionContext.v1CRDFromCustomResourceType(clazz).build());
        }
    }
}
