package com.klinec.admwl;

import com.klinec.admwl.remoteInterface.AdmwlTask;

/**
 * Called when task finishes its computation.
 *
 * Created by dusanklinec on 15.11.15.
 */
public interface AdmwlOnJobFinishedListener<T> {
    void onAdmwlJobFinished(AdmwlTask<T> admwlTask, T jobResult);
}
