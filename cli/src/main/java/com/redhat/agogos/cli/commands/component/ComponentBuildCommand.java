package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.cli.commands.build.BuildDescribeCommand;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
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

@Command(mixinStandardHelpOptions = true, name = "build", aliases = { "b" }, description = "build a component")
public class ComponentBuildCommand extends AbstractResourceSubcommand<Component> {
    @Parameters(index = "0", description = "Name of the component to build.")
    String name;

    @Override
    public Integer call() {
        Component component = kubernetesFacade.get(Component.class, kubernetesFacade.getNamespace(), name);
        if (component == null) {
            spec.commandLine().getOut().println("⛔ Unable to find component '" + name + "'");
            return ExitCode.USAGE;
        }
        String uuid = UUID.randomUUID().toString();

        SubmissionSpec spc = new SubmissionSpec();
        spc.setName(name);
        spc.setInstance(uuid);
        spc.setResource(Resource.COMPONENT);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = kubernetesFacade.unmarshal(Map.class,
                new ByteArrayInputStream(objectMapper.asJson(spc).getBytes()));

        CustomRun cr = new CustomRunBuilder()
                .withKind(HasMetadata.getKind(CustomRun.class))
                .withNewMetadata()
                .withGenerateName("agogos-submission-component-cli-custom-run-")
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
                        Label.RESOURCE + "=" + Resource.COMPONENT.toString().toLowerCase())
                .build();
        List<Build> builds = kubernetesFacade.listNotEmpty(Build.class, kubernetesFacade.getNamespace(), options);
        if (builds.size() > 0) {
            return cli.run(spec.commandLine().getOut(), spec.commandLine().getErr(), BuildDescribeCommand.class,
                    builds.get(0).getMetadata().getName());
        } else {
            spec.commandLine().getOut().println("⛔ Unable to find build with submitted UUID " + uuid + ".");
            return ExitCode.SOFTWARE;
        }
    }
}
