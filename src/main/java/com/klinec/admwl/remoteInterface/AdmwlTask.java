package com.klinec.admwl.remoteInterface;

import com.klinec.admwl.AdmwlCancellation;
import com.klinec.admwl.AdmwlProgressMonitor;

import java.io.Serializable;

/**
 * Basic computation task interface for RMI.
 *
 * Created by dusanklinec on 15.11.15.
 */
public interface AdmwlTask<T> extends Serializable {
    /**
     * Returns unique task identifier UUID.
     * @return task UUID.
     */
    String getTaskId();

    /**
     * Main work method;
     * @return task computation result.
     */
    T execute(AdmwlCancellation cancellation, AdmwlProgressMonitor progress);
}