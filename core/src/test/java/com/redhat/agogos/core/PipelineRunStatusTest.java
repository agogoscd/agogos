package com.redhat.agogos.core;

import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.knative.internal.pkg.apis.ConditionBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

@QuarkusTest
public class PipelineRunStatusTest {

    @Test
    public void fromTektonEnumValue() throws Exception {
        PipelineRunStatus status = PipelineRunStatus.fromTekton("", "STARTED");

        Assertions.assertTrue(
                status == PipelineRunStatus.STARTED,
                String.format("%s != %s", status, PipelineRunStatus.STARTED));
    }

    @Test
    public void fromTektonDefault() throws Exception {
        PipelineRunStatus status = PipelineRunStatus.fromTekton("", "");

        Assertions.assertTrue(
                status == PipelineRunStatus.FAILED,
                String.format("%s != %s", status, PipelineRunStatus.FAILED));
    }

    @Test
    public void fromTektonCancelled() throws Exception {
        PipelineRunStatus status = PipelineRunStatus.fromTekton("", "PipelineRunCancelled");

        Assertions.assertTrue(
                status == PipelineRunStatus.CANCELLED,
                String.format("%s != %s", status, PipelineRunStatus.CANCELLED));
    }

    @Test
    public void fromTektonCancelling() throws Exception {
        PipelineRunStatus status = PipelineRunStatus.fromTekton("Unknown", "PipelineRunCancelled");

        Assertions.assertTrue(
                status == PipelineRunStatus.CANCELLING,
                String.format("%s != %s", status, PipelineRunStatus.CANCELLING));
    }

    @Test
    public void fromTektonTimeout() throws Exception {
        PipelineRunStatus status = PipelineRunStatus.fromTekton("", "PipelineRunTimeout");

        Assertions.assertTrue(
                status == PipelineRunStatus.TIMEOUT,
                String.format("%s != %s", status, PipelineRunStatus.TIMEOUT));
    }

    @Test
    public void fromPipelineRun() throws Exception {
        Condition condition = new ConditionBuilder()
                .withStatus("Unknown")
                .withReason("PipelineRunCancelled")
                .build();
        PipelineRun pipelineRun = new PipelineRunBuilder()
                .withNewStatus()
                .withConditions(List.of(condition))
                .endStatus()
                .build();
        PipelineRunStatus status = PipelineRunStatus.fromPipelineRun(pipelineRun);

        Assertions.assertTrue(
                status == PipelineRunStatus.CANCELLING,
                String.format("%s != %s", status, PipelineRunStatus.CANCELLING));
    }

    @ParameterizedTest
    @EnumSource(value = PipelineRunStatus.class, names = { "STARTED", "RUNNING" })
    public void toEventStarted(PipelineRunStatus status) throws Exception {

        Assertions.assertTrue(
                status.toEvent() == PipelineRunState.STARTED,
                String.format("%s != %s", status.toEvent(), PipelineRunState.STARTED));
    }

    @ParameterizedTest
    @EnumSource(value = PipelineRunStatus.class, names = { "COMPLETED", "SUCCEEDED" })
    public void toEventSucceeded(PipelineRunStatus status) throws Exception {
        Assertions.assertTrue(
                status.toEvent() == PipelineRunState.SUCCEEDED,
                String.format("%s != %s", status.toEvent(), PipelineRunState.SUCCEEDED));
    }

    @ParameterizedTest
    @EnumSource(value = PipelineRunStatus.class, names = { "FAILED", "TIMEOUT" })
    public void toEventFailed(PipelineRunStatus status) throws Exception {
        Assertions.assertTrue(
                status.toEvent() == PipelineRunState.FAILED,
                String.format("%s != %s", status.toEvent(), PipelineRunState.FAILED));
    }

    @ParameterizedTest
    @EnumSource(value = PipelineRunStatus.class, names = { "STARTED", "RUNNING" })
    public void toStatusRunning(PipelineRunStatus status) throws Exception {
        Assertions.assertTrue(
                status.toStatus() == ResultableResourceStatus.RUNNING,
                String.format("%s != %s", status.toStatus(), ResultableResourceStatus.RUNNING));
    }

    @ParameterizedTest
    @EnumSource(value = PipelineRunStatus.class, names = { "COMPLETED", "SUCCEEDED" })
    public void toStatusFinished(PipelineRunStatus status) throws Exception {
        Assertions.assertTrue(
                status.toStatus() == ResultableResourceStatus.FINISHED,
                String.format("%s != %s", status.toStatus(), ResultableResourceStatus.FINISHED));
    }

    @ParameterizedTest
    @EnumSource(value = PipelineRunStatus.class, names = { "CANCELLING", "CANCELLED" })
    public void toStatusAborted(PipelineRunStatus status) throws Exception {
        Assertions.assertTrue(
                status.toStatus() == ResultableResourceStatus.ABORTED,
                String.format("%s != %s", status.toStatus(), ResultableResourceStatus.ABORTED));
    }

    @ParameterizedTest
    @EnumSource(value = PipelineRunStatus.class, names = { "FAILED", "TIMEOUT" })
    public void toStatusFailed(PipelineRunStatus status) throws Exception {
        Assertions.assertTrue(
                status.toStatus() == ResultableResourceStatus.FAILED,
                String.format("%s != %s", status.toStatus(), ResultableResourceStatus.FAILED));
    }

}
