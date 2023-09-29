package com.redhat.agogos.test;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryOutputCatcher {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryOutputCatcher.class);

    StringWriter sout = new StringWriter(

    );
    StringWriter serr = new StringWriter();

    @Getter
    PrintWriter out = new PrintWriter(sout);

    @Getter
    PrintWriter err = new PrintWriter(serr);

    public InMemoryOutputCatcher() {
    }

    public void reset() {
        out.flush();
        err.flush();
        sout.getBuffer().setLength(0);
        serr.getBuffer().setLength(0);
    }

    public List<String> stdoutMessages() {
        return records(out, sout);
    }

    public boolean stdoutContains(String message) {
        return contains(out, sout, message);
    }

    public List<String> stderrMessages() {
        return records(err, serr);
    }

    public boolean stderrContains(String message) {
        return contains(err, serr, message);
    }

    public boolean compareToStdout(List<String> data) {
        return data.equals(stdoutMessages());
    }

    public boolean compareToStdoutNoDurations(List<String> data) {
        List<String> so = stdoutMessages().stream().map(x -> x.replaceAll("\\d+ \\w+\\(s\\)\\s*", ""))
                .collect(Collectors.toList());
        List<String> d = data.stream().map(x -> x.replaceAll("\\d+ \\w+\\(s\\)\\s*", "")).collect(Collectors.toList());
        return d.equals(so);
    }

    public boolean compareToStderr(List<String> data) {
        return data.equals(stderrMessages());
    }

    public void logStdout() {
        stdoutMessages().forEach(s -> {
            LOG.info("STDOUT: " + s);
        });
    }

    public void logStderr() {
        stderrMessages().forEach(s -> {
            LOG.info("STDERR: " + s);
        });
    }

    private List<String> records(PrintWriter p, StringWriter s) {
        p.flush();
        return new BufferedReader(new StringReader(s.toString())).lines().collect(Collectors.toList());
    }

    private boolean contains(PrintWriter p, StringWriter s, String message) {
        return records(p, s).stream().anyMatch(m -> m.equals(message));
    }
}
