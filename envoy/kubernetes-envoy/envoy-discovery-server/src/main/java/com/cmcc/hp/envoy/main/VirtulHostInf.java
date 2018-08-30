package com.cmcc.hp.envoy.main;

import java.util.LinkedList;
import java.util.List;

public class VirtulHostInf {
	public String name;
	public List<String> domains = new LinkedList<>();
	public List<RouteInf> routeInfs = new LinkedList<>();

	public VirtulHostInf(String name,String domain)
	{
		this.name=name;
		this.addDomain(domain);
	}
	public void addDomain(String domain) {
		this.domains.add(domain);
	}

	public void addRouteInf(String match, String toCluster) {
		this.routeInfs.add(new RouteInf(match, toCluster));
	}
}
