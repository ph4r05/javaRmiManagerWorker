package com.klinec.admwl.demo;
import com.klinec.admwl.AdmwlCancellation;
import com.klinec.admwl.AdmwlProgressMonitor;
import com.klinec.admwl.remoteInterface.AdmwlTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single computation job.
 * Created by dusanklinec on 23.11.15.
 *
 */
public class ComputationJob implements AdmwlTask<ComputationResult> {
    private static final Logger logger = LoggerFactory.getLogger(ComputationJob.class);
    private static final long serialVersionUID = 1L;

    private String taskId;
    private int input;
    private ComputationProblem problem;

    public ComputationJob(String taskId, ComputationProblem problem, int input) {
        this.taskId = taskId;
        this.problem = problem;
        this.input = input;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public ComputationResult execute(AdmwlCancellation cancellation, AdmwlProgressMonitor progress) {
        final long curTime = System.currentTimeMillis();
        logger.info("<{} time={} thread={}>", taskId, curTime, Thread.currentThread().getName());

        // Execute the job
        final ComputationResult report = problem.compute(input, cancellation, progress);

        final long compTime = System.currentTimeMillis();
        logger.info("</{} time={} spent={} ms thread={}>", taskId, compTime, compTime - curTime, Thread.currentThread().getName());

        return report;
    }
}
