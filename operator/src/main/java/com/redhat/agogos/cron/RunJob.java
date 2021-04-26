package com.redhat.agogos.cron;

import com.redhat.agogos.k8s.client.PipelineClient;
import com.redhat.agogos.k8s.client.RunClient;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import io.cloudevents.CloudEvent;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RunJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(RunJob.class);

    public static final String PIPELINE_NAME = "pipeline";
    public static final String PIPELINE_NAMESPACE = "namespace";

    @Inject
    PipelineClient pipelineClient;

    @Inject
    RunClient runClient;

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

        LOG.info("Scheduling a new Run for '{}' Pipeline from '{}' namespace", name, namespace);

        Pipeline pipeline = pipelineClient.getByName(name, namespace);

        OwnerReference ownerReference = new OwnerReferenceBuilder() //
                .withApiVersion(pipeline.getApiVersion()) //
                .withKind(pipeline.getKind()) //
                .withName(pipeline.getMetadata().getName()) //
                .withUid(pipeline.getMetadata().getUid()) //
                .withBlockOwnerDeletion(true) //
                .withController(true) //
                .build();

        Run run = new Run();
        run.getMetadata().setGenerateName(name + "-");
        run.getSpec().setPipeline(name);
        run.getMetadata().getOwnerReferences().add(ownerReference);

        run = runClient.create(run, namespace);

        LOG.info("Run '{}' scheduled, next run at {}", run.getFullName(),
                context.getNextFireTime());
    }
}
