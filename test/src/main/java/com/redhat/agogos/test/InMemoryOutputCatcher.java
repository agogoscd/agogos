package com.redhat.agogos.test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

public class InMemoryOutputCatcher {

    StringWriter sout = new StringWriter();
    StringWriter serr = new StringWriter();

    @Getter
    PrintWriter out = new PrintWriter(sout);

    @Getter
    PrintWriter err = new PrintWriter(serr);

    public InMemoryOutputCatcher() {}

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

    private List<String> records(PrintWriter p, StringWriter s)  {
        p.flush();
        return new BufferedReader(new StringReader(s.toString())).lines().collect(Collectors.toList());
    }

    private boolean contains(PrintWriter p, StringWriter s, String message)  {
        return records(p, s).stream().anyMatch(m -> m.equals(message));
    }
}
