package com.redhat.agogos;

public enum ResourceStatus {
    New,
    Initializing,
    Ready,
    Failed;

    public static int maxLength() {
        int length = 0;

        for (ResourceStatus status : ResourceStatus.values()) {
            if (status.toString().length() > length) {
                length = status.toString().length();
            }
        }

        return length;
    }
}
