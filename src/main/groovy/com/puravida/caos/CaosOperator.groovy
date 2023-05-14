package com.puravida.caos

import com.puravida.caos.models.V1Wrapper
import com.puravida.caos.models.V1Caos
import com.puravida.caos.models.V1CaosList
import groovy.util.logging.Slf4j
import io.kubernetes.client.extended.controller.reconciler.Request
import io.kubernetes.client.extended.controller.reconciler.Result
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.ModelMapper
import io.micronaut.core.annotation.NonNull
import io.micronaut.kubernetes.client.informer.Informer
import io.micronaut.kubernetes.client.operator.Operator
import io.micronaut.kubernetes.client.operator.OperatorResourceLister
import io.micronaut.kubernetes.client.operator.ResourceReconciler

@Operator(
        informer = @Informer(
                apiType = V1Caos,
                apiListType = V1CaosList,
                apiGroup = V1Wrapper.GROUP,
                resourcePlural = V1Wrapper.PLURAL,
                resyncCheckPeriod = 30000L
        )
)
@Slf4j
class CaosOperator implements ResourceReconciler<V1Caos>{

        private final CustomObjectsApi customApi
        private final CoreV1Api coreApi
        private final AppsV1Api appsApi
        private final CaosExecutor caosScheduler

        CaosOperator(CustomObjectsApi customApi,
                     CoreV1Api coreApi,
                     AppsV1Api appsApi,
                     CaosExecutor caosScheduler ) {
                this.customApi = customApi
                this.coreApi = coreApi
                this.appsApi = appsApi
                this.caosScheduler = caosScheduler
                ModelMapper.addModelMap(
                        V1Wrapper.GROUP,
                        V1Wrapper.API_VERSION,
                        V1Wrapper.KIND,
                        V1Wrapper.PLURAL,
                        V1Caos,
                        V1CaosList,
                )
                log.info("Caos operator created")
        }

        @Override
        Result reconcile(@NonNull Request request, @NonNull OperatorResourceLister<V1Caos> lister) {
                try {
                        log.info("reconcile $request")

                        def optional = lister.get(request)
                        if (!optional.present) {
                                return new Result(false)
                        }

                        def res = optional.get()
                        def wrapper = new V1Wrapper(res)

                        if (wrapper.beingDeleted) {
                                return deleteResource(wrapper)
                        } else {
                                return createOrUpdateResource(wrapper)
                        }

                }catch( ApiException apiException){
                        log.error("ApiException $apiException.code, $apiException.message \n$apiException.responseBody",apiException)
                        return new Result(false)
                }catch(Throwable e){
                        log.error(e.toString(), e)
                        return new Result(false)
                }
        }

        Result deleteResource(V1Wrapper wrapper){
                log.info("Removing $wrapper.name")

                caosScheduler.remove(wrapper)

                wrapper.metadata.finalizers.clear();
                customApi.replaceNamespacedCustomObject(
                        V1Wrapper.GROUP,
                        V1Wrapper.API_VERSION,
                        wrapper.namespace,
                        V1Wrapper.PLURAL,
                        wrapper.name,
                        wrapper.resource,
                        null,
                        null)
                return new Result(false)
        }

        Result createOrUpdateResource(V1Wrapper wrapper){
                caosScheduler.run(wrapper)
                if( !wrapper.reconciled ) {
                        return markAsResolved(wrapper)
                }
                return new Result(false)
        }

        Result markAsResolved(V1Wrapper wrapper){
                wrapper.reconciled()
                customApi.replaceNamespacedCustomObject(
                        V1Wrapper.GROUP,
                        V1Wrapper.API_VERSION,
                        wrapper.namespace,
                        V1Wrapper.PLURAL,
                        wrapper.name,
                        wrapper.resource,
                        null,
                        null)
                log.info "Monkey reconciled $wrapper.name"
                return new Result(false)
        }
}
