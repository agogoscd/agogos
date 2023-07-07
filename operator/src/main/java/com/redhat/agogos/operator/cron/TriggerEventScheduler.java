package com.redhat.agogos.operator.cron;

import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Set;

/**
 * Responsible for scheduling sending a {@link io.cloudevents.CloudEvent} used
 * to invoke a Tekton {@link io.fabric8.tekton.triggers.v1beta1.Trigger}.
 * 
 * A Quartz {@link Scheduler} is used as the implementation.
 */
@ApplicationScoped
public class TriggerEventScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(TriggerEventScheduler.class);

    @Inject
    Scheduler quartz;

    /**
     * Schedules a CloudEvent responsible for directly activating a Tekton Trigger
     * so it can be executed.
     * 
     * @param trigger
     * @param cron
     * @throws SchedulerException
     */
    public void scheduleTimedTriggerEvent(Trigger trigger, String cron) throws SchedulerException {
        LOG.info("Scheduling new trigger for {} '{}'", trigger.getSpec().getTarget().getKind(),
                trigger.getSpec().getTarget().getName());

        JobDetail quartzJob = null;

        switch (trigger.getSpec().getTarget().getKind()) {
            case "Pipeline":
                quartzJob = JobBuilder.newJob(RunJob.class)
                        .withIdentity(trigger.getMetadata().getName(), trigger.getMetadata().getNamespace()) //
                        .usingJobData(RunJob.PIPELINE_NAME, trigger.getSpec().getTarget().getName()) //
                        .usingJobData(RunJob.PIPELINE_NAMESPACE, trigger.getMetadata().getNamespace()) //
                        .build();
                break;
            case "Component":
                quartzJob = JobBuilder.newJob(BuildJob.class)
                        .withIdentity(trigger.getMetadata().getName(), trigger.getMetadata().getNamespace()) //
                        .usingJobData(BuildJob.COMPONENT_NAME, trigger.getSpec().getTarget().getName()) //
                        .usingJobData(BuildJob.COMPONENT_NAMESPACE, trigger.getMetadata().getNamespace()) //
                        .build();
                break;

            default:
                throw new ApplicationException("Unsupported Trigger target: '{}'", trigger.getSpec().getTarget().getKind());
        }

        CronTrigger quartzTrigger = TriggerBuilder.newTrigger() //
                .withIdentity(trigger.getMetadata().getName(), trigger.getMetadata().getNamespace()) //
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)) //
                .startNow() //
                .build();

        LOG.info("Scheduling timed job for '{}' trigger and '{}' schedule, next scheduled run around {}",
                trigger.getFullName(), cron, quartzTrigger.getFireTimeAfter(new Date()));

        quartz.scheduleJob(quartzJob, Set.of(quartzTrigger), true);
    }

    public void removeTimedTriggerEvent(Trigger trigger) throws SchedulerException {
        LOG.info("Removing timed job '{}'", trigger.getFullName());

        quartz.deleteJob(new JobKey(trigger.getMetadata().getName(), trigger.getMetadata().getNamespace()));
    }
}
