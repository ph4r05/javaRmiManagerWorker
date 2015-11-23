package com.klinec.admwl;

/**
 * Simple utilities for ADMWL.
 *
 * Created by dusanklinec on 23.11.15.
 */
public class AdmwlUtils {
    public static void throwIfCancelled(AdmwlCancellation cancellation){
        if (cancellation == null){
            return;
        }

        if (cancellation.isCancelled()){
            throw new AdmwlOperationCanceledException();
        }
    }

    public static boolean isCancelled(AdmwlCancellation cancellation){
        return cancellation != null && cancellation.isCancelled();
    }

    public static void onProgressed(AdmwlProgressMonitor progressMonitor, double progress){
        if (progressMonitor == null){
            return;
        }

        progressMonitor.onAdmwlProgressed(progress);
    }
}
