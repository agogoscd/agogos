package com.redhat.agogos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@JsonSerialize(using = ToStringSerializer.class)
public enum ResultableResourceStatus {
    NEW("New"),
    RUNNING("Running"),
    FINISHED("Finished"),
    FAILED("Failed"),
    ABORTED("Aborted");

    private final String printable;

    private ResultableResourceStatus(String printable) {
        this.printable = printable;
    }

    @Override
    public String toString() {
        return printable;
    }
}
