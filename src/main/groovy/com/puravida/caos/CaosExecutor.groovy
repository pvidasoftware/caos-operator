package com.puravida.caos

import com.puravida.caos.models.V1Caos
import com.puravida.caos.models.V1CaosList
import com.puravida.caos.models.V1Wrapper
import groovy.util.logging.Slf4j
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.apis.CustomObjectsApi
import io.kubernetes.client.util.ModelMapper
import io.micronaut.context.annotation.Context
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Slf4j
@Singleton
@Context
class CaosExecutor {

    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1)

    Map<String, ScheduledFuture> tasks = [:]

    private final CustomObjectsApi customApi
    private final CoreV1Api coreApi
    private final AppsV1Api appsApi

    CaosExecutor(CustomObjectsApi customApi, CoreV1Api coreApi, AppsV1Api appsApi) {
        this.customApi = customApi
        this.coreApi = coreApi
        this.appsApi = appsApi
    }

    void remove(V1Wrapper wrapper) {
        log.info "Canceling task for $wrapper.name"
        if( tasks.containsKey(wrapper.name)){
            def task = tasks[wrapper.name]
            task.cancel(true)
            log.info "Task for $wrapper.name canceled"
            tasks.remove(wrapper.name)
        }
    }

    void run(final V1Wrapper wrapper) {
        if( tasks.containsKey(wrapper.name)){
            log.info "Caos for $wrapper.podSelector was previously created, doing nothing"
            return
        }
        long rate = 1 //1sec
        switch (wrapper.mode){
            case 'friendly':
                rate *= 60*4 // 4mnt
                break
            case 'moderate':
                rate *= 60 // 1 mnt
                break
            case 'aggressive':
                rate *= 6 // 6 sec
                break
        }
        log.info "Running caos for $wrapper.podSelector at $rate secs"
        def task = executorService.scheduleAtFixedRate({
            try{
                log.info "Running task"
                selectAndDeletePod(wrapper)
            }catch(Throwable t){
                log.error "Error runnning task", t
            }
        } as Runnable, 1, rate, TimeUnit.SECONDS)

        tasks[wrapper.name] = task
    }

    void selectAndDeletePod(final V1Wrapper wrapper){

        log.info "Creating caos for $wrapper.podSelector in $wrapper.namespace"

        def names = coreApi
                .listNamespacedPod(
                        wrapper.namespace,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ).items*.metadata.name

        def podSelector = ~wrapper.podSelector

        def pods = names.findAll({name->
            name ==~ podSelector
        })

        pods.shuffle()

        log.info "Selecting of of $pods"

        if (!pods) {
            log.info "No pods matching $wrapper.podSelector"
            return
        }

        def selected = pods.first()

        log.info "Deleting $selected pod"

        coreApi.deleteNamespacedPod(selected,
                "default",
                null, null,
                null, null,
                null, null)

    }

}
