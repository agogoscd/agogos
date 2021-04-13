package com.redhat.agogos.cron;

import com.redhat.agogos.k8s.client.RunClient;
import com.redhat.agogos.v1alpha1.Run;
import io.cloudevents.CloudEvent;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PipelineRunJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunJob.class);

    public static final String PIPELINE_NAME = "pipeline";
    public static final String PIPELINE_NAMESPACE = "namespace";

    @Inject
    RunClient pipelineRunClient;

    /**
     * Function to send a {@link CloudEvent} with as specified by the data available
     * in the {@link JobExecutionContext}.
     * 
     * Event will be sent to the Broker defined by the application configuration.
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String name = dataMap.getString(PIPELINE_NAME);
        String namespace = dataMap.getString(PIPELINE_NAMESPACE);

        LOG.info("Scheduling a new PipelineRun for '{}' Pipeline from '{}' namespace", name, namespace);

        Run pipelineRun = pipelineRunClient.create(name, namespace);

        LOG.info("PipelineRun '{}' scheduled, next run at {}", pipelineRun.getMetadata().getName(),
                context.getNextFireTime());
    }
}
