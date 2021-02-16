package com.redhat.cpaas.k8s.webhooks;

import io.fabric8.kubernetes.client.CustomResource;

public abstract class AdmissionHandler<T extends CustomResource<?, ?>> {

}
