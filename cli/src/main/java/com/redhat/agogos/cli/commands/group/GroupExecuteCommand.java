package com.redhat.agogos.cli.commands.group;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.cli.commands.execution.ExecutionDescribeCommand;
import com.redhat.agogos.cli.eventing.CloudEventPublisher;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Submission.SubmissionSpec;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.UUID;

@Command(mixinStandardHelpOptions = true, name = "execute", aliases = { "ex", "e" }, description = "execute a group")
public class GroupExecuteCommand extends AbstractResourceSubcommand<Group> {
    @Parameters(index = "0", description = "Name of the group to execute.")

    String name;

    @Inject
    CloudEventPublisher publisher;

    @Override
    public Integer call() {
        String uuid = UUID.randomUUID().toString();

        SubmissionSpec spc = new SubmissionSpec();
        spc.setName(name);
        spc.setInstance(uuid);
        spc.setResource(Resource.GROUP);

        publisher.publish(kubernetesFacade.getNamespace(), "execution", Group.class, spc);

        ListOptions options = new ListOptionsBuilder()
                .withLabelSelector(Label.NAME + "=" + name + "," + Label.INSTANCE + "=" + uuid + "," +
                        Label.RESOURCE + "=" + Resource.GROUP.toString().toLowerCase())
                .build();
        List<Execution> executions = kubernetesFacade.list(Execution.class, kubernetesFacade.getNamespace(), options, true);
        if (executions.size() > 0) {
            return cli.run(ExecutionDescribeCommand.class, executions.get(0).getMetadata().getName());
        } else {
            spec.commandLine().getOut().println("Unable to find execution with submitted UUID " + uuid + ".");
            return CommandLine.ExitCode.SOFTWARE;
        }
    }
}
