package com.redhat.agogos.cli;

import com.redhat.agogos.core.k8s.Label;
import io.fabric8.kubernetes.api.model.HasMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.PrintWriter;
import java.util.List;

@ApplicationScoped
public class Helper {

    @Inject
    CLI cli;

    public PrintWriter getStdout() {
        return cli.getCommandLine().getOut();
    }

    public PrintWriter getStderr() {
        return cli.getCommandLine().getErr();
    }

    public void printStdout(String msg) {
        cli.getCommandLine().getOut().println(msg);
    }

    public void printStderr(String msg) {
        cli.getCommandLine().getErr().println(msg);
    }

    public void printStatus(List<HasMetadata> resources) {
        printStatus(resources.toArray(new HasMetadata[resources.size()]));
    }

    public void printStatus(HasMetadata... resources) {
        printStatus("👉 OK: ", resources);
    }

    public void printStatus(String start, HasMetadata... resources) {
        StringBuilder sb = null;

        for (HasMetadata resource : resources) {
            sb = new StringBuilder()
                    .append(start)
                    .append(getStatusLine(resource));
            printStdout(sb.toString());
        }
    }

    private String getStatusLine(HasMetadata resource) {
        StringBuilder sb = null;

        sb = new StringBuilder()
                .append(resource.getKind())
                .append(": ")
                .append(resource.getMetadata().getName())
                .append(" (")
                .append(resource.getApiVersion())
                .append(")");

        if (resource.getMetadata().getNamespace() != null) {
            sb.append(" (ns: ")
                    .append(resource.getMetadata().getNamespace())
                    .append(")");
        }

        if (resource.getMetadata().getLabels() != null
                && resource.getMetadata().getLabels().containsKey(Label.EXTENSION.toString())) {
            sb.append(" (extension: ")
                    .append(resource.getMetadata().getLabels().get(Label.EXTENSION.toString()))
                    .append(")");
        }

        return sb.toString();
    }
}
