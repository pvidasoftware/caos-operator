package com.puravida.caos.models

import io.kubernetes.client.openapi.models.V1ObjectMeta

import java.time.OffsetDateTime

class V1Wrapper {

    public static final String API_VERSION = "v1"
    public static final String KIND = "Caos"
    public static final String GROUP = "caos.puravida.com"
    public static final String PLURAL = "monkeys"
    public static final String FINALIZER = GROUP + "/finalizer"

    public static final String RECONCILED_ANNOTATION = GROUP + "/reconciled"
    public static final String RECONCILIATION_TIMESTAMP_ANNOTATION = GROUP + "/reconciled-at"

    public static final String CREATED_AT_ANNOTATION = GROUP + "/created-at"
    public static final String RESTARTED_AT_ANNOTATION = GROUP + "/restarted-at"

    private V1Caos v1

    V1Wrapper(V1Caos v1) {
        this.v1 = v1
    }

    V1Caos getResource(){
        v1
    }

    private void initAnnotations() {
        if (!v1.metadata.annotations) {
            v1.metadata.annotations = [:]
        }
    }

    String getName(){
        v1.metadata?.name ?: ""
    }

    String getNamespace(){
        v1.metadata?.namespace ?: ""
    }

    V1ObjectMeta getMetadata(){
        v1.metadata
    }

    String getMode(){
        v1.spec.mode
    }

    String getPodSelector(){
        v1.spec.podSelector
    }

    String getDeploymentSelector(){
        v1.spec.deploymentSelector
    }

    boolean isReconciled() {
        initAnnotations()
        return v1.metadata.annotations.containsKey(RECONCILED_ANNOTATION)
    }

    void reconciled() {
        initAnnotations();
        v1.metadata.annotations[RECONCILED_ANNOTATION] = Boolean.TRUE.toString()
        v1.metadata.annotations[RECONCILIATION_TIMESTAMP_ANNOTATION] =  OffsetDateTime.now().toString()
        v1.metadata.addFinalizersItem(FINALIZER)
    }

    boolean isBeingDeleted() {
        v1.metadata.deletionTimestamp != null;
    }

    void finalizeDeletion() {
        v1.metadata.finalizers.clear();
    }
}
