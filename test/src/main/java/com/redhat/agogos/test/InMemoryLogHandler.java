package com.redhat.agogos.test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import lombok.Getter;

public class InMemoryLogHandler extends Handler {
    @Getter
    List<LogRecord> records = new ArrayList<>();

    @Override
    public void publish(LogRecord record) {
        records.add(record);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    public List<String> getMessages() {
        return records.stream().map(e -> e.getMessage()).collect(Collectors.toList());

    }

    public boolean contains(String message) {
        return records.stream().map(e -> e.getMessage()).anyMatch(m -> m.equals(message));
    }
}
