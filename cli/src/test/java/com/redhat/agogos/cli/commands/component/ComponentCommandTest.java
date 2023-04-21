package com.redhat.agogos.cli.commands.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ComponentCommandTest extends AbstractCommandTest {

    @Test
    @DisplayName("Should not build unknown component")
    void shouldNotBuildUnknownComponent() throws Exception {
        mockServer.expect().post().withPath("/apis/agogos.redhat.com/v1alpha1/namespaces/test/builds")
                .andReturn(Status.BAD_REQUEST.getStatusCode(),
                        new StatusBuilder()
                                .withCode(Status.BAD_REQUEST.getStatusCode())
                                .withMessage("Component 'test-component' does not exist in namespace 'default'")
                                .build())
                .always();
        int exitCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "build", "test-component");

        assertEquals(1, exitCode); // Invalid output
        assertTrue(catcher.stderrContains("🛑 Oops, an error occurred!"));
        assertTrue(catcher.stderrContains("Component 'test-component' does not exist in namespace 'default'"));
    }
}
