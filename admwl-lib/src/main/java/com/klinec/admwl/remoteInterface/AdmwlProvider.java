package com.klinec.admwl.remoteInterface;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Core RMI interface of a ADML provider.
 * ADML provider is a RMI server distributing computation jobs to the connected client workers.
 * ADML provider is supposed to be started from the control center panel. Worker clients are supposed
 * to connect to the ADML provider and poll job queue when they are available.
 *
 * When worker starts it should register to ADML provider.
 *
 * Created by dusanklinec on 15.11.15.
 */
public interface AdmwlProvider<Result> extends Remote {
    /**
     * Worker machines connected to the job provider server pull a new job using this method from the job queue.
     * If no job is currently in the work queue, null is returned.
     *
     * @param workerId worker string identifier, UUID
     * @param timeout timeout value to block until some task becomes available in the task queue. If value is <= 0 call
     *                is non-blocking, returns immediately.
     *
     * @return A new task to be executed.
     * @throws RemoteException
     */
    AdmwlTask<Result> getNewJob(String workerId, long timeout) throws RemoteException;

    /**
     * Worker calls this method when computation is finished.
     * @param workerId worker string identifier, UUID
     * @param admwlTask  task that was finished
     * @param jobResult result of the task
     */
    void jobFinished(String workerId, AdmwlTask<Result> admwlTask, Result jobResult) throws RemoteException;

    /**
     * New worker machine registers itself to a job provider.
     * Job provider is aware of a new worker capable of executing tasks.
     *
     * @param workerId worker string identifier, UUID
     * @throws RemoteException
     */
    void registerWorker(String workerId, AdmwlWorker<Result> workerCallback) throws RemoteException;

    /**
     * Worker is disconnecting from work queue.
     *
     * @param workerId worker string identifier, UUID
     * @throws RemoteException
     */
    void unregisterWorker(String workerId) throws RemoteException;

    /**
     * Worker calls this on a regular basis to let server know it lives.
     * Moreover worker check if server exists. If keep alive is without response, client may shutdown.
     *
     * @param workerId worker string identifier, UUID
     * @param pingCtr ping sequence counter.
     * @throws RemoteException
     */
    String keepAlivePing(String workerId, long pingCtr) throws RemoteException;

    //
    // Extended API
    //

    /**
     * Worker signalizes computation progress to the manager.
     *
     * @param workerId worker string identifier, UUID
     * @param taskId task that was finished
     * @param progress total progress value
     * @return if true, worker keeps on computing the task. If false, worker stops the computation.
     */
    boolean jobProgress(String workerId, String taskId, double progress) throws RemoteException;

    /**
     * Worker asks provider whether he should cancel computation of the provided job.
     *
     * @param workerId  worker string identifier, UUID
     * @param taskId unique string task identifier, UUID
     * @return
     * @throws RemoteException
     */
    boolean shouldCancel(String workerId, String taskId) throws RemoteException;

    /**
     * Worker asks if he should terminate itself.
     * Works as a keep-alive also.
     *
     * @param workerId worker string identifier, UUID
     * @return true if worker should terminate itself.
     * @throws RemoteException
     */
    boolean shouldTerminate(String workerId) throws RemoteException;
}
