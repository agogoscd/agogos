package com.redhat.agogos.operator.k8s.controllers.group;

import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration
public class GroupController extends AbstractController<Group> {
}
