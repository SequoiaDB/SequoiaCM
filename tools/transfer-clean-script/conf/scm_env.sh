srcSiteId=1
targetSiteId=2

# metasource
metaSdbCoord=192.168.30.82:11810
metaSdbUser=sdbadmin
metaSdbPassword=sdbadmin

# transfer
# targetSiteUrl
url=192.168.30.82:8080/branchsite
targetSiteInstances=30-81:15000,30-81:15010
scmUser=admin
scmPassword=admin

# clean
queueSize=1000
thread=20
cleanSiteLobSdbCoord=192.168.30.82:11810,192.168.30.83:11810,192.168.30.84:11810
cleanSiteLobSdbUser=sdbadmin
cleanSiteLobSdbPassword=sdbadmin

# clean zookeeper
zkUrls=192.168.30.82:2181
maxBuffer=30
maxResidualTime=180000
maxChildNum=1000

