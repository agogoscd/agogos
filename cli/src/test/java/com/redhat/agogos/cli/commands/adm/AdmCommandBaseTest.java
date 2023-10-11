package com.redhat.agogos.cli.commands.adm;

import com.redhat.agogos.cli.CLI.Profile;
import com.redhat.agogos.cli.commands.AbstractCommandTest;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class AdmCommandBaseTest extends AbstractCommandTest {
    protected EventListener el = null;
    protected ResourceQuota quota = null;
    protected ServiceAccount sa = null;
    protected List<GenericKubernetesResource> generics = null;

    @BeforeAll
    public void beforeAll() {
        cli.setProfile(Profile.admin);

        el = utils.loadTestResources(EventListener.class, "commands/adm/el.yml").get(0);
        quota = utils.loadTestResources(ResourceQuota.class, "commands/adm/quota.yml").get(0);
        sa = utils.loadTestResources(ServiceAccount.class, "commands/adm/sa.yml").get(0);
        generics = utils.loadTestResources(GenericKubernetesResource.class, "commands/adm/generics.yml");
    }
}
