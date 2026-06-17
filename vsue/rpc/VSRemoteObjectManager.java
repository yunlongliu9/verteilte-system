package vsue.rpc;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class VSRemoteObjectManager {
    public  static String username = "Server";
    private String host;
    private int port;
    private boolean useBuggyConnections;
    private Map<Integer, Remote> remoteObjects = new HashMap<>();
    private Map<Long, Method> methodCache = new HashMap<>();
    private Map<Remote, Remote> stubCache = new HashMap<>();
    private int nextID = 1;
    private static VSRemoteObjectManager instance = null;

    private VSRemoteObjectManager(String host, int port, boolean useBuggyConnections) {
        this.host = host;
        this.port = port;
        this.useBuggyConnections = useBuggyConnections;
    }

    public static synchronized VSRemoteObjectManager getInstance() {
        if (instance == null) {
            throw new RuntimeException(
                    "VSRemoteObjectManager not initialized"
           );
        }
        return instance;
    }

    // Singleton
    public static synchronized VSRemoteObjectManager getInstance(String host, int port, boolean useBuggyConnections) {
        if (instance == null) {
            instance = new VSRemoteObjectManager(host, port, useBuggyConnections);
        }
        return instance;
    }

    public synchronized Remote  exportObject(Remote object) {
        return exportObject(object, false);
    }

    // Exports a remote object and returns a stub that can be used by clients to invoke methods on it.
    public synchronized Remote exportObject(Remote object, boolean reuseConnection) {
        if (stubCache.containsKey(object)) {
            return stubCache.get(object);
        }
        System.out.println("Exporting object: " + object.getClass().getName());
        int id = nextID++;
        remoteObjects.put(id, object);
        
        addToMethodCache(object, id);
        VSRemoteReference ref = new VSRemoteReference(host, port, id); // server host and portt
        VSInvocationHandler handler = new VSInvocationHandler(ref, reuseConnection, this.useBuggyConnections);

        List<Class<?>> interfaces = collectAllInterfaces(object.getClass());
        List<Class<?>> remoteInterfaces = filterRemoteInterfaces(interfaces);

        Remote stub = (Remote) Proxy.newProxyInstance(
                object.getClass().getClassLoader(),
                remoteInterfaces.toArray(new Class<?>[0]),
                handler);
        stubCache.put(object, stub);
        return stub;
    }

    // Collects all interfaces implemented by the class and its superclasses
    private List<Class<?>> collectAllInterfaces(Class<?> clazz) {
        List<Class<?>> interfaces = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            for (Class<?> iface : current.getInterfaces()) {
                if (!interfaces.contains(iface)) {
                    interfaces.add(iface);
                }
            }
            current = current.getSuperclass();
        }
        return interfaces;
    }

    // Filters the collected interfaces to only include those that extend Remote
    private List<Class<?>> filterRemoteInterfaces(List<Class<?>> interfaces) {
        List<Class<?>> remoteInterfaces = new ArrayList<>();
        for (Class<?> iface : interfaces) {
            if (Remote.class.isAssignableFrom(iface)) {
                remoteInterfaces.add(iface);
            }
        }
        return remoteInterfaces;
    }

    public synchronized Remote lookUpStub(Remote object) {
        return stubCache.get(object);
    }

    // Caches method signatures for a given remote object and its assigned ID
    public void addToMethodCache(Remote object, int objectID) {
        List<Class<?>> interfaces = collectAllInterfaces(object.getClass());
        List<Class<?>> remoteInterfaces = filterRemoteInterfaces(interfaces);
        for (Class<?> iface : remoteInterfaces) {
            for (Method method : iface.getMethods()) {
                long cacheKey = ((long) objectID << 32) | (method.toGenericString().hashCode() & 0xFFFFFFFFL);
                methodCache.put(cacheKey, method);
            }
        }
    }

    // Combines objectID and methodNameHash into a single long key for caching
    public Method getMethod(int objectID, int methodNameHash) {
        long cacheKey = ((long) objectID << 32) | (methodNameHash & 0xFFFFFFFFL);
        return methodCache.get(cacheKey);
    }

    // Executes the method on the remote object and returns the result
    public Object invokeMethod(int objectID, int methodNameHash, Object[] args) throws Exception{
        Remote object = remoteObjects.get(objectID);
        if (object == null) {
            throw new RuntimeException("Object not found");
        }
    
        Method method = getMethod(objectID, methodNameHash);
        try {
            // Wie InvocationHandler: wenn RemoteObjekt: stub zurückgeben.
            return method.invoke(object, args);
        } catch (InvocationTargetException e){
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        
        } catch (Exception e) {
            throw new RuntimeException("Method invocation failed", e);
        }
    }
    public String getHost() {
        return host;
    }
    public int getPort() {
        return port;
    }
}
