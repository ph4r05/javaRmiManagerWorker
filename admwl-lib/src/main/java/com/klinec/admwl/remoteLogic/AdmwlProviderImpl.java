package com.klinec.admwl.remoteLogic;

import com.klinec.admwl.AdmwlOnJobCancelCheckListener;
import com.klinec.admwl.AdmwlOnJobFinishedListener;
import com.klinec.admwl.AdmwlOnJobProgressListener;
import com.klinec.admwl.remoteInterface.AdmwlProvider;
import com.klinec.admwl.remoteInterface.AdmwlTask;
import com.klinec.admwl.remoteInterface.AdmwlWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple worker provider implementation
 * Created by dusanklinec on 15.11.15.
 */
public class AdmwlProviderImpl<Result> implements AdmwlProvider<Result> {
    private static final Logger logger = LoggerFactory.getLogger(AdmwlProviderImpl.class);
    private static final long serialVersionUID = 1L;

    private static final AtomicLong pingCtr = new AtomicLong(0);

    /**
     * Map of registered workers.
     */
    private final Map<String, AdmwlWorker<Result>> workers = new ConcurrentHashMap<String, AdmwlWorker<Result>>();

    /**
     * Binary atomic flag determining if manager is still running.
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    /**
     * Concurrent queue of jobs to be executed by workers.
     */
    private final Queue<AdmwlTask<Result>> jobQueue = new ConcurrentLinkedQueue<AdmwlTask<Result>>();

    /**
     * Jobs finished listener.
     */
    private AdmwlOnJobFinishedListener<Result> jobFinishedListener;
    private AdmwlOnJobProgressListener<Result> jobProgressListener;
    private AdmwlOnJobCancelCheckListener<Result> jobCancelCheckListener;

    /**
     * Registry to be used for binding.
     */
    private Registry registry;

    /**
     * Name to be used for binding to the service.
     */
    private final String bindingName;

    /**
     * Stub being used for export.
     */
    private AdmwlProvider<Result> stub;

    /**
     * If true job enqueue is permitted if there is no worker available, otherwise
     * job is immediately returned as completed.
     */
    private boolean allowEnqueueIfNoWorker = true;

    public AdmwlProviderImpl(String bindingName) {
        this.bindingName = bindingName;
    }

    /**
     * Starts main ADMLP server.
     */
    public void initServer() throws RemoteException {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        stub = (AdmwlProvider<Result>) UnicastRemoteObject.exportObject(this, 0);

        // Starting our own registry so it has class definitions of our classes.
        // Starting a new registry may need to allow it on the local firewall
        // or to execute manager with administrator rights.
        if(registry == null) {
            logger.info("Registry was null, creating a new one on the localhost");
            registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        }

        // Rebind the exported provider.
        registry.rebind(bindingName, stub);

        // Old way - using default registry
        // Naming.bind("rmi://localhost/" + NAME, stub);
        logger.info("ADML Provider ready @ " + bindingName);
    }

    /**
     * Goes through all workers and remove those failing to respond on ping (connection broken)
     */
    public void checkAllWorkers(){
        if (workers.isEmpty()){
            return;
        }

        List<String> keysToRemove = new ArrayList<String>(workers.size());
        for (Map.Entry<String, AdmwlWorker<Result>> workerEntry : workers.entrySet()) {
            final String workerKey = workerEntry.getKey();
            final AdmwlWorker<Result> worker = workerEntry.getValue();

            boolean works = true;
            try {
                worker.ping(pingCtr.incrementAndGet());
                works = true;
            } catch(Exception e){
                works = false;
            }

            if (!works){
                logger.info("Worker is not working {}", workerKey);
                keysToRemove.add(workerKey);
            }
        }

        for(String wKey : keysToRemove){
            workers.remove(wKey);
        }

        if (!keysToRemove.isEmpty()){
            logger.info("Workers removed: {}, workers left: {}", keysToRemove.size(), workers.size());
        }
    }

    /**
     * Adds computation job to the queue
     */
    public void enqueueJob(AdmwlTask<Result> job){
        if (!isRunning.get()){
            logger.info("System could not accept a new job as it is terminated");
            return;
        }

        // Has to have at least one worker registered.
        if (!allowEnqueueIfNoWorker && workers.isEmpty()){
            logger.error("No workers registered, cannot compute a job");
            if (jobFinishedListener != null) {
                jobFinishedListener.onAdmwlJobFinished(job, null);
            }
            return;
        }

        jobQueue.add(job);
    }

    /**
     * Returns false is server is shutted down - accepting no more jobs.
     * @return
     */
    public boolean isServerRunning(){
        return isRunning.get();
    }

    /**
     * Initiates shutting down procedure.
     * Terminates all connected workers.
     */
    public void shutdown(){
        for (Map.Entry<String, AdmwlWorker<Result>> workerEntry : workers.entrySet()) {
            try {
                logger.info("Shutting down worker {}", workerEntry.getKey());
                workerEntry.getValue().shutdown(true);

            } catch (RemoteException e) {
                logger.error("Exception when shutting down worker " + workerEntry.getKey(), e);
            }
        }

        isRunning.set(false);

        // Unbind from registry
        try {
            registry.unbind(bindingName);
        } catch (RemoteException e) {
            logger.error("Exception when unbinding", e);
        } catch (NotBoundException e) {
            logger.error("Could not unbind from RMI registry", e);
        } finally {
            registry = null;
        }

        stub = null;

        // Wait so workers can unregister
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("Interrupted");
        }
    }

    @Override
    public AdmwlTask<Result> getNewJob(String workerId, long timeout) throws RemoteException {
        if (!isRunning.get()){
            return null;
        }

        return jobQueue.poll();
    }

    @Override
    public void jobFinished(String workerId, AdmwlTask<Result> admwlTask, Result jobResult) throws RemoteException {
        logger.info("Job has finished");
        if (jobFinishedListener == null){
            logger.error("Job finished listener is null");
            return;
        }

        jobFinishedListener.onAdmwlJobFinished(admwlTask, jobResult);
    }

    @Override
    public void registerWorker(String workerId, AdmwlWorker<Result> workerCallback) throws RemoteException {
        logger.info("Registering worker {}", workerId);
        workers.put(workerId, workerCallback);
    }

    @Override
    public void unregisterWorker(String workerId) throws RemoteException {
        logger.info("Unregistering worker {}", workerId);
        workers.remove(workerId);
    }

    @Override
    public String keepAlivePing(String workerId, long pingCtr) throws RemoteException {
        if (!isRunning.get()){
            return null;
        }

        return workerId;
    }

    //
    // Extended API
    //

    @Override
    public boolean jobProgress(String workerId, String taskId, double progress) throws RemoteException {
        if (!isRunning.get()){
            return false;
        }

        if (jobProgressListener != null){
            return jobProgressListener.onAdmwlJobProgressed(workerId, taskId, progress);
        }

        return true;
    }

    @Override
    public boolean shouldCancel(String workerId, String taskId) throws RemoteException {
        if (!isRunning.get()){
            return true;
        }

        if (jobCancelCheckListener != null){
            return jobCancelCheckListener.onAdmwlJobCancelCheck(workerId, taskId);
        }

        return false;
    }

    @Override
    public boolean shouldTerminate(String workerId) throws RemoteException {
        if (!isRunning.get()){
            return true;
        }

        return false;
    }

    public AdmwlOnJobFinishedListener<Result> getJobFinishedListener() {
        return jobFinishedListener;
    }

    public void setJobFinishedListener(AdmwlOnJobFinishedListener<Result> jobFinishedListener) {
        this.jobFinishedListener = jobFinishedListener;
    }

    public AdmwlOnJobProgressListener<Result> getJobProgressListener() {
        return jobProgressListener;
    }

    public void setJobProgressListener(AdmwlOnJobProgressListener<Result> jobProgressListener) {
        this.jobProgressListener = jobProgressListener;
    }

    public AdmwlOnJobCancelCheckListener<Result> getJobCancelCheckListener() {
        return jobCancelCheckListener;
    }

    public void setJobCancelCheckListener(AdmwlOnJobCancelCheckListener<Result> jobCancelCheckListener) {
        this.jobCancelCheckListener = jobCancelCheckListener;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public boolean isAllowEnqueueIfNoWorker() {
        return allowEnqueueIfNoWorker;
    }

    public void setAllowEnqueueIfNoWorker(boolean allowEnqueueIfNoWorker) {
        this.allowEnqueueIfNoWorker = allowEnqueueIfNoWorker;
    }
}
