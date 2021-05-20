package com.redhat;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import lombok.Getter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestLogger implements BeforeEachCallback, AfterEachCallback {

    static class InMemoryHandler extends Handler {
        @Getter
        List<LogRecord> records = new ArrayList<>();

        public InMemoryHandler() {
            //store.put("records", records);
        }

        public void clear() {
            records.clear();
        }

        @Override
        public void publish(LogRecord record) {
            System.out.println("PUBLISH");
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
        System.out.println("STATIC IMNIT");
        handler = new InMemoryHandler();
    }

    // @Override
    // public void beforeEach(ExtensionContext extensionContext) throws Exception {

    // }

    // @Override
    // public void afterEach(ExtensionContext extensionContext) throws Exception {

    // }

    public List<LogRecord> getRecords() {
        return TestLogger.handler.getRecords();
    }

    public List<String> getMessages() {

        return TestLogger.handler.getRecords().stream().map(e -> e.getMessage()).collect(Collectors.toList());
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // System.out.println("BEFORE");
        // LogContext.getLogContext().getLogger("").addHandler(handler);

        TestLogger.handler.clear();

    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        System.out.println("AFTER");
        //LogContext.getLogContext().getLogger("").removeHandler(handler);
        //handler = null;
    }

}
