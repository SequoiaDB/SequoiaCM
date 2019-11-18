SequoiaCM
=================
SequoiaCM is a distributed enterprise content management system based on the Spring Cloud micro-services framework. It is mainly used for enterprises to store and manage massive unstructured data

Module
-----------------
    infrastructure       -- SequoiaCM common components 
    cloud                -- SequoiaCM cloud server components
    config-server        -- SequoiaCM config server
    schedule-server      -- SequoiaCM schedule server
    contentserver        -- SequoiaCM content server
    om-server            -- SequoiaCM om server
    dev.py               -- SequoiaCM development tool

Building
-----------------
* 1. Install SequoiaDB environment, get the source code of SequoiaCM_HF 

```lang-javascript
    git clone http://gitlab.sequoiadb.com/sequoiadb/sequoiacm.git
```
* 2. Compile Project by dev.py

```lang-javascript
    python dev.py --compile all
```
* 3. Initialize system by systme.py, install component services

Driver
-----------------
The java driver is the interface that SequoiaCM provides for external calls.

* 1. get sequoiacm-driver-3.0.0-release.tar.gz in contentserver-3.0.0-release.tar.gz
* 2. operate SequoiaCM System by the java driver 

