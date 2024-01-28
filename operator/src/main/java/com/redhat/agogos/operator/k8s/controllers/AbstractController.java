package com.redhat.agogos.operator.k8s.controllers;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.AgogosResource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionInfoStatus;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.ResultableStatus;
import com.redhat.agogos.core.v1alpha1.Run;
import com.redhat.agogos.operator.eventing.CloudEventPublisher;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractController<T extends AgogosResource<?, ?>>
        implements Namespaced, Reconciler<T>, Cleaner<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractController.class);

    @Inject
    protected CloudEventPublisher cloudEventPublisher;

    @Inject
    protected KubernetesSerialization objectMapper;

    @Inject
    protected KubernetesFacade kubernetesFacade;

    @Override
    public UpdateControl<T> reconcile(T resource, Context<T> context) {
        LOG.info("Checking {} '{}', returning noUpdate", resource.getKind(), resource.getFullName());
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(T resource, Context<T> context) {
        LOG.info("Removing {} '{}'", resource.getKind(), resource.getFullName());
        return DeleteControl.defaultDelete();
    }

    protected AgogosResource<?, ?> parentResource(T resource, Context<T> context) {
        throw new ApplicationException("No implementation of parentResource for '{}'", resource.getKind());
    }

    protected void updateExecutionInformation(T resource, String name, ResultableStatus resourceStatus) {
        String group = resource.getMetadata().getLabels().get(Label.create(Resource.GROUP));
        if (group != null) {
            String fullName = resource.getFullName();

            // Update the Execution with the new build status.
            String instance = resource.getMetadata().getLabels().get(Label.INSTANCE.toString());
            ListOptions options = new ListOptionsBuilder()
                    .withLabelSelector(Label.RESOURCE + "=" + Resource.GROUP.toString().toLowerCase() + ","
                            + Label.NAME + "=" + group + "," + Label.INSTANCE + "=" + instance)
                    .build();
            List<Execution> executions = kubernetesFacade.list(Execution.class, resource.getMetadata().getNamespace(), options);
            if (executions.size() > 0) {
                Execution execution = executions.get(0);
                ExecutionInfoStatus info = findExecutionInfoStatus(resource, execution, resource.getMetadata().getName());
                if (info != null) {
                    ResultableStatus s = new ResultableStatus();
                    s.setCompletionTime(resourceStatus.getCompletionTime());
                    s.setLastUpdate(resourceStatus.getLastUpdate());
                    s.setStartTime(resourceStatus.getStartTime());
                    s.setStatus(resourceStatus.getStatus());

                    info.setStatus(s);
                    if (kubernetesFacade.patchStatus(execution) != null) {
                        LOG.info("Updated execution info for '{}' to {} in '{}'", fullName,
                                resourceStatus.getStatus(), execution.getFullName());
                    } else {
                        LOG.error("Unable to update execution info for '{}' to {} in '{}'", fullName,
                                resourceStatus.getStatus(), execution.getFullName());
                    }
                } else {
                    LOG.error("Unable to find ExecutionInfo for build '{}' with group '{}' and instance '{}'",
                            fullName, group, instance);
                }
            } else {
                LOG.error("Unable to find Execution for build '{}'", fullName);
            }
        }
    }

    private ExecutionInfoStatus findExecutionInfoStatus(T resource, Execution execution, String name) {
        if (resource instanceof Build) {
            return execution.getStatus().getBuilds().get(name);
        } else if (resource instanceof Group) {
            return execution.getStatus().getExecutions().get(name);
        } else if (resource instanceof Run) {
            return execution.getStatus().getRuns().get(name);
        } else {
            throw new ApplicationException("Unrecognized resource in findExecutionInfo: " + resource.getKind());
        }
    }
}
