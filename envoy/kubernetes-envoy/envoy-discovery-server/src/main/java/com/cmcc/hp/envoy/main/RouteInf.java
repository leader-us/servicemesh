package com.cmcc.hp.envoy.main;

public class RouteInf {
public String match;
public String toCluster;
public RouteInf(String match, String toCluster) {
	super();
	this.match = match;
	this.toCluster = toCluster;
}

}
