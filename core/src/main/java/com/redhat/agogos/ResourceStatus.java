package com.redhat.agogos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@JsonSerialize(using = ToStringSerializer.class)
public enum ResourceStatus {
    NEW("New"),
    INITIALIZING("Initializing"),
    READY("Ready"),
    FAILED("Failed");

    private final String printable;

    private ResourceStatus(String printable) {
        this.printable = printable;
    }

    public static int maxLength() {
        int length = 0;

        for (ResourceStatus status : ResourceStatus.values()) {
            if (status.toString().length() > length) {
                length = status.toString().length();
            }
        }

        return length;
    }

    @Override
    public String toString() {
        return printable;
    }
}
