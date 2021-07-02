package com.redhat.agogos.cron;

import com.redhat.agogos.k8s.client.BuildClient;
import com.redhat.agogos.k8s.client.ComponentClient;
import com.redhat.agogos.v1alpha1.Build;
import io.cloudevents.CloudEvent;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class BuildJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(BuildJob.class);

    public static final String COMPONENT_NAME = "component";
    public static final String COMPONENT_NAMESPACE = "namespace";

    @Inject
    ComponentClient componentClient;

    @Inject
    BuildClient buildClient;

    /**
     * Function to send a {@link CloudEvent} with as specified by the data available
     * in the {@link JobExecutionContext}.
     * 
     * Event will be sent to the Broker defined by the application configuration.
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String name = dataMap.getString(COMPONENT_NAME);
        String namespace = dataMap.getString(COMPONENT_NAMESPACE);

        LOG.info("Scheduling a new Build for '{}' Component from '{}' namespace", name, namespace);

        Build build = new Build();
        build.getMetadata().setGenerateName(name + "-");
        build.getMetadata().setNamespace(namespace);
        build.getSpec().setComponent(name);

        build = buildClient.create(build, namespace);

        LOG.info("Build '{}' scheduled, next run at {}", build.getFullName(),
                context.getNextFireTime());
    }
}
