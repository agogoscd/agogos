package com.redhat.agogos.k8s.client;

public interface AgogosClient {

    /**
     * Return current namespace.
     * 
     * @return
     */
    String namespace();

    V1alpha1APIGroup v1alpha1();

}
