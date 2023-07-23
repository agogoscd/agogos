package com.redhat.agogos.core.k8s;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@JsonSerialize(using = ToStringSerializer.class)
public enum Label {
    INSTANCE("instance"),
    NAME("name"),
    RESOURCE("resource");

    public static String AGOGOS_LABEL_PREFIX = "agogos.redhat.com/";

    private final String printable;
    private final static Map<String, Label> map = Arrays.asList(Label.values()).stream()
            .collect(Collectors.toMap(r -> r.toString().toLowerCase(), r -> r));

    private Label(String printable) {
        this.printable = printable;
    }

    @Override
    public String toString() {
        return String.format("%s%s", AGOGOS_LABEL_PREFIX, printable);
    }

    public static Label fromType(String type) {
        return type != null ? map.get(type.toLowerCase()) : null;
    }

    public static String create(Resource resource) {
        return String.format("%s%s", AGOGOS_LABEL_PREFIX, resource.toString().toLowerCase());
    }
}
