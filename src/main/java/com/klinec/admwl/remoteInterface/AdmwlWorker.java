package com.klinec.admwl.remoteInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI interface for a worker.
 * If possible, worker client should pass this interface to the ADML provider so it can execute basic queries on the worker
 * or signalize cancellation or shutdown request.
 *
 * Created by dusanklinec on 15.11.15.
 */
public interface AdmwlWorker<T> extends Remote {

    /**
     * Returns worker unique identifier UUID.
     * @return worker UUID
     * @throws RemoteException
     */
    String getWorkerId() throws RemoteException;

    /**
     * Synchronous execution of the task.
     *
     * @param t
     * @return
     * @throws RemoteException
     */
     T executeTask(AdmwlTask<T> t) throws RemoteException;

    /**
     * Tests availability of the client.
     *
     * @param pingCtr ping sequence counter
     * @return String arbitrary non-null string if client lives
     * @throws RemoteException
     */
    String ping(long pingCtr) throws RemoteException;

    /**
     * Cancels task being executed.
     * Returns task-id of the cancelled task or null if nothing was cancelled.
     *
     * @throws RemoteException
     */
    String cancelTask() throws RemoteException;

    /**
     * Worker should shut down after provider calls this method.
     *
     * @throws RemoteException
     */
    void shutdown() throws RemoteException;

}
