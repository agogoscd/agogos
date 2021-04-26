package com.redhat.agogos.cli.commands.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Option;

public abstract class AbstractDescribeCommand<T> implements Runnable {

    @Option(names = { "--output", "-o" })
    String output;

    protected void describe(T resource) {
        if (output != null && output.equals("yaml")) {
            try {
                System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resource));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            System.out.println(resource);
        }
    }

}
