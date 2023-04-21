package com.redhat.agogos.k8s.controllers.trigger;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.redhat.agogos.cron.TriggerEventScheduler;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.Resource;
import com.redhat.agogos.k8s.TektonPipelineHelper;
import com.redhat.agogos.k8s.controllers.AbstractDependentResource;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.triggers.TimedTriggerEvent;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import com.redhat.agogos.v1alpha1.triggers.TriggerTarget;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.triggers.v1alpha1.TriggerBuilder;
import io.fabric8.tekton.triggers.v1alpha1.TriggerSpecBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import jakarta.inject.Inject;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TriggerDependentResource
        extends AbstractDependentResource<io.fabric8.tekton.triggers.v1alpha1.Trigger, Trigger> {

    @Inject
    TektonPipelineHelper pipelineHelper;

    @Inject
    TriggerEventScheduler scheduler;

    private static final Logger LOG = LoggerFactory.getLogger(TriggerDependentResource.class);

    public TriggerDependentResource() {
        super(io.fabric8.tekton.triggers.v1alpha1.Trigger.class);
    }

    @Override
    public io.fabric8.tekton.triggers.v1alpha1.Trigger desired(Trigger agogos, Context<Trigger> context) {
        io.fabric8.tekton.triggers.v1alpha1.Trigger trigger = new io.fabric8.tekton.triggers.v1alpha1.Trigger();
        Optional<io.fabric8.tekton.triggers.v1alpha1.Trigger> optional = context
                .getSecondaryResource(io.fabric8.tekton.triggers.v1alpha1.Trigger.class);
        if (!optional.isEmpty()) {
            trigger = optional.get();
            LOG.debug("Agogos Trigger '{}', using existing Tekton Trigger '{}'",
                    agogos.getFullName(), trigger.getFullResourceName());
        } else {
            LOG.debug("Agogos Trigger '{}', creating new Tekton Trigger", agogos.getFullName());
        }
        // Depending on the configuration of the Trigger a correct target resource is
        // created and bound with the Tekton Trigger
        TriggerSpecBuilder triggerSpecBuilder = initTriggerSpecBuilder(agogos);

        // Add required filters for events
        agogos.getSpec().getEvents().forEach(event -> {
            // For TimedTriggerEvents we need to schedule these ourselves
            // Currently Tekton Triggers do not support cron-like format
            if (event instanceof TimedTriggerEvent) {
                scheduleTimedTrigger(agogos, (TimedTriggerEvent) event);
                return;
            }

            event.toCel(agogos).forEach(expression -> {
                LOG.debug("Adding CEL interceptor: '{}' to trigger '{}'", expression, agogos.getMetadata().getName());

                triggerSpecBuilder.addNewInterceptor()
                        .withNewCel()
                        .withFilter(expression)
                        .endCel()
                        .endInterceptor();
            });
        });

        if (!triggerSpecBuilder.hasInterceptors()) {
            LOG.warn("No interceptors found for Tekton Trigger '{}', Agogos Trigger will not be created",
                    agogos.getNamespacedName());
            return null;
        }

        LOG.info("Updating '{}' Tekton Trigger", agogos.getNamespacedName());

        // Set the owner for Tekton Trigger to Agogos Trigger
        OwnerReference ownerReference = new OwnerReferenceBuilder()
                .withApiVersion(agogos.getApiVersion())
                .withKind(agogos.getKind())
                .withName(agogos.getMetadata().getName())
                .withUid(agogos.getMetadata().getUid())
                .withBlockOwnerDeletion(true)
                .withController(true)
                .build();

        TriggerBuilder triggerBuilder = new TriggerBuilder(trigger)
                .withNewMetadata()
                .withName(agogos.getMetadata().getName()).withOwnerReferences(ownerReference)
                .withNamespace(agogos.getMetadata().getNamespace())
                .endMetadata()
                .withSpec(triggerSpecBuilder.build());

        trigger = triggerBuilder.build();
        LOG.debug("New Tekton Trigger '{}' created for Agogos Trigger '{}", trigger.getMetadata().getName(),
                agogos.getFullName());
        return trigger;
    }

    /**
     * Prepares the {@link PipelineRun} resource that should be created when the
     * trigger is fired. The type of the resource depends on the provided
     * {@link TriggerTarget} object which holds
     * 
     * @param target {@link TriggerTarget} object
     * @return {@link TriggerSpecBuilder} object
     */
    private TriggerSpecBuilder initTriggerSpecBuilder(Trigger agogogs) {
        // TODO: TriggerTarget apiVersion is unused, check this
        TriggerTarget target = agogogs.getSpec().getTarget();
        PipelineRun pipelineRun = null;
        Map<String, String> labels = null;

        switch (target.getKind()) {
            case "Component":
                Component component = agogosClient.v1alpha1().components().inNamespace(agogogs.getMetadata().getNamespace())
                        .withName(target.getName()).get();

                // TODO: Move to validation admission webhook
                if (component == null) {
                    throw new ApplicationException("Component '{}' could not be found", target.getName());
                }

                pipelineRun = pipelineHelper.generate(component.getKind(), component.getMetadata().getName(),
                        component.getMetadata().getNamespace());

                labels = pipelineRun.getMetadata().getLabels();

                if (labels == null) {
                    labels = new HashMap<>();
                }

                labels.put(Resource.COMPONENT.getLabel(), component.getMetadata().getName());
                break;
            case "Pipeline":
                // TODO: Make it possible to specify namespace on the Trigger target?
                Pipeline pipeline = agogosClient.v1alpha1().pipelines().inNamespace(agogogs.getMetadata().getNamespace())
                        .withName(target.getName()).get();
                ;

                // TODO: Add to validation admission webhook
                if (pipeline == null) {
                    throw new ApplicationException("Pipeline '{}' could not be found", target.getName());
                }

                pipelineRun = pipelineHelper.generate(pipeline.getKind(), pipeline.getMetadata().getName(),
                        pipeline.getMetadata().getNamespace());

                labels = pipelineRun.getMetadata().getLabels();

                if (labels == null) {
                    labels = new HashMap<>();
                }
                labels.put(Resource.PIPELINE.getLabel(), pipeline.getMetadata().getName());
                break;

            default:
                throw new ApplicationException("Unsupported Trigger target resource type: '{}'", target.getKind());
        }

        pipelineRun.getMetadata().setLabels(labels);

        pipelineRun.getMetadata().getLabels().putIfAbsent(Resource.RESOURCE.getLabel(), target.getKind().toLowerCase());

        TriggerSpecBuilder triggerSpecBuilder = new TriggerSpecBuilder()
                .withNewTemplate()
                .withNewSpec()
                .withResourcetemplates(pipelineRun)
                .endSpec()
                .endTemplate();

        return triggerSpecBuilder;
    }

    private void scheduleTimedTrigger(Trigger agogos, TimedTriggerEvent timed) {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
        CronParser parser = new CronParser(cronDefinition);
        Cron cron;

        try {
            cron = parser.parse(timed.getCron());
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(
                    "Cron expression '{}' defined in '{}' trigger is not a valid UNIX Cron expression", timed.getCron(),
                    agogos.getNamespacedName());
        }

        CronMapper cronMapper = CronMapper.fromUnixToQuartz();

        try {
            scheduler.scheduleTimedTriggerEvent(agogos, cronMapper.map(cron).asString());
        } catch (SchedulerException e) {
            throw new ApplicationException("Could not schedule timed event trigger '{}' for '{}' trigger",
                    timed.getCron(), agogos.getNamespacedName(), e);
        }
    }
}
