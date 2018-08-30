# servicemesh
Service Mesh study 

envoy学习
部署好kubernete环境，envoy目录下是入门例子
kubectl create -f myapp.yaml 
[root@kub-master kubernetes-envoy]# kubectl get svc 
NAME          CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
front-proxy   10.111.177.90    <nodes>       80:30082/TCP   7m
kubernetes    10.96.0.1        <none>        443/TCP        18m
service1      10.109.75.233    <none>        80/TCP         7m
service2      10.101.117.216   <none>        80/TCP         7m

front-proxy为网关，浏览器访问虚机IP地址，端口为30082，URL为http://虚机IP:30082/service/1 

页面显示：
Hello from behind Envoy (service 1)! hostname: service1 resolvedhostname: 10.32.0.4
说明Envoy正常
访问 http://虚机IP:30082/service/2
代理到 service2的地址
Hello from behind Envoy (service 2)! hostname: service2 resolvedhostname: 10.32.0.5

另外一个版本为Java Envoy Controller，动态下发Front proxy的配置信息，实现了一个迷你原型版版的istio系统
myapp-controller.yaml 为K8s工程文件，源码在git里。