FROM openjdk:8-jdk-alpine
MAINTAINER Leader.us 719867650@qq.com
copy envoy-server_lib /envoy-server_lib
expose 12345
CMD ["java", "-cp", "/envoy-server_lib/*","com.cmcc.hp.envoy.main.MyDiscoveryServer"]