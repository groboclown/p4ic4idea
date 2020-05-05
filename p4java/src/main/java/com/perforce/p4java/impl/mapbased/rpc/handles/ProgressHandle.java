package com.perforce.p4java.impl.mapbased.rpc.handles;

import com.perforce.p4java.impl.mapbased.rpc.CommandEnv.RpcHandler;

public class ProgressHandle extends AbstractHandle {

    public ProgressHandle(RpcHandler rpcHandler) {
        super(rpcHandler);
    }

    @Override
    public String getHandleType() {
        return "ProgressHandle";
    }

}
