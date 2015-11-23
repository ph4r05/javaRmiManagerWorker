package com.klinec.admwl.remoteLogic;

import com.klinec.admwl.AdmwlCancellation;
import com.klinec.admwl.AdmwlProgressMonitor;
import com.klinec.admwl.remoteInterface.AdmwlProvider;
import com.klinec.admwl.remoteInterface.AdmwlTask;
import com.klinec.admwl.remoteInterface.AdmwlWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * Simple implementation by
 * Created by dusanklinec on 15.11.15.
 */
public class AdmwlWorkerImpl<T> implements AdmwlWorker<T> {
    private static final Logger logger = LoggerFactory.getLogger(AdmwlWorkerImpl.class);
    private static final long serialVersionUID = 1L;

    /**
     * Adml provider from RMI.
     * Looked up via RMI service, calls getJob on it.
     */
    private AdmwlProvider<T> provider;

    /**
     * Our worker ID.
     */
    private final String workerId = UUID.randomUUID().toString();

    /**
     * Atomic boolean set to yes if worker loops are running.
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    /**
     * Number of threads to use for computation.
     */
    private int threadCount = 1;

    /**
     * Executor service running task threads.
     */
    private ExecutorService executor;

    public AdmwlWorkerImpl(String host, String svc) throws RemoteException, MalformedURLException, NotBoundException {
        startWorker(host, -1, svc);
    }

    public AdmwlWorkerImpl(String host, int port, String svc) throws RemoteException, MalformedURLException, NotBoundException {
        startWorker(host, port, svc);
    }

    protected void startWorker(String host, int port, String svc) throws RemoteException, MalformedURLException, NotBoundException {
        // Install security manager.  This is only necessary
        // if the remote object's client stub does not reside
        // on the client machine (it resides on the server).
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        // Export ourselves so provider can invoke methods on us.
        final AdmwlWorker workerStub = (AdmwlWorker)UnicastRemoteObject.exportObject(this, 0);
        String connectURL = "rmi://"+host+"/" + svc;
        if (port > 0){
            connectURL = "rmi://"+host+":"+port+"/" + svc;
        }

        //Get a reference to the remote server on this machine
        provider = (AdmwlProvider<T>) Naming.lookup(connectURL);
        logger.info("Provider looked up successfully");

        // Register to the manager.
        provider.registerWorker(workerId, workerStub);
    }

    protected void unregisterWorker(){
        // Unregister from the provider, if possible.
        // If shutdown was triggered by the server during shutdown sequence,
        // it is probably dead by now.
        try {
            if (provider != null) {
                provider.unregisterWorker(workerId);
            }
        }catch(Exception e){
            logger.error("Could not unregister from the provider, maybe it is dead", e);
        }
    }

    public void work(){
        if (threadCount <= 1){
            run();
            unregisterWorker();
            return;
        }

        executor = Executors.newFixedThreadPool(threadCount);
        for(int i=0; i<threadCount; i++){
            // Submit new anonymous worker polling manager queue.
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    AdmwlWorkerImpl.this.run();
                }
            });
        }

        // Wait until tasks are terminated.
        executor.shutdown();
        try {
            final boolean done = executor.awaitTermination(365 * 10, DAYS);

            logger.info("Waiting finished, success: {}", done);
        } catch (InterruptedException e) {
            logger.error("Computation interrupted", e);
        }

        // Unregister from the provider, if possible.
        unregisterWorker();
    }

    /**
     * Worker method.
     */
    public void run(){
        try {
            logger.info("Entering worker loop, waiting for jobs, my id is {}, thread {}", workerId, Thread.currentThread().getName());
            while (isRunning.get()) {
                try {
                    Thread.sleep(100);
                } catch(InterruptedException ie){
                    logger.info("Worker interrupted", ie);
                    break;
                }

                if (!isRunning.get() || provider == null || provider.shouldTerminate(workerId)) {
                    break;
                }

                final AdmwlTask<T> job = provider.getNewJob(workerId, 1000);
                if (job == null) {
                    continue;
                }

                // Cancellation & progress monitoring.
                final String taskId = job.getTaskId();
                final AdmwlCancellation cancellation = new AdmwlCancellation() {
                    @Override
                    public boolean isCancelled() {
                        try {
                            return provider.shouldCancel(workerId, taskId);
                        } catch (RemoteException e) {
                            ;
                        }

                        return false;
                    }
                };

                final AdmwlProgressMonitor progress = new AdmwlProgressMonitor() {
                    @Override
                    public boolean onAdmwlProgressed(double progress) {
                        try {
                            provider.jobProgress(workerId, taskId, progress);
                        } catch (RemoteException e) {
                            ;
                        }
                        return true;
                    }
                };

                T result = null;
                try {
                    logger.info("<job name={}>", taskId);
                    result = job.execute(cancellation, progress);
                    logger.info("</job name={}>", taskId);

                } catch (Exception ex) {
                    logger.error("Exception while evaluation a job", ex);
                }

                provider.jobFinished(workerId, job, result);
            }

            logger.info("Terminating worker {}, thread {}", workerId, Thread.currentThread().getName());

        } catch(Exception e){
            logger.error("Exception during worker run", e);
        }

        logger.info("Worker terminated {}, thread {}", workerId, Thread.currentThread().getName());
    }

    @Override
    public String getWorkerId() throws RemoteException {
        return workerId;
    }

    @Override
    public T executeTask(AdmwlTask<T> t) throws RemoteException {
        // TODO: implement
        return null;
    }

    @Override
    public String ping(long pingCtr) throws RemoteException {
        // TODO: implement.
        return workerId;
    }

    @Override
    public String cancelTask() throws RemoteException {
        // TODO: implement
        return null;
    }

    @Override
    public void shutdown() throws RemoteException {
        isRunning.set(false);
        logger.info("Shutting down worker {}", workerId);
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }
}
