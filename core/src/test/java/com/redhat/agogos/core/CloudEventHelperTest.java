package com.redhat.agogos.core;

import com.redhat.agogos.core.v1alpha1.Build;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CloudEventHelperTest {

    @Test
    public void type() throws Exception {
        String typeString = CloudEventHelper.type(Build.class, PipelineRunState.STARTED);
        Assertions.assertEquals(typeString, "com.redhat.agogos.event.build.started.v1alpha1");
    }
}
