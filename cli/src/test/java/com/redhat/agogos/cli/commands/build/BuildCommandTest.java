package com.redhat.agogos.cli.commands.build;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.test.ResourceUtils;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@QuarkusTest
public class BuildCommandTest extends AbstractCommandTest {
    private static final Logger LOG = LoggerFactory.getLogger(BuildCommandTest.class);

    @Override
    @BeforeEach
    protected void setup() {
        super.setup();
        resourceLoader.installKubernetesResources(ResourceUtils.testResourceAsInputStream("loader/builds-list.yaml"), "test");
    }

    @Test
    @DisplayName("Should handle -l/--last option")
    void shouldHandleLastOption() throws Exception {
        List<String> args = Arrays.asList("-l", "--last");

        args.stream().forEach(arg -> {
            LOG.info("Testing build command option: " + arg);

            catcher.reset();

            int exitCode = cli.run(catcher.getOut(), catcher.getErr(), "build", "describe", "--last");
            assertEquals(0, exitCode);
            assertTrue(catcher.stdoutContains("💖 About"));
            assertTrue(catcher.stdoutContains("Name:\t\tnew-component-ml68b"));
            assertTrue(catcher.stdoutContains("🎉 Status"));
            assertTrue(catcher.stdoutContains("Status:\t\tFinished "));
            assertTrue(catcher.stdoutContains("Reason:\t\tBuild finished"));
            // Created date will change.
            //assertTrue(catcher.stdoutContains("Created:     2023-04-19 13:42:26"));
            assertTrue(catcher.stdoutContains("Started:\t2021-08-12 08:31:10"));
            assertTrue(catcher.stdoutContains("Finished:\t2021-08-12 08:33:32"));
            assertTrue(catcher.stdoutContains("Duration:\t2 minute(s)"));
        });
    }
}
