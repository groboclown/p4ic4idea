package com.perforce.p4java.impl.mapbased.rpc.sys;

public enum RpcPerforceDigestType {
    MD5("md5"),
    GIT_TEXT("GitText"),
    GIT_BINARAY("GitBinary"),
    SHA256("sha256");

    public String rpcName;

    RpcPerforceDigestType(String rpcName) {
        this.rpcName = rpcName;
    }

    public static RpcPerforceDigestType GetType(String rpcName) {
        for( RpcPerforceDigestType t : RpcPerforceDigestType.values() ) {
            if(t.rpcName.equals(rpcName)) {
                return t;
            }
        }
        return MD5;
    }
}
