package com.perforce.p4java.server.callback;

import java.util.ArrayList;
import java.util.HashMap;

import com.perforce.p4java.impl.mapbased.rpc.CommandEnv;

public interface IParallelCallback {

	boolean transmit(CommandEnv cmdEnv, int threads, HashMap<String, String> flags, ArrayList<String> args);

}
