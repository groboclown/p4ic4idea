package com.perforce.p4java.impl.mapbased.rpc.handles;

import java.util.LinkedList;
import java.util.List;

import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;

public class ReconcileHandle extends AbstractHandle {

    private static final String RECONCILE_HANDLER_SKIP_ADD_KEY = "skipAdd";
    private static final String RECONCILE_DEL_COUNT_KEY = "delCount";
    
    public ReconcileHandle(RpcHandler rpcHandler) {
        super(rpcHandler);
    }

    @Override
    public String getHandleType() {
        return "ReconcileHandle";
    }

    
    @SuppressWarnings("unchecked")
    public List<String> getSkipFiles() {
        if (!rpcHandler.getMap().containsKey(RECONCILE_HANDLER_SKIP_ADD_KEY)) {
            List<String> list = new LinkedList<String>();
            rpcHandler.getMap().put(RECONCILE_HANDLER_SKIP_ADD_KEY, list);
            return list;
        }
        return (List<String>) rpcHandler.getMap().get(RECONCILE_HANDLER_SKIP_ADD_KEY);
    }

    public void incrementDelCount() {
        long delCount = 0;
        if (rpcHandler.getMap().containsKey(RECONCILE_DEL_COUNT_KEY)) {
            delCount = (long) rpcHandler.getMap().get(RECONCILE_DEL_COUNT_KEY);
        }
        delCount++;
        rpcHandler.getMap().put(RECONCILE_DEL_COUNT_KEY, delCount);
    }

    public long getDelCount() {
        if (rpcHandler.getMap().containsKey(RECONCILE_DEL_COUNT_KEY)) {
            return (long) rpcHandler.getMap().get(RECONCILE_DEL_COUNT_KEY);
        }
        return 0;
    }
}
