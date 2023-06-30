package com.redhat.agogos.cli;

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
        StringBuilder sb = null;

        for (HasMetadata resource : resources) {
            sb = new StringBuilder()//
                    .append("👉 OK: ")
                    .append(getStatusLine(resource));
            LOG.info(sb.toString());
        }
    }

    public static String getStatusLine(HasMetadata resource) {
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

        return sb.toString();
    }
}
