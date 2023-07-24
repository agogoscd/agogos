package com.redhat.agogos.core;

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

    @Override
    public String toString() {
        return printable;
    }
}
