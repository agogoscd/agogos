package com.redhat.agogos.interceptors;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.ResultableResourceStatus;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Run;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/interceptors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class InterceptorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(InterceptorHandler.class);

    private static final String PATH_NAME = "$.['build', 'run'].metadata.name";
    private static final String PATH_GROUP_LABEL = "$.['build', 'run'].metadata.labels['" + Resource.GROUP.getResourceLabel()
            + "']";
    private static final String PATH_INSTANCE_LABEL = "$.['build', 'run'].metadata.labels['" + Resource.getInstanceLabel()
            + "']";

    @Inject
    KubernetesFacade kubernetesFacade;

    @POST
    @Path("/group-execute")
    public Response groupBuild(InterceptorRequest request) {
        InterceptorResponse response = new InterceptorResponse();

        String namespace = (String) request.getInterceptorParams().get("namespace");
        DocumentContext ctx = JsonPath.parse(request.getBody());
        String name = (String) ctx.read(PATH_GROUP_LABEL, List.class).get(0);
        String instance = (String) ctx.read(PATH_INSTANCE_LABEL, List.class).get(0);

        LOG.info("Processing 'group-execute' interceptor: ns({}), group({}), instance({})", namespace, name, instance);

        try {
            Group group = kubernetesFacade.get(Group.class, namespace, name);
            Set<String> components = group.getSpec().getComponents().stream().collect(Collectors.toSet());

            if (components.size() > 0) {
                List<Build> builds = getBuilds(namespace, name, instance);
                if (components.size() == builds.size()) {
                    String myName = (String) ctx.read(PATH_NAME, List.class).get(0);

                    // Check completion time and only succeed if this is the last build.
                    Build last = getLastBuild(builds);
                    if (!myName.equals(last.getMetadata().getName())) {
                        String nf = getNotFinishedBuilds(builds);
                        if (nf != null) {
                            setFailedResponse(response, Code.FAILED_PRECONDITION, nf);
                            LOG.info(nf);
                        }
                    }
                } else {
                    String msg = String.format("Not all builds available, %d components/%d builds",
                            components.size(), builds.size());
                    setFailedResponse(response, Code.FAILED_PRECONDITION, msg);
                    LOG.info(msg);
                }
            }

            Set<String> pipelines = group.getSpec().getPipelines().stream().collect(Collectors.toSet());
            if (response.isContinueFlag() && pipelines.size() > 0) {
                List<Run> runs = getRuns(namespace, name, instance);
                if (pipelines.size() == runs.size()) {
                    String myName = (String) ctx.read(PATH_NAME, List.class).get(0);

                    // Check completion time and only succeed if this is the last build.
                    Run last = getLastRun(runs);
                    if (!myName.equals(last.getMetadata().getName())) {
                        String nf = getNotFinishedRuns(runs);
                        if (nf != null) {
                            setFailedResponse(response, Code.FAILED_PRECONDITION, nf);
                            LOG.info(nf);
                        }
                    }
                } else {
                    String msg = String.format("Not all runs available, %d pipelines/%d run",
                            pipelines.size(), runs.size());
                    setFailedResponse(response, Code.FAILED_PRECONDITION, msg);
                    LOG.info(msg);
                }
            }
        } catch (Exception e) {
            setFailedResponse(response, Code.INTERNAL, e.getMessage());
            LOG.error("Exception processing interceptor", e);
        }

        LOG.info("Interceptor 'group-execute': ns({}), group({}), instance({}) => continue = {}",
                namespace, name, instance, response.isContinueFlag());
        return Response.ok(response).build();
    }

    private List<Build> getBuilds(String namespace, String group, String instance) {
        ListOptions options = new ListOptionsBuilder()
                .withLabelSelector(getLabelSelector(group, instance))
                .build();
        return kubernetesFacade.list(Build.class, namespace, options);
    }

    private String getNotFinishedBuilds(List<Build> builds) {
        List<String> nf = builds.stream()
                .filter(b -> ResultableResourceStatus.FINISHED != b.getStatus().getStatus())
                .map(b -> String.format("%s: %s", b.getMetadata().getName(), b.getStatus().getStatus()))
                .collect(Collectors.toList());
        if (nf.size() > 0) {
            return String.format("Unfinished builds => %s", String.join(",", nf));
        }
        return null;
    }

    private Build getLastBuild(List<Build> builds) {
        // Sort by completiom time, with no completion time items at the end.
        Comparator<Build> buildComparator = new Comparator<Build>() {
            @Override
            public int compare(Build build1, Build build2) {
                ZonedDateTime completion1 = build1.getStatus().completionTime();
                ZonedDateTime completion2 = build2.getStatus().completionTime();
                if (completion1 == null) {
                    return 1;
                } else if (completion2 == null) {
                    return -1;
                }
                return completion1.compareTo(completion2);
            }
        };
        return builds.stream().sorted(buildComparator).reduce((first, second) -> second).orElse(null);
    }

    private List<Run> getRuns(String namespace, String group, String instance) {
        ListOptions options = new ListOptionsBuilder()
                .withLabelSelector(getLabelSelector(group, instance))
                .build();
        return kubernetesFacade.list(Run.class, namespace, options);
    }

    private String getNotFinishedRuns(List<Run> runs) {
        List<String> nf = runs.stream()
                .filter(r -> ResultableResourceStatus.FINISHED != r.getStatus().getStatus())
                .map(r -> String.format("%s: %s", r.getMetadata().getName(), r.getStatus().getStatus()))
                .collect(Collectors.toList());
        if (nf.size() > 0) {
            return String.format("Unfinished runs => %s", String.join(",", nf));
        }
        return null;
    }

    private Run getLastRun(List<Run> runs) {
        // Sort by completiom time, with no completion time items at the end.
        Comparator<Run> runComparator = new Comparator<Run>() {
            @Override
            public int compare(Run run1, Run run2) {
                ZonedDateTime completion1 = run1.getStatus().completionTime();
                ZonedDateTime completion2 = run2.getStatus().completionTime();
                if (completion1 == null) {
                    return 1;
                } else if (completion2 == null) {
                    return -1;
                }
                return completion1.compareTo(completion2);
            }
        };
        return runs.stream().sorted(runComparator).reduce((first, second) -> second).orElse(null);
    }

    private String getLabelSelector(String group, String instance) {
        StringBuffer sb = new StringBuffer();
        sb.append(Resource.GROUP.getResourceLabel());
        sb.append("=");
        sb.append(group);
        sb.append(",");
        sb.append(Resource.getInstanceLabel());
        sb.append("=");
        sb.append(instance);
        return sb.toString();
    }

    private void setFailedResponse(InterceptorResponse response, Code code, String message) {
        response.getStatus().setCode(code);
        response.getStatus().setMessage(message);
        response.setContinueFlag(false);
    }
}
