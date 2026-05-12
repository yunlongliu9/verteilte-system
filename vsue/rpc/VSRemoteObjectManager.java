package vsue.rpc;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.rmi.registry.LocateRegistry;

public class VSRemoteObjectManager  {
    /*
     * export + invoke; 
    * server and client can use this;
     */
    private Map<Integer, Object> objectRegistry = new HashMap<>();
    private static VSRemoteObjectManager instance =new VSRemoteObjectManager();
    private int nextObjectId = 1;
    private VSRemoteObjectManager() {
           
    }
    public static VSRemoteObjectManager getInstance() {
        return instance;
    }

    public Remote exportObject(Object obj,int port) throws Exception {
        int objectId = nextObjectId++;
        objectRegistry.put(objectId, obj);
        Remote stub =  (Remote) java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                obj.getClass().getInterfaces(),
                new VSInvocationHandler(new VSRemoteReference("localhost", port, objectId))
        );
        return stub;
    }

    public Object invokeMethod(int objectId, String methodName, Object[] parameters) throws Exception {
        Object obj = objectRegistry.get(objectId);
        if (obj == null) {
            throw new Exception("Object with ID " + objectId + " not found.");
        }
        Method method = findMethod(obj.getClass(), methodName, parameters);
        if (method == null) {
            throw new Exception("Method " + methodName + " not found in object with ID " + objectId);
        }
        return method.invoke(obj, parameters);
    }

    private Method findMethod(Class<?> clazz,String methodName,Object[] parameters) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    
}
