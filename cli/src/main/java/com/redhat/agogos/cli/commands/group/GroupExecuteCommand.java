package com.redhat.agogos.cli.commands.group;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.cli.commands.execution.ExecutionDescribeCommand;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Group;
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

@Command(mixinStandardHelpOptions = true, name = "execute", aliases = { "ex", "e" }, description = "execute a group")
public class GroupExecuteCommand extends AbstractResourceSubcommand<Group> {
    @Parameters(index = "0", description = "Name of the group to execute.")
    String name;

    @Override
    public Integer call() {
        Group group = kubernetesFacade.get(Group.class, kubernetesFacade.getNamespace(), name);
        if (group == null) {
            spec.commandLine().getErr().println("⛔ Unable to find group '" + name + "'");
            return ExitCode.USAGE;
        }

        String uuid = UUID.randomUUID().toString();

        SubmissionSpec spc = new SubmissionSpec();
        spc.setName(name);
        spc.setInstance(uuid);
        spc.setResource(Resource.GROUP);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = kubernetesFacade.unmarshal(Map.class,
                new ByteArrayInputStream(objectMapper.asJson(spc).getBytes()));

        CustomRun cr = new CustomRunBuilder()
                .withKind(HasMetadata.getKind(CustomRun.class))
                .withNewMetadata()
                .withGenerateName("agogos-submission-group-cli-custom-run-")
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
                        Label.RESOURCE + "=" + Resource.GROUP.toString().toLowerCase())
                .build();
        List<Execution> executions = kubernetesFacade.listNotEmpty(Execution.class, kubernetesFacade.getNamespace(), options);
        if (executions.size() > 0) {
            System.out.println("============================= HERE");
            return cli.run(spec.commandLine().getOut(), spec.commandLine().getErr(), ExecutionDescribeCommand.class,
                    executions.get(0).getMetadata().getName());
        } else {
            spec.commandLine().getErr().println("⛔ Unable to find execution with submitted UUID " + uuid + ".");
            return ExitCode.SOFTWARE;
        }
    }
}
