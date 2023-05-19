package com.redhat.agogos.k8s.controllers.build;

import com.redhat.agogos.k8s.controllers.AbstractRunController;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = PipelineRunDependentResource.class, reconcilePrecondition = PipelineRunPrecondition.class)
})
public class BuildController extends AbstractRunController<Build> implements EventSourceInitializer<Build> {

    private static final Logger LOG = LoggerFactory.getLogger(BuildController.class);
    public static final String COMPONENT_INDEX = "ComponentIndex";

    @Override
    protected Component parentResource(Build build, Context<Build> context) {
        return context.getSecondaryResource(Component.class).orElseThrow();
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Build> context) {
        context.getPrimaryCache().addIndexer(COMPONENT_INDEX,
                b -> List.of(indexKey(b.getSpec().getComponent(), b.getMetadata().getNamespace())));
        InformerEventSource<Component, Build> componentES = new InformerEventSource<>(InformerConfiguration
                .from(Component.class, context)
                .withSecondaryToPrimaryMapper(component -> context.getPrimaryCache().byIndex(COMPONENT_INDEX,
                        indexKey(component.getMetadata().getName(), component.getMetadata().getNamespace())).stream()
                        .map(ResourceID::fromResource).collect(Collectors.toSet()))
                .withPrimaryToSecondaryMapper((PrimaryToSecondaryMapper<Build>) primary -> Set.of(
                        new ResourceID(primary.getSpec().getComponent(), primary.getMetadata().getNamespace())))
                .build(), context);
        return EventSourceInitializer.nameEventSources(componentES);
    }

    private String indexKey(String name, String namespace) {
        return name + "#" + namespace;
    }
}