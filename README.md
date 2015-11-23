# javaRmiManagerWorker
Java RMI manager worker implementation for computation parallelization across multiple computers.

## Description
Java Remote Method Invocation is used for communication between manager and worker processes.

Simple worker manager pattern is implemented. Manager is one process, generating new jobs and submitting them for computation.
There are multiple workers connected to manager and polling its job queue.

Manager process creates jobs for computation, adding them to the internal job queue.
As worker process connects to the manager process, it registers itself. Registration allows provider to call
methods on the worker.

When worker has a free working thread available it asks manager for a new job for processing.
Progress monitoring and cancellation are supported. Progress is signalized from the task, through worker to the manager.
Same code flow is followed when checking for cancellation of given task.

When computation finishes, manager callback for job finished is called with job object and job result.

Each worker generates unique UUID after initialization. Under this UUID it is registered in the manager.

In order to test the code, use provided shell scripts to start one manager first. Then start multiple worker processes.
As manager finishes its work, it terminates all worker processes.

