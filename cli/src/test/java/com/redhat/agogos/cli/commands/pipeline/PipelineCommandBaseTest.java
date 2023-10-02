package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Run;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class PipelineCommandBaseTest extends AbstractCommandTest {
    protected List<Run> runs;
    protected List<Pipeline> pipelines;

    @BeforeAll
    public void beforeAll() {
        pipelines = utils.loadTestResources(Pipeline.class, "pipeline/pipelines.yml");
        runs = utils.loadTestResources(Run.class, "pipeline/runs.yml");
    }
}
