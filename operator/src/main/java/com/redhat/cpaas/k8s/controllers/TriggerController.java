package com.redhat.cpaas.k8s.controllers;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.redhat.agogos.v1alpha1.triggers.TimedTriggerEvent;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import com.redhat.agogos.v1alpha1.triggers.TriggerTarget;
import com.redhat.cpaas.cron.TriggerEventScheduler;
import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.k8s.ResourceLabels;
import com.redhat.cpaas.k8s.TektonPipelineHelper;
import com.redhat.cpaas.k8s.client.ComponentResourceClient;
import com.redhat.cpaas.k8s.client.PipelineClient;
import com.redhat.cpaas.v1alpha1.ComponentResource;
import com.redhat.cpaas.v1alpha1.PipelineResource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.triggers.v1alpha1.TriggerBuilder;
import io.fabric8.tekton.triggers.v1alpha1.TriggerSpecBuilder;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class TriggerController implements ResourceController<Trigger> {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerController.class);

    @Inject
    TektonClient tektonClient;

    @Inject
    TektonPipelineHelper pipelineHelper;

    @Inject
    ComponentResourceClient componentClient;

    @Inject
    TriggerEventScheduler scheduler;

    @Inject
    PipelineClient pipelineClient;

    @Override
    public DeleteControl deleteResource(Trigger trigger, Context<Trigger> context) {
        trigger.getSpec().getEvents().forEach(event -> {
            // For TimedTriggerEvents we need to schedule these ourselves
            // Currently Tekton Triggers do not support cron-like format
            if (event instanceof TimedTriggerEvent) {
                try {
                    scheduler.removeTimedTriggerEvent(trigger);
                } catch (SchedulerException e) {
                    LOG.error("Could not remove timed event trigger '{}'", trigger.getNamespacedName(), e);
                }
            }
        });

        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<Trigger> createOrUpdateResource(Trigger trigger, Context<Trigger> context) {
        LOG.info("Trigger '{}' modified", trigger.getMetadata().getName());

        updateTrigger(trigger);

        // TODO: Update status subresource afterwards
        return UpdateControl.noUpdate();
    }

    /**
     * Prepares the {@link PipelineRun} resource that should be created when the
     * trigger is fired. The type of the resource depends on the provided
     * {@link TriggerTarget} object which holds
     * 
     * @param target {@link TriggerTarget} object
     * @return {@link TriggerSpecBuilder} object
     */
    private TriggerSpecBuilder initTriggerSpecBuilder(Trigger trigger) {
        // TODO: TriggerTarget apiVersion is unused, check this
        TriggerTarget target = trigger.getSpec().getTarget();
        PipelineRun pipelineRun = null;

        switch (target.getKind()) {
            case "Component":
                ComponentResource component = componentClient.getByName(target.getName());

                // TODO: Move to validation admission webhook
                if (component == null) {
                    throw new ApplicationException("Component '{}' could not be found", target.getName());
                }

                pipelineRun = pipelineHelper.generate(component.getKind(), component.getMetadata().getName(),
                        component.getMetadata().getNamespace());

                Map<String, String> labels = pipelineRun.getMetadata().getLabels();

                if (labels == null) {
                    labels = new HashMap<>();
                }

                labels.put(ResourceLabels.COMPONENT.getValue(), component.getMetadata().getName());

                pipelineRun.getMetadata().setLabels(labels);
                break;
            case "Pipeline":
                // TODO: Make it possible to specify namespace on the Trigger target?
                PipelineResource pipeline = pipelineClient.getByName(target.getName(),
                        trigger.getMetadata().getNamespace());

                // TODO: Add to validation admission webhook
                if (pipeline == null) {
                    throw new ApplicationException("Pipeline '{}' could not be found", target.getName());
                }

                pipelineRun = pipelineHelper.generate(pipeline.getKind(), pipeline.getMetadata().getName(),
                        pipeline.getMetadata().getNamespace());
                break;

            default:
                throw new ApplicationException("Unsupported Trigger target resource type: '{}'", target.getKind());
        }

        pipelineRun.getMetadata().getLabels().putIfAbsent(ResourceLabels.RESOURCE.getValue(), target.getKind().toLowerCase());

        TriggerSpecBuilder triggerSpecBuilder = new TriggerSpecBuilder() //
                .withNewTemplate() //
                .withNewSpec() //
                .withResourcetemplates(pipelineRun) //
                .endSpec() //
                .endTemplate();

        return triggerSpecBuilder;
    }

    /**
     * Based on the Agogos Trigger resource create required Tekton Triggers
     * resources.
     * 
     * @param trigger Agogos Trigger instance
     */
    private void updateTrigger(Trigger trigger) {
        // Depending on the configuration of the Trigger a correct target resource is
        // created and bound with the Tekton Trigger
        TriggerSpecBuilder triggerSpecBuilder = initTriggerSpecBuilder(trigger);

        // Add required filters for events
        trigger.getSpec().getEvents().forEach(event -> {
            // For TimedTriggerEvents we need to schedule these ourselves
            // Currently Tekton Triggers do not support cron-like format
            if (event instanceof TimedTriggerEvent) {
                scheduleTimedTrigger(trigger, (TimedTriggerEvent) event);
                return;
            }

            event.toCel(trigger).forEach(expression -> {
                LOG.debug("Adding CEL interceptor: '{}' to trigger '{}'", expression, trigger.getMetadata().getName());

                triggerSpecBuilder.addNewInterceptor() //
                        .withNewCel() //
                        .withFilter(expression) //
                        .endCel() //
                        .endInterceptor();
            });
        });

        if (!triggerSpecBuilder.hasInterceptors()) {
            LOG.warn("No interceptors found for '{}' Tekton Trigger, Trigger will not be created",
                    trigger.getNamespacedName());
            return;
        }

        LOG.info("Updating '{}' Tekton Trigger", trigger.getNamespacedName());

        // Set the owner for Tekton Trigger to Agogos Trigger
        OwnerReference ownerReference = new OwnerReferenceBuilder() //
                .withApiVersion(trigger.getApiVersion()) //
                .withKind(trigger.getKind()) //
                .withName(trigger.getMetadata().getName()) //
                .withUid(trigger.getMetadata().getUid()) //
                .withBlockOwnerDeletion(true) //
                .withController(true) //
                .build();

        TriggerBuilder triggerBuilder = new TriggerBuilder() //
                .withNewMetadata() //
                .withName(trigger.getMetadata().getName()).withOwnerReferences(ownerReference) //
                .endMetadata() //
                .withSpec(triggerSpecBuilder.build());

        // Create the Tekton Trigger
        tektonClient.v1alpha1().triggers().createOrReplace(triggerBuilder.build());
    }

    private void scheduleTimedTrigger(Trigger trigger, TimedTriggerEvent timed) {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
        CronParser parser = new CronParser(cronDefinition);
        Cron cron;

        try {
            cron = parser.parse(timed.getCron());
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(
                    "Cron expression '{}' defined in '{}' trigger is not a valid UNIX Cron expression", timed.getCron(),
                    trigger.getNamespacedName());
        }

        CronMapper cronMapper = CronMapper.fromUnixToQuartz();

        try {
            scheduler.scheduleTimedTriggerEvent(trigger, cronMapper.map(cron).asString());
        } catch (SchedulerException e) {
            throw new ApplicationException("Could not schedule timed event trigger '{}' for '{}' trigger",
                    timed.getCron(), trigger.getNamespacedName(), e);
        }
    }
}
