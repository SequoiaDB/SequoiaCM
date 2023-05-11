srcSiteName=rootSite
targetSiteName=branchSite1

# metasource
metaSdbCoord=192.168.30.82:11810
metaSdbUser=sdbadmin
metaSdbPassword=sdbadmin
#metaSdbPasswordFile=../conf/pwd.txt

# connection pool
connectTimeout=10000
socketTimeout=0
maxConnectionNum=500
keepAliveTimeout=0

# transfer
# targetSiteUrl
url=192.168.30.82:8080/branchSite1
scmUser=admin
scmPassword=admin
batchSize=100
fileTransferTimeout=1800000
fileStatusCheckBatchSize=50
fileStatusCheckInterval=1000

#scmPasswordFile=../conf/pwd1.txt
#transferLogbackPath=../conf/transferLogback.xml

# clean
queueSize=1000
thread=20
datasourceConf=""
srcSitePasswordFile=/opt/sequoiacm/secret/ds1.pwd
# srcSitePassword=sdbadmin
# targetSiteInstances=30-81:15000,30-81:15010
#cleanLogbackPath=../conf/cleanLogback.xml

# clean zookeeper
zkUrls=192.168.30.82:2181
maxBuffer=1024
maxResidualTime=30000
maxChildNum=1000
cron="*/1 * * * *"

#start outFile
maxFileNumber=10
maxFileSize=100 #100MB
