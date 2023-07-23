package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.cli.commands.build.BuildDescribeCommand;
import com.redhat.agogos.cli.eventing.CloudEventPublisher;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Submission.SubmissionSpec;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.UUID;

@Command(mixinStandardHelpOptions = true, name = "build", aliases = { "b" }, description = "build a component")
public class ComponentBuildCommand extends AbstractResourceSubcommand<Component> {
    @Parameters(index = "0", description = "Name of the component to build.")
    String name;

    @Inject
    CloudEventPublisher publisher;

    @Override
    public void run() {
        String uuid = UUID.randomUUID().toString();

        SubmissionSpec spc = new SubmissionSpec();
        spc.setName(name);
        spc.setInstance(uuid);
        spc.setResource(Resource.COMPONENT);

        publisher.publish(kubernetesFacade.getNamespace(), "build", Component.class, spc);

        ListOptions options = new ListOptionsBuilder()
                .withLabelSelector(Label.NAME + "=" + name + "," + Label.INSTANCE + "=" + uuid + "," +
                        Label.RESOURCE + "=" + Resource.COMPONENT.toString().toLowerCase())
                .build();
        List<Build> builds = kubernetesFacade.list(Build.class, options);
        if (builds.size() > 0) {
            cli.run(BuildDescribeCommand.class, builds.get(0).getMetadata().getName());
        } else {
            spec.commandLine().getOut().println("Unable to find build with submitted UUID " + uuid + ".");
        }
    }
}
