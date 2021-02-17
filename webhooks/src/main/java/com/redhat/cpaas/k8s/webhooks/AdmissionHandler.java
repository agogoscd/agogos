package com.redhat.cpaas.k8s.webhooks;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import io.fabric8.kubernetes.client.CustomResource;

public abstract class AdmissionHandler<T extends CustomResource<?, ?>> {
    public static AdmissionReview review(AdmissionReview admissionReview, Consumer<AdmissionResponseBuilder> response) {
        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder() //
                .withUid(admissionReview.getRequest().getUid());

        response.accept(responseBuilder);

        AdmissionReviewBuilder reviewBuilder = new AdmissionReviewBuilder() //
                .withApiVersion(admissionReview.getApiVersion());

        return reviewBuilder.withResponse(responseBuilder.build()).build();
    }

    public static AdmissionReview allow(AdmissionReview admissionReview) {
        return AdmissionHandler.review(admissionReview, response -> response.withAllowed(true));
    }
}
