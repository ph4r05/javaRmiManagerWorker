package com.klinec.admwl.demo;

import com.klinec.admwl.AdmwlCancellation;
import com.klinec.admwl.AdmwlProgressMonitor;
import com.klinec.admwl.AdmwlUtils;

import java.io.Serializable;

/**
 * Class that implements computation logic.
 * Created by dusanklinec on 23.11.15.
 */
public class ComputationProblem implements Serializable{
    private static final long serialVersionUID = 1L;

    /**
     * Faking some computation.
     *
     * @param input
     * @return
     */
    public ComputationResult compute(int input, AdmwlCancellation cancellation, AdmwlProgressMonitor progress){
        final int toSleepCycles = 10;
        try {
            for(int i=0; i<toSleepCycles; i++) {

                // Check if computation was cancelled.
                if (AdmwlUtils.isCancelled(cancellation)){
                    return new ComputationResult(-1);
                }

                // Report progress.
                AdmwlUtils.onProgressed(progress, (double)i/toSleepCycles);

                // "work"
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            ;
        }

        return new ComputationResult(input);
    }
}
