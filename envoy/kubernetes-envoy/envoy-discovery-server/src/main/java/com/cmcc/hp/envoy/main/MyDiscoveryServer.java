package com.cmcc.hp.envoy.main;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Struct;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.JsonFormat;

import envoy.api.v2.Cds.Cluster;
import envoy.api.v2.Cds.Cluster.DiscoveryType;
import envoy.api.v2.Cds.Cluster.EdsClusterConfig;
import envoy.api.v2.Cds.Cluster.LbPolicy;
import envoy.api.v2.Eds.ClusterLoadAssignment;
import envoy.api.v2.Lds.Listener;
import envoy.api.v2.Rds.RouteConfiguration;
import envoy.api.v2.core.AddressOuterClass.Address;
import envoy.api.v2.core.AddressOuterClass.SocketAddress;
import envoy.api.v2.core.AddressOuterClass.SocketAddress.Protocol;
import envoy.api.v2.core.ConfigSourceOuterClass.AggregatedConfigSource;
import envoy.api.v2.core.ConfigSourceOuterClass.ConfigSource;
import envoy.api.v2.endpoint.EndpointOuterClass.Endpoint;
import envoy.api.v2.endpoint.EndpointOuterClass.LbEndpoint;
import envoy.api.v2.endpoint.EndpointOuterClass.LocalityLbEndpoints;
import envoy.api.v2.endpoint.EndpointOuterClass.LocalityLbEndpoints.Builder;
import envoy.api.v2.listener.Listener.Filter;
import envoy.api.v2.listener.Listener.FilterChain;
import envoy.api.v2.route.RouteOuterClass.Route;
import envoy.api.v2.route.RouteOuterClass.RouteAction;
import envoy.api.v2.route.RouteOuterClass.RouteMatch;
import envoy.api.v2.route.RouteOuterClass.VirtualHost;
import envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManagerOuterClass.HttpConnectionManager;
import envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManagerOuterClass.HttpConnectionManager.CodecType;
import envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManagerOuterClass.HttpFilter;
import envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManagerOuterClass.Rds;
import io.envoyproxy.controlplane.cache.SimpleCache;
import io.envoyproxy.controlplane.cache.Snapshot;
import io.envoyproxy.controlplane.server.DiscoveryServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;

public class MyDiscoveryServer {

	private static final String GROUP = "front-envoy";

	/**
	 * Example minimal xDS implementation using the java-control-plane lib.
	 *
	 * @param arg
	 *            command-line args
	 */

	private static Cluster buildClusterWithEndpoint(String clusterName, DiscoveryType discoveryType,
			List<EndpointInf> endPointInfs) {
		Cluster.Builder clBuidler = Cluster.newBuilder().setName(clusterName)
				.setConnectTimeout(Durations.fromSeconds(5)).setType(discoveryType).setLbPolicy(LbPolicy.ROUND_ROBIN);
		if (discoveryType == DiscoveryType.EDS) {
			throw new RuntimeException("Bad DiscoveryType :EDS ");
		}
		Builder lbEndpointsBuilder = LocalityLbEndpoints.newBuilder();

		for (EndpointInf endInf : endPointInfs) {
			lbEndpointsBuilder.addLbEndpoints(LbEndpoint.newBuilder()
					.setEndpoint(Endpoint.newBuilder()
							.setAddress(Address.newBuilder().setSocketAddress(SocketAddress.newBuilder()
									.setAddress(endInf.ip).setPortValue(endInf.port).setProtocol(Protocol.TCP))
									.build())));
		}
		return clBuidler.setLoadAssignment(
				ClusterLoadAssignment.newBuilder().setClusterName(clusterName).addEndpoints(lbEndpointsBuilder).build())
				.build();
	}

	private static Cluster buildClusterWithEDSEndpoint(String clusterName) {
		return Cluster.newBuilder().setName(clusterName).setConnectTimeout(Durations.fromSeconds(5))
				.setEdsClusterConfig(EdsClusterConfig.newBuilder()
						.setEdsConfig(
								ConfigSource.newBuilder().setAds(AggregatedConfigSource.getDefaultInstance()).build())
						.setServiceName(clusterName))
				.setType(DiscoveryType.EDS).build();
	}

	private static ClusterLoadAssignment buildLoadAssignment(String clusterName, List<EndpointInf> endPointInfs) {
		Builder lbEndpointsBuilder = LocalityLbEndpoints.newBuilder();

		for (EndpointInf endInf : endPointInfs) {
			lbEndpointsBuilder.addLbEndpoints(LbEndpoint.newBuilder()
					.setEndpoint(Endpoint.newBuilder().setAddress(Address
							.newBuilder().setSocketAddress(SocketAddress.newBuilder().setAddress(endInf.ip)
									.setPortValue(endInf.port).setProtocol(Protocol.TCP).setResolverName(endInf.ip))
							.build())));
		}

		return ClusterLoadAssignment.newBuilder().setClusterName(clusterName).addEndpoints(lbEndpointsBuilder).build();

	}

	private static Listener createListener(String listenerName, int port, String routeName) {
		HttpConnectionManager manager = HttpConnectionManager.newBuilder().setCodecType(CodecType.AUTO)
				.setStatPrefix("ingress_http")
				.setRds(Rds.newBuilder()
						.setConfigSource(
								ConfigSource.newBuilder().setAds(AggregatedConfigSource.getDefaultInstance()).build())
						.setRouteConfigName(routeName))
				.addHttpFilters(HttpFilter.newBuilder().setName("envoy.router")).build();

		return Listener.newBuilder().setName(listenerName)
				.setAddress(Address.newBuilder().setSocketAddress(
						SocketAddress.newBuilder().setAddress("0.0.0.0").setPortValue(port).setProtocol(Protocol.TCP)))
				.addFilterChains(FilterChain.newBuilder().addFilters(Filter.newBuilder()
						.setName("envoy.http_connection_manager").setConfig(messageAsStruct(manager))))
				.build();
	}

	/**
	 * Returns a new test route.
	 *
	 * @param routeName
	 *            name of the new route
	 * @param clusterName
	 *            name of the test cluster that is associated with this route
	 */
	public static RouteConfiguration createRoute(String routeName, VirtulHostInf virtualHostInfo) {

		VirtualHost.Builder builder = VirtualHost.newBuilder().setName(virtualHostInfo.name);
		builder.addAllDomains(virtualHostInfo.domains);
		for (RouteInf routeInf : virtualHostInfo.routeInfs) {
			builder.addRoutes(Route.newBuilder().setMatch(RouteMatch.newBuilder().setPrefix(routeInf.match))
					.setRoute(RouteAction.newBuilder().setCluster(routeInf.toCluster)));
		}

		return RouteConfiguration.newBuilder().setName(routeName).addVirtualHosts(builder).build();

	}

	private static Struct messageAsStruct(MessageOrBuilder message) {
		try {
			String json = JsonFormat.printer().preservingProtoFieldNames().print(message);

			Struct.Builder structBuilder = Struct.newBuilder();

			JsonFormat.parser().merge(json, structBuilder);

			return structBuilder.build();
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException("Failed to convert protobuf message to struct", e);
		}
	}

	public static void main(String[] arg) throws IOException, InterruptedException {

		SimpleCache<String> cache = new SimpleCache<>(node -> {
			System.out.println("node " + node.getId() + " cluster :" + node.getCluster());
			return GROUP;
		});

		List<EndpointInf> cluster1EndPointInfs = new LinkedList<EndpointInf>();
		cluster1EndPointInfs.add(new EndpointInf("service1", 80));

		List<EndpointInf> cluster2EndPointInfs = new LinkedList<EndpointInf>();
		cluster2EndPointInfs.add(new EndpointInf("service2", 80));

		VirtulHostInf httpVirtualHostInfo = new VirtulHostInf("vhost", "*");
		httpVirtualHostInfo.addRouteInf("/service/1", "service1");
		httpVirtualHostInfo.addRouteInf("/service/2", "service2");

		cache.setSnapshot(GROUP,
				Snapshot.create(
						ImmutableList.of(
								buildClusterWithEndpoint("service1", DiscoveryType.STRICT_DNS, cluster1EndPointInfs),
								buildClusterWithEndpoint("service2", DiscoveryType.STRICT_DNS, cluster2EndPointInfs)),
						ImmutableList.of(), ImmutableList.of(createListener("httplistner", 80, "http_local_route")),
						ImmutableList.of(createRoute("http_local_route", httpVirtualHostInfo)), "1"));

		DiscoveryServer discoveryServer = new DiscoveryServer(cache);

		ServerBuilder<NettyServerBuilder> builder = NettyServerBuilder.forPort(12345)
				.addService(discoveryServer.getAggregatedDiscoveryServiceImpl())
				.addService(discoveryServer.getClusterDiscoveryServiceImpl())
				.addService(discoveryServer.getEndpointDiscoveryServiceImpl())
				.addService(discoveryServer.getListenerDiscoveryServiceImpl())
				.addService(discoveryServer.getRouteDiscoveryServiceImpl());

		Server server = builder.build();
		server.start();

		System.out.println("Server has started on port " + server.getPort());
		Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
		// 测试Router信息变更，增加一个路由
		Thread.sleep(30000);
		httpVirtualHostInfo.addRouteInf("/service/3", "service3");
		cache.setSnapshot(GROUP,
				Snapshot.create(
						ImmutableList.of(
								buildClusterWithEndpoint("service1", DiscoveryType.STRICT_DNS, cluster1EndPointInfs),
								buildClusterWithEndpoint("service2", DiscoveryType.STRICT_DNS, cluster2EndPointInfs)),
						"1", ImmutableList.of(), "1",
						ImmutableList.of(createListener("httplistner", 80, "http_local_route")), "1",
						ImmutableList.of(createRoute("http_local_route", httpVirtualHostInfo)), "2"));
		server.awaitTermination();
	}
}
