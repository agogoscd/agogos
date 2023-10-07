package com.redhat.agogos.cli.commands.stage;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.v1alpha1.Stage;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "d", "desc" }, description = "describe stage")
public class StageDescribeCommand extends AbstractResourceSubcommand<Stage> {

    @Parameters(description = "Name of the stage")
    String name;

    @Override
    public Integer call() {
        Stage stage = null;

        stage = getStage(kubernetesFacade.getNamespace());
        if (stage == null) {
            stage = getStage(agogosEnvironment.getRunningNamespace());
        }

        return showResource(stage);
    }

    private Stage getStage(String namespace) {
        return kubernetesFacade.get(Stage.class, namespace, name);
    }
}
