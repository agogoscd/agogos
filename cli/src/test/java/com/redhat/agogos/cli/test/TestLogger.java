package com.redhat.agogos.cli.test;

import lombok.Getter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class TestLogger implements BeforeEachCallback, AfterEachCallback {

    static class InMemoryHandler extends Handler {
        @Getter
        List<LogRecord> records = new ArrayList<>();

        public void clear() {
            records.clear();
        }

        @Override
        public void publish(LogRecord record) {
            records.add(record);
            System.out.println(records.size());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    public static TestLogger create() {
        return new TestLogger();
    }

    static InMemoryHandler handler;

    static {
        handler = new InMemoryHandler();
    }

    public List<LogRecord> getRecords() {
        return TestLogger.handler.getRecords();
    }

    public List<String> getMessages() {
        return TestLogger.handler.getRecords().stream().map(e -> e.getMessage()).collect(Collectors.toList());
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        TestLogger.handler.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
    }

}
