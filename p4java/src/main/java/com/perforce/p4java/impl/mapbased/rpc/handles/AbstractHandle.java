package com.perforce.p4java.impl.mapbased.rpc.handles;

import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;

public abstract class AbstractHandle {

    public AbstractHandle(CommandEnv.RpcHandler rpcHandler) {
        this.rpcHandler = rpcHandler;
        if (rpcHandler == null) {
            throw new NullPointerException("No handle provided");
        }
        if (rpcHandler.getType() == null) {
            rpcHandler.setType(getHandleType());
        } else if (!rpcHandler.getType().equals(getHandleType())) {
            throw new RuntimeException("Wrong handle type provided");
        }
    }
    
    public abstract String getHandleType();

    CommandEnv.RpcHandler rpcHandler;

}
