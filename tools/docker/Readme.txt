文件清单：
    scm-k8s-util-1.0.0.jar
    sequoiacm-cloud-adminserver-1.0.0.jar
    sequoiacm-cloud-authserver-1.0.0.jar
    sequoiacm-cloud-gateway-1.0.0.jar
    sequoiacm-cloud-servicecenter-1.0.0.jar
    sequoiacm-config-server-1.0.0.jar
    sequoiacm-contentserver-3.0.0.jar
    sequoiacm-schedule-server-1.0.0.jar
    
    多数据源依赖库
    ceph_s3
    ceph_swift
    hbase
    hdfs
    
将上述文件拷贝至本文件夹下，执行如下命令构建镜像：
$ docker build -t sequoiadb/sequoiacm:3.0.0  .

执行如下命令获得构建的镜像包
$ docker save  -o ./sequoiacm-image-3.0.0.tar  sequoiadb/sequoiacm:3.0.0 

    
