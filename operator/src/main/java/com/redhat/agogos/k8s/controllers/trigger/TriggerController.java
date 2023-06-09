package com.redhat.agogos.k8s.controllers.trigger;

import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.cron.TriggerEventScheduler;
import com.redhat.agogos.k8s.controllers.AbstractController;
import com.redhat.agogos.v1alpha1.Status;
import com.redhat.agogos.v1alpha1.triggers.TimedTriggerEvent;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = TriggerDependentResource.class) })
public class TriggerController extends AbstractController<Trigger> {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerController.class);

    @Inject
    TriggerEventScheduler scheduler;

    @Override
    public DeleteControl cleanup(Trigger trigger, Context<Trigger> context) {
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

        return super.cleanup(trigger, context);
    }

    @Override
    public UpdateControl<Trigger> reconcile(Trigger agogos, Context<Trigger> context) {
        Optional<io.fabric8.tekton.triggers.v1beta1.Trigger> optional = context
                .getSecondaryResource(io.fabric8.tekton.triggers.v1beta1.Trigger.class);

        Status agogosStatus = agogos.getStatus();
        if (optional.isPresent()) {
            agogosStatus.setStatus(String.valueOf(ResourceStatus.Ready));
            agogosStatus.setReason("Agogos Trigger is ready");
        } else {
            agogosStatus.setStatus(String.valueOf(ResourceStatus.Failed));
            agogosStatus.setReason("Could not create Agogos Trigger");
        }
        agogosStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        LOG.info("Set status for Trigger '{}' to {}", agogos.getFullName(), agogos.getStatus().getStatus());

        return UpdateControl.updateStatus(agogos);
    }
}
