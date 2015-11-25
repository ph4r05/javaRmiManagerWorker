package com.klinec.admwl.demo;

import com.klinec.admwl.*;
import com.klinec.admwl.remoteInterface.AdmwlTask;
import com.klinec.admwl.remoteLogic.AdmwlProviderImpl;
import com.klinec.admwl.remoteLogic.AdmwlRegistry;
import com.klinec.admwl.remoteLogic.AdmwlWorkerImpl;
import org.kohsuke.args4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class Main implements AdmwlOnJobFinishedListener<ComputationResult>,AdmwlOnJobProgressListener<ComputationResult>,AdmwlOnJobCancelCheckListener<ComputationResult> {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String SVCNAME = "ADMWL";

    // Receives other command line parameters than options
    @Argument
    private List<String> arguments = new ArrayList<String>(8);

    @Option(name="--worker", aliases={"-w"}, usage = "Start application in the RMI worker mode")
    private boolean workerMode = false;

    @Option(name="--worker-spawn", aliases={"-n"}, usage = "Number of workers to be spawned with the manager process. " +
            "Use only to start additional workers automatically with manager. Ignored in worker case.")
    private int workerSpawn = 0;

    @Option(name="--threads", aliases={"-t"}, usage = "Thread count for worker process.")
    private int threads = 1;

    @Option(name="--rmiregistry-connect", aliases={"-c"}, usage = "RMI registry to connect to. If specified, for server " +
            "registry is not created, this one is used")
    private String rmiRegistryConnect = null;

    @Option(name="--rmiregistry-only", aliases={"-o"}, usage = "Used only to start RMI registry and loop forever. Starts registry " +
            "on localhost.")
    private boolean rmiRegistryOnly = false;

    @Option(name="--rmiregistry-port", aliases={"-p"}, usage = "RMI registry port to use. If negative, the default one is used")
    private int rmiRegistryPort = -1;

    private AdmwlRegistry registry;
    private AdmwlProviderImpl<ComputationResult> manager;

    // Collecting results
    private final Queue<ComputationResult> taskResults = new ConcurrentLinkedQueue<>();
    private final AtomicInteger taskToProcess = new AtomicInteger(0);
    private final AtomicInteger taskProcessed = new AtomicInteger(0);

    /**
     * Command line arguments parser.
     */
    private CmdLineParser cmdLineParser;

    public static void main(String args[]) {
        logger.info("Starting the application");

        final Main app = new Main();
        app.startApplication(args);

        System.exit(0);
    }

    /**
     * Main entry point for starting the application.
     * @param args
     */
    protected void startApplication(String args[]){
        try {
            // Parse command line arguments
            parseArgs(args);

            // Standalone registry
            if (rmiRegistryOnly){
                if (!startRegistryAndWork()){
                    logger.error("Registry start failed");
                    return;
                }
                System.exit(0);
            }

            // Worker
            if (workerMode){
                workerLogic();
                return;
            }

            // Manager
            managerLogic();
            return;

        } catch (CmdLineException e) {
            logger.error("Exception during parsing command line arguments");
            cmdLineParser.printUsage(System.err);

        } catch (AccessException | AccessControlException e){
            logger.error("Access exception", e);
            logger.error("Security or Access exception indicates you may have forgotten to" +
                    " you use -Djava.security.policy=java.policy for starting the program. Please check the policy");

        } catch (RemoteException e) {
            logger.error("RMI exception", e);

        } catch (Exception e){
            logger.error("General exception", e);
        }
    }

    /**
     * Acting as a worker.
     *
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    protected void workerLogic() throws RemoteException, NotBoundException, MalformedURLException {
        logger.info("Starting in the worker mode");
        if (rmiRegistryConnect == null){
            rmiRegistryConnect = "localhost";
        }

        AdmwlWorkerImpl<ComputationResult> worker = new AdmwlWorkerImpl<>(rmiRegistryConnect, rmiRegistryPort, SVCNAME);
        worker.setThreadCount(threads);

        // Starts worker loops, blocks until worker is running.
        worker.work();
        System.exit(0);
    }

    /**
     * Acting as a manager.
     *
     * @throws RemoteException
     */
    protected void managerLogic() throws RemoteException {
        registry = new AdmwlRegistry();

        // If registry hostname to connect is not null, do not create own but use existing
        if (rmiRegistryConnect != null && !rmiRegistryConnect.isEmpty()){
            logger.info("Connecting to the RMI registry at {}, port: {}",
                    rmiRegistryConnect, rmiRegistryPort <= 0 ? "Default" : rmiRegistryPort);

            registry.lookupRegistry(rmiRegistryConnect, rmiRegistryPort);

        } else {
            // Create a new local host registry.
            logger.info("Creating new local RMI registry, port: {}", rmiRegistryPort <= 0 ? "Default" : rmiRegistryPort);
            registry.createRegistry(rmiRegistryPort);

        }

        // Create a new manager.
        logger.info("Binding RMI server");
        manager = new AdmwlProviderImpl<>(SVCNAME);
        manager.setRegistry(registry.getRegistry());
        manager.setJobFinishedListener(this);
        manager.setJobProgressListener(this);
        manager.setJobCancelCheckListener(this);
        manager.initServer();

        // Spawn additional workers?
        spawnWorkers();

        // Add some computation jobs.
        taskToProcess.set(0);
        taskProcessed.set(0);
        for(int i=0; i<30; i++){
            final ComputationProblem computationProblem = new ComputationProblem();
            final ComputationJob computationJob = new ComputationJob("job_" + i, computationProblem, i);

            taskToProcess.incrementAndGet();
            manager.enqueueJob(computationJob);
        }

        // Wait until job is done.
        waitServerTasksFinished();

        // Finished, shutdown workers.
        manager.shutdown();
        registry.shutdown();
    }

    /**
     * Spawn additional workers if enabled in options.
     */
    private void spawnWorkers(){
        if (workerSpawn <= 0){
            return;
        }

        final File f = new File(System.getProperty("java.class.path"));
        final File dir = f.getAbsoluteFile().getParentFile();
        final String path = dir.toString();
        logger.info("Class path {}", path);

        for (int i = 0; i < workerSpawn; i++) {
            try {
                logger.info("Starting worker process {}/{}", i+1, workerSpawn);

                ProcessBuilder pb = new ProcessBuilder(
                        "java",
                        "-Djava.security.policy="+path+"/../java.policy",
                        "-jar",
                        f.getAbsolutePath(),
                        "--worker");

                Process p = pb.start();

            } catch (Exception e) {
                logger.error("Exception in starting a worker", e);
            }
        }
    }

    /**
     * Blocks until all tasks are finished using RMI server.
     *
     */
    private void waitServerTasksFinished(){
        while(true){
            if (taskToProcess.get() <= taskProcessed.get()){
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("Interrupted", e);
                break;
            }

            manager.checkAllWorkers();
        }

        logger.info("Waiting finished, result size: {}", taskResults.size());
    }

    @Override
    public void onAdmwlJobFinished(AdmwlTask<ComputationResult> admwlTask, ComputationResult jobResult) {
        // Adding a null object to the concurrent queue causes freeze.
        if (jobResult != null) {
            taskResults.add(jobResult);
        }

        taskProcessed.incrementAndGet();
        logger.info("Job finished {}, resSize: {}, taskProcessed {}, result: {}",
                admwlTask.getTaskId(), taskResults.size(), taskProcessed.get(), jobResult);

        if (taskProcessed.get() == taskToProcess.get()){
            // When migrating to async, off the main queue, here trigger post-processing.
            logger.info("All jobs were processed");
        }
    }

    @Override
    public boolean onAdmwlJobProgressed(String workerId, String taskId, double progress) {
        logger.info("Progress {}/{}: {}", workerId, taskId, progress);
        return true;
    }

    @Override
    public boolean onAdmwlJobCancelCheck(String workerId, String taskId) {
        return Math.random() * 100 <= 1;
    }

    /**
     * Starts localhost standalone registry.
     */
    protected boolean startLocalRegistry(){
        registry = new AdmwlRegistry();
        try {
            logger.info("Starting local RMI registry, port: {}", rmiRegistryPort <= 0 ? "Default" : rmiRegistryPort);
            registry.createRegistry(rmiRegistryPort);
            return true;

        } catch (RemoteException e) {
            logger.error("Registry starting error");
        }

        return false;
    }

    /**
     * Starts standalone registry and blocks forever so registry can exists in standalone java process.
     */
    protected boolean startRegistryAndWork(){
        final boolean success = startLocalRegistry();
        if (!success){
            return false;
        }

        logger.info("Registry started, going to loop forever");
        while(true){
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.info("Registry loop interrupted");
                return true;
            }
        }
    }

    /**
     * Parse command line arguments, fills class attributes.
     * @param args argument list to parse
     */
    protected void parseArgs(String args[]) throws CmdLineException {
        // command line argument parser
        if (cmdLineParser == null) {
            cmdLineParser = new CmdLineParser(this);
        }

        cmdLineParser.parseArgument(args);
    }
}
