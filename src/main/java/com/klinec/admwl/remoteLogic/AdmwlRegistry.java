package com.klinec.admwl.remoteLogic;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Registry create / locate helper.
 *
 * Created by dusanklinec on 16.11.15.
 */
public class AdmwlRegistry {
    private Registry registry;

    /**
     * Creates a new registry on the localhost and standard registry port.
     * Administrator access or firewall rule change may be required as socket binding operation is performed.
     *
     * @throws RemoteException
     */
    public Registry createRegistry() throws RemoteException {
        return createRegistry(Registry.REGISTRY_PORT);
    }

    /**
     * Creates a new registry on the localhost and given port.
     * Administrator access or firewall rule change may be required as socket binding operation is performed.
     *
     * @param port
     * @throws RemoteException
     */
    public Registry createRegistry(int port) throws RemoteException {
        // Starting our own registry so it has class definitions of our classes.
        // Starting a new registry may need to allow it on the local firewall
        // or to execute manager with administrator rights.
        registry = LocateRegistry.createRegistry(port <= 0 ? Registry.REGISTRY_PORT : port);
        return registry;
    }

    public void shutdown() {
        if (registry == null){
            return;
        }

        registry = null;
    }

    /**
     * Connects to existing registry on given host and port.
     * Registry has to be running when calling this.
     * It has to have access to remote object classes. They are either on classpath
     * or available on the http for fetch.
     *
     * @param host
     * @param port
     * @return
     * @throws RemoteException
     */
    public Registry lookupRegistry(String host, int port) throws RemoteException {
        registry = LocateRegistry.getRegistry(host, port <= 0 ? Registry.REGISTRY_PORT : port);
        return registry;
    }

    /**
     * Connects to existing registry on the given host with standard port.
     * Registry has to be running when calling this.
     * It has to have access to remote object classes. They are either on classpath
     * or available on the http for fetch.
     *
     * @return
     * @throws RemoteException
     */
    public Registry lookupRegistry(String host) throws RemoteException {
        registry = LocateRegistry.getRegistry(host, Registry.REGISTRY_PORT);
        return registry;
    }

    /**
     * Connects to existing registry on localhost on given port.
     * Registry has to be running when calling this.
     * It has to have access to remote object classes. They are either on classpath
     * or available on the http for fetch.
     *
     * @param port
     * @return
     * @throws RemoteException
     */
    public Registry lookupRegistry(int port) throws RemoteException {
        registry = LocateRegistry.getRegistry("localhost", port <= 0 ? Registry.REGISTRY_PORT : port);
        return registry;
    }

    /**
     * Connects to existing registry on localhost on the standard port.
     * Registry has to be running when calling this.
     * It has to have access to remote object classes. They are either on classpath
     * or available on the http for fetch.
     *
     * @return
     * @throws RemoteException
     */
    public Registry lookupRegistry() throws RemoteException {
        registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT);
        return registry;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
}
