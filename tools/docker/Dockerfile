FROM java:8
ADD sequoiacm-cloud-servicecenter-1.0.0.jar /opt/sequoiacm/jars/service-center.jar
ADD sequoiacm-cloud-gateway-1.0.0.jar /opt/sequoiacm/jars/gateway.jar
ADD sequoiacm-cloud-authserver-1.0.0.jar /opt/sequoiacm/jars/auth-server.jar
ADD sequoiacm-cloud-adminserver-1.0.0.jar /opt/sequoiacm/jars/admin-server.jar

ADD sequoiacm-config-server-1.0.0.jar /opt/sequoiacm/jars/config-server.jar
ADD sequoiacm-schedule-server-1.0.0.jar  /opt/sequoiacm/jars/schedule-server.jar
ADD sequoiacm-contentserver-3.0.0.jar /opt/sequoiacm/jars/contentserver.jar

ADD scm-k8s-util-1.0.0.jar /opt/sequoiacm/jars/k8s-util.jar

ADD ceph_s3          /opt/sequoiacm/jars/ceph_s3
ADD ceph_swift       /opt/sequoiacm/jars/ceph_swift
ADD hbase            /opt/sequoiacm/jars/hbase
ADD hdfs             /opt/sequoiacm/jars/hdfs

ENV JAR_NAME ''
ENV EXEC_PARAM ''
ENV OPTIONS ''
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' > /etc/timezone
ENTRYPOINT ["sh", "-c", "java $EXEC_PARAM -jar  /opt/sequoiacm/jars/$JAR_NAME.jar $OPTIONS"]