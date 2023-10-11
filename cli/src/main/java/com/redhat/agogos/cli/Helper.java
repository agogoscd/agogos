package com.redhat.agogos.cli;

import com.redhat.agogos.core.k8s.Label;
import io.fabric8.kubernetes.api.model.HasMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class Helper {

    @Inject
    CLI cli;

    public void println(String msg) {
        cli.getCommandLine().getOut().println(msg);
    }

    public void status(List<HasMetadata> resources) {
        status(resources.toArray(new HasMetadata[resources.size()]));
    }

    public void status(HasMetadata... resources) {
        status("👉 OK: ", resources);
    }

    public void status(String start, HasMetadata... resources) {
        StringBuilder sb = null;

        for (HasMetadata resource : resources) {
            sb = new StringBuilder()
                    .append(start)
                    .append(getStatusLine(resource));
            println(sb.toString());
        }
    }

    private String getStatusLine(HasMetadata resource) {
        StringBuilder sb = null;

        sb = new StringBuilder()//
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
