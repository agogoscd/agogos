package com.redhat.agogos.operator.k8s.controllers.group;

import com.redhat.agogos.core.ResourceStatus;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Status;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false)
public class GroupController extends AbstractController<Group> {

    private static final Logger LOG = LoggerFactory.getLogger(GroupController.class);

    @Override
    public UpdateControl<Group> reconcile(Group group, Context<Group> context) {
        Status groupStatus = group.getStatus();
        if (groupStatus.getStatus() != ResourceStatus.READY) {
            groupStatus.setStatus(ResourceStatus.READY);
            groupStatus.setReason("Group is ready");
            groupStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
            LOG.info("Set status for Group '{}' to {}", group.getFullName(), groupStatus.getStatus());
            return UpdateControl.updateStatus(group);
        }

        return UpdateControl.noUpdate();
    }
}
