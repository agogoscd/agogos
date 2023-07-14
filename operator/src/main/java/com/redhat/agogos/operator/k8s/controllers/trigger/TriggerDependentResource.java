package com.redhat.agogos.operator.k8s.controllers.trigger;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.triggers.TimedTriggerEvent;
import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import com.redhat.agogos.core.v1alpha1.triggers.TriggerTarget;
import com.redhat.agogos.operator.cron.TriggerEventScheduler;
import com.redhat.agogos.operator.k8s.controllers.AbstractDependentResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.pipeline.v1beta1.CustomRun;
import io.fabric8.tekton.pipeline.v1beta1.CustomRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.CustomRunSpec;
import io.fabric8.tekton.pipeline.v1beta1.CustomRunSpecBuilder;
import io.fabric8.tekton.triggers.v1beta1.TriggerBuilder;
import io.fabric8.tekton.triggers.v1beta1.TriggerSpecBinding;
import io.fabric8.tekton.triggers.v1beta1.TriggerSpecBindingBuilder;
import io.fabric8.tekton.triggers.v1beta1.TriggerSpecBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import jakarta.inject.Inject;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TriggerDependentResource
        extends AbstractDependentResource<io.fabric8.tekton.triggers.v1beta1.Trigger, Trigger> {

    private static String AGOGOS_CUSTOM_RUN_PREFIX = "agogos-trigger-custom-run-";
    public static String AGOGOS_CUSTOM_RUN_LABEL = Resource.AGOGOS_LABEL_PREFIX + "triggered-customrun";

    @Inject
    TriggerEventScheduler scheduler;

    private static final Logger LOG = LoggerFactory.getLogger(TriggerDependentResource.class);

    public TriggerDependentResource() {
        super(io.fabric8.tekton.triggers.v1beta1.Trigger.class);
    }

    @Override
    public io.fabric8.tekton.triggers.v1beta1.Trigger desired(Trigger agogos, Context<Trigger> context) {
        io.fabric8.tekton.triggers.v1beta1.Trigger trigger = new io.fabric8.tekton.triggers.v1beta1.Trigger();
        Optional<io.fabric8.tekton.triggers.v1beta1.Trigger> optional = context
                .getSecondaryResource(io.fabric8.tekton.triggers.v1beta1.Trigger.class);

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

        TriggerSpecBinding binding = new TriggerSpecBindingBuilder()
                .withName("instance")
                .withValue("$(body.build.metadata.labels['" + escapeLabel(Resource.getInstanceLabel()) + "'])")
                .build();

        triggerSpecBuilder.addToBindings(binding);

        // Add required filters for events
        agogos.getSpec().getEvents().forEach(event -> {
            // For TimedTriggerEvents we need to schedule these ourselves
            // Currently Tekton Triggers do not support cron-like format
            if (event instanceof TimedTriggerEvent) {
                scheduleTimedTrigger(agogos, (TimedTriggerEvent) event);
                return;
            }

            event.interceptors(agogos).forEach(interceptor -> {
                LOG.debug("Adding interceptor: '{}' to trigger '{}'", interceptor.getName(), agogos.getMetadata().getName());

                triggerSpecBuilder.addToInterceptors(interceptor);
            });
        });

        if (!triggerSpecBuilder.hasInterceptors()) {
            LOG.warn("No interceptors found for Tekton Trigger '{}', Agogos Trigger will not be created",
                    agogos.getFullName());
            return null;
        }

        LOG.info("Updating '{}' Tekton Trigger", agogos.getFullName());

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
     * Prepares the {@link CustomRun} resource that should be created when the
     * trigger is fired.
     * 
     * @return {@link TriggerSpecBuilder} object
     */
    private TriggerSpecBuilder initTriggerSpecBuilder(Trigger agogos) {
        // TODO: TriggerTarget apiVersion is unused, check this
        TriggerTarget target = agogos.getSpec().getTarget();

        CustomRunSpec customSpec = new CustomRunSpecBuilder()
                .withNewCustomSpec()
                .withApiVersion(HasMetadata.getApiVersion(Build.class)) // Agogos API version.
                .withKind(target.getKind())
                .addToSpec("name", target.getName())
                .endCustomSpec()
                .build();

        CustomRun customRun = new CustomRunBuilder()
                .withApiVersion(HasMetadata.getApiVersion(CustomRun.class))
                .withKind(HasMetadata.getKind(CustomRun.class))
                .withNewMetadata()
                .withGenerateName(AGOGOS_CUSTOM_RUN_PREFIX)
                .addToLabels(Resource.RESOURCE.getResourceLabel(), target.getKind().toLowerCase())
                .addToLabels(AGOGOS_CUSTOM_RUN_LABEL, Boolean.TRUE.toString().toLowerCase())
                .addToLabels(Resource.getInstanceLabel(), "$(tt.params.instance)")
                .endMetadata()
                .withSpec(customSpec)
                .build();

        TriggerSpecBuilder triggerSpecBuilder = new TriggerSpecBuilder()
                .withNewTemplate()
                .withNewSpec()
                .addNewParam("mydefault", "Agogos instance", "instance")
                .withResourcetemplates(customRun)
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
                    agogos.getFullName());
        }

        CronMapper cronMapper = CronMapper.fromUnixToQuartz();

        try {
            scheduler.scheduleTimedTriggerEvent(agogos, cronMapper.map(cron).asString());
        } catch (SchedulerException e) {
            throw new ApplicationException("Could not schedule timed event trigger '{}' for '{}' trigger",
                    timed.getCron(), agogos.getFullName(), e);
        }
    }

    private String escapeLabel(String label) {
        // Strings in this part of the trigger template need to escape dots and slashes.
        return label.replaceAll("/", "\\\\/").replaceAll("\\.", "\\\\.");
    }
}
