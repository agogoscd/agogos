package com.redhat.agogos.cli.commands.adm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Utils {
    public static Integer executeShellCommand(String command, HashMap<String, String> envVars) {

        final Logger LOG = LoggerFactory.getLogger(Utils.class);

        int exitCode = 1;
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));

        LOG.info("👉 Command {}", command);
        // Set environment variables
        Map<String, String> env = processBuilder.environment();

        envVars.forEach(env::put);

        try {
            Process process;
            process = processBuilder.inheritIO().start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                LOG.info("👉 Command {} : responded '{}'", command, line);
            }

            // Wait for the command to complete
            exitCode = process.waitFor();
            process.getOutputStream();
            LOG.info("👉 Command {} : Exit code '{}'", command, exitCode);

            return exitCode;

        } catch (IOException | InterruptedException e) {
            LOG.info("👉 Command {} : Exception caught '{}'", command, e.getStackTrace());
            return exitCode;
        }
    }
}
