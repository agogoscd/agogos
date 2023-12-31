package com.redhat.agogos.operator.k8s.controllers.trigger;

import com.redhat.agogos.core.ResourceStatus;
import com.redhat.agogos.core.v1alpha1.Status;
import com.redhat.agogos.core.v1alpha1.triggers.TimedTriggerEvent;
import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import com.redhat.agogos.operator.cron.TriggerEventScheduler;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
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
                    LOG.error("Could not remove timed event trigger '{}'", trigger.getFullName(), e);
                }
            }
        });

        return super.cleanup(trigger, context);
    }

    @Override
    public UpdateControl<Trigger> reconcile(Trigger agogos, Context<Trigger> context) {
        Status agogosStatus = agogos.getStatus();
        if (agogosStatus.getStatus() != ResourceStatus.READY) {
            agogosStatus.setStatus(ResourceStatus.READY);
            agogosStatus.setReason("Agogos Trigger is ready");
            agogosStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
            LOG.info("Set status for Trigger '{}' to {}", agogos.getFullName(), agogos.getStatus().getStatus());
            return UpdateControl.updateStatus(agogos);
        }

        return UpdateControl.noUpdate();
    }
}
