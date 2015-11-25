package com.klinec.admwl;

/**
 * Called when worker signalizes progress event to the provider.
 *
 * Created by dusanklinec on 23.11.15.
 */
public interface AdmwlOnJobProgressListener<T> {
    boolean onAdmwlJobProgressed(String workerId, String taskId, double progress);
}
