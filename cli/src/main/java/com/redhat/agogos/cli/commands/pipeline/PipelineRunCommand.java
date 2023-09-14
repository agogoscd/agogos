package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.cli.commands.run.RunDescribeCommand;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Run;
import com.redhat.agogos.core.v1alpha1.Submission;
import com.redhat.agogos.core.v1alpha1.Submission.SubmissionSpec;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.tekton.pipeline.v1beta1.CustomRun;
import io.fabric8.tekton.pipeline.v1beta1.CustomRunBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Command(mixinStandardHelpOptions = true, name = "run", description = "run a pipeline")
public class PipelineRunCommand extends AbstractResourceSubcommand<Pipeline> {
    @Parameters(index = "0", description = "Name of the pipeline to run")
    String name;

    @Override
    public Integer call() {
        Pipeline pipeline = kubernetesFacade.get(Pipeline.class, kubernetesFacade.getNamespace(), name, false);
        if (pipeline == null) {
            spec.commandLine().getOut().println("⛔ Unable to find pipeline '" + name + "'");
            return ExitCode.USAGE;
        }

        String uuid = UUID.randomUUID().toString();

        SubmissionSpec spc = new SubmissionSpec();
        spc.setName(name);
        spc.setInstance(uuid);
        spc.setResource(Resource.PIPELINE);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = kubernetesFacade.unmarshal(Map.class,
                new ByteArrayInputStream(objectMapper.asJson(spc).getBytes()));

        CustomRun cr = new CustomRunBuilder()
                .withKind(HasMetadata.getKind(CustomRun.class))
                .withNewMetadata()
                .withGenerateName("agogos-submission-pipeline-cli-custom-run-")
                .endMetadata()
                .withNewSpec()
                .withNewCustomSpec()
                .withApiVersion(HasMetadata.getApiVersion(Submission.class))
                .withKind(HasMetadata.getKind(Submission.class))
                .withSpec(map)
                .endCustomSpec()
                .endSpec()
                .build();

        kubernetesFacade.create(cr);

        ListOptions options = new ListOptionsBuilder()
                .withLabelSelector(Label.NAME + "=" + name + "," + Label.INSTANCE + "=" + uuid + "," +
                        Label.RESOURCE + "=" + Resource.PIPELINE.toString().toLowerCase())
                .build();
        List<Run> runs = kubernetesFacade.list(Run.class, kubernetesFacade.getNamespace(), options, true);
        if (runs.size() > 0) {
            return cli.run(RunDescribeCommand.class, runs.get(0).getMetadata().getName());
        } else {
            spec.commandLine().getOut().println("⛔ Unable to find run with submitted UUID " + uuid + ".");
            return ExitCode.SOFTWARE;
        }
    }
}
