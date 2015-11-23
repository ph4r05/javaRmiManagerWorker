package com.klinec.admwl;

/**
 * Called when worker checks cancellation for given task in provider.
 *
 * Created by dusanklinec on 23.11.15.
 */
public interface AdmwlOnJobCancelCheckListener<T> {
    boolean onAdmwlJobCancelCheck(String workerId, String taskId);
}
