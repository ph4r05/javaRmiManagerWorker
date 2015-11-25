package com.klinec.admwl.remoteInterface;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI interface for a worker.
 * If possible, worker client should pass this interface to the ADML provider so it can execute basic queries on the worker
 * or signalize cancellation or shutdown request.
 *
 * Created by dusanklinec on 15.11.15.
 */
public interface AdmwlWorker<Result> extends Remote {

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
     Result executeTask(AdmwlTask<Result> t) throws RemoteException;

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
     * @param taskId task identifier to cancel. If null, cancells all tasks being executed at the moment.
     * @throws RemoteException
     */
    String cancelTask(String taskId) throws RemoteException;

    /**
     * Worker should shut down after provider calls this method.
     *
     * @param cancelRunning if true cancels all currently running tasks. Task need to support cancellation mechanism.
     * @throws RemoteException
     */
    void shutdown(boolean cancelRunning) throws RemoteException;

}
