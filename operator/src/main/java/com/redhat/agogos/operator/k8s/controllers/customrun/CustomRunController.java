package com.redhat.agogos.operator.k8s.controllers.customrun;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Dependency;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Submission;
import com.redhat.agogos.core.v1alpha1.Submission.SubmissionSpec;
import com.redhat.agogos.operator.eventing.CloudEventPublisher;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.fabric8.tekton.pipeline.v1beta1.CustomRun;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false)
public class CustomRunController implements Namespaced, Reconciler<CustomRun>, Cleaner<CustomRun> {
    /*
     * This Controller processes Agogos-specific CustomRun resources and creates
     * the correct Agogos resource.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CustomRunController.class);

    @Inject
    CloudEventPublisher publisher;

    @Inject
    KubernetesFacade kubernetesFacade;

    @Inject
    KubernetesSerialization objectMapper;

    @Override
    public UpdateControl<CustomRun> reconcile(CustomRun customRun, Context<CustomRun> context) {

        if (customRun.getMetadata().getName().startsWith("agogos-") && customRun.getSpec().getCustomSpec() != null) {
            switch (Resource.fromType(customRun.getSpec().getCustomSpec().getKind())) {
                case SUBMISSION:
                    if (customRun.getMetadata().getName().contains("-cli-")) {
                        // This is from a CLI command. A message needs to be sent for the submission
                        // so that any interceptors defined on triggers will run before the submission
                        // is processed.
                        sendExecuteCloudEvent(customRun);
                        kubernetesFacade.delete(customRun);
                    } else {
                        Submission s = new Submission();
                        s.getMetadata().setGenerateName("submission-");
                        s.getMetadata().setNamespace(customRun.getMetadata().getNamespace());
                        s.setSpec(objectMapper.unmarshal(objectMapper.asJson(customRun.getSpec().getCustomSpec().getSpec()),
                                Submission.SubmissionSpec.class));
                        kubernetesFacade.create(s);
                        kubernetesFacade.delete(customRun);
                    }
                    break;
                case DEPENDENCY:
                    Dependency d = new Dependency();
                    d.getMetadata().setGenerateName("dependency-");
                    d.getMetadata().setNamespace(customRun.getMetadata().getNamespace());
                    d.setSpec(objectMapper.unmarshal(objectMapper.asJson(customRun.getSpec().getCustomSpec().getSpec()),
                            Dependency.DependencySpec.class));
                    kubernetesFacade.create(d);
                    kubernetesFacade.delete(customRun);
                    break;
                default:
                    LOG.error("Unhandled resource '{}'", Resource.fromType(customRun.getSpec().getCustomSpec().getKind()));
            }
        }

        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(CustomRun resource, Context<CustomRun> context) {
        return DeleteControl.defaultDelete();
    }

    private void sendExecuteCloudEvent(CustomRun customRun) {
        SubmissionSpec spec = objectMapper.unmarshal(objectMapper.asJson(customRun.getSpec().getCustomSpec().getSpec()),
                Submission.SubmissionSpec.class);

        String name = customRun.getMetadata().getName();
        String ns = customRun.getMetadata().getNamespace();

        if (name.contains("-component-")) {
            publisher.publish(ns, "build", Component.class, spec);
        } else if (name.contains("-group-")) {
            publisher.publish(ns, "execution", Group.class, spec);
        } else if (name.contains("-pipeline-")) {
            publisher.publish(ns, "run", Pipeline.class, spec);
        }

    }
}
