# servicemesh
Service Mesh study 

envoyѧϰ
�����kubernete������envoyĿ¼������������
kubectl create -f myapp.yaml 
[root@kub-master kubernetes-envoy]# kubectl get svc 
NAME          CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
front-proxy   10.111.177.90    <nodes>       80:30082/TCP   7m
kubernetes    10.96.0.1        <none>        443/TCP        18m
service1      10.109.75.233    <none>        80/TCP         7m
service2      10.101.117.216   <none>        80/TCP         7m

front-proxyΪ���أ�������������IP��ַ���˿�Ϊ30082��URLΪhttp://���IP:30082/service/1 

ҳ����ʾ��
Hello from behind Envoy (service 1)! hostname: service1 resolvedhostname: 10.32.0.4
˵��Envoy����
���� http://���IP:30082/service/2
���� service2�ĵ�ַ
Hello from behind Envoy (service 2)! hostname: service2 resolvedhostname: 10.32.0.5

����һ���汾ΪJava Envoy Controller����̬�·�Front proxy��������Ϣ��ʵ����һ������ԭ�Ͱ���istioϵͳ
myapp-controller.yaml ΪK8s�����ļ���Դ����git�