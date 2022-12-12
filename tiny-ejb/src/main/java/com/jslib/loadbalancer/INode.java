package com.jslib.loadbalancer;

import com.jslib.loadbalancer.Node.State;

public interface INode {

	void acquire();

	long release();

	void incrementsErrorsCount();

	String getImplementationURL();

	State getState();

	int getPriority();

	int getUsageCount();

	long getUsageTime();

	int getErrorsCount();

}