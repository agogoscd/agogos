package com.redhat.agogos.cli;

import com.redhat.agogos.core.k8s.Label;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Helper {
    private static final Logger LOG = LoggerFactory.getLogger(Helper.class);

    public static void status(List<HasMetadata> resources) {
        status(resources.toArray(new HasMetadata[resources.size()]));
    }

    public static void status(HasMetadata... resources) {
        status("👉 OK: ", resources);
    }

    public static void status(String start, HasMetadata... resources) {
        StringBuilder sb = null;

        for (HasMetadata resource : resources) {
            sb = new StringBuilder()//
                    .append(start)
                    .append(getStatusLine(resource));
            LOG.info(sb.toString());
        }
    }

    private static String getStatusLine(HasMetadata resource) {
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

        if (resource.getMetadata().getLabels().containsKey(Label.EXTENSION.toString())) {
            sb.append(" (extension: ")
                    .append(resource.getMetadata().getLabels().get(Label.EXTENSION.toString()))
                    .append(")");
        }

        return sb.toString();
    }
}
