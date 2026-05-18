package vsue.faults;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import vsue.faults.VSRequestID;
import vsue.faults.ReplyMessage;
import vsue.rpc.VSRemoteReference;
import java.rmi.registry.LocateRegistry;

public class VSRemoteObjectManager  {
    /*
     * export + invoke; 
    * server and client can use this;
     */
    private Map<Integer, Object> objectRegistry = new HashMap<>();
    
    private static VSRemoteObjectManager instance =new VSRemoteObjectManager();
    private int nextObjectId = 1;
    private final Map<String, ReplyMessage> amoCache = Collections.synchronizedMap(
        new LinkedHashMap<String, ReplyMessage>() {
                @Override
                protected boolean removeEldestEntry(
                    Map.Entry<String, ReplyMessage> eldest
                ) {
                    return size() > 100;
                }
            }
        );
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

    public Object invokeMethod(int objectId, String methodName, Object[] parameters,VSRequestID requestID) throws Exception {
    
        Object obj = objectRegistry.get(objectId);
        if (obj == null) {
            throw new Exception("Object with ID " + objectId + " not found.");
        }
        Method method = findMethod(obj.getClass(), methodName, parameters);
        if (method == null) {
            throw new Exception("Method " + methodName + " not found in object with ID " + objectId);
        }
        VSRPCSemantic semantic = method.getAnnotation(VSRPCSemantic.class);
        if (semantic != null && semantic.value() == VSRPCSemanticType.AT_MOST_ONCE){
            String callID =  requestID.getCallID();
            if (amoCache.containsKey(callID)) {
                return amoCache
                    .get(callID)
                    .getResult();
            }else{
                Object result = method.invoke(obj, parameters);
                amoCache.put(
                        requestID.getCallID(),
                        new ReplyMessage(
                                result,
                                null,
                                requestID));
                return result;
            }
        } else {
            return method.invoke(obj, parameters);
        }
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
