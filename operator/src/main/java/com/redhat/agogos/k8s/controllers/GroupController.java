package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.v1alpha1.Group;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration
public class GroupController extends AbstractController<Group> {
}
