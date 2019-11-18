package com.sequoiacm.hbase.dataservice;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequoiacm.datasource.dataservice.ScmService;
import com.sequoiacm.datasource.metadata.HadoopSiteUrl;
import com.sequoiacm.datasource.metadata.ScmSiteUrl;
import com.sequoiacm.hbase.HbaseException;
import com.sequoiacm.hbase.dataoperation.HbaseCommonDefine;

public class HbaseDataService extends ScmService {
    private static final Logger logger = LoggerFactory.getLogger(HbaseDataService.class);
    private Configuration conf = null;
    private Map<String, String> dataConf = new HashMap<String, String>();

    public HbaseDataService(int siteId, ScmSiteUrl siteUrl) throws HbaseException {
        super(siteId, siteUrl);

        try {
            HadoopSiteUrl hbaseSiteUrl = (HadoopSiteUrl) siteUrl;
            dataConf = hbaseSiteUrl.getDataConf();
            parseConf();
        }
        catch (Exception e) {
            throw new HbaseException(
                    "create HbaseDataService failed:siteId=" + siteId + ",siteUrl=" + siteUrl, e);
        }

        checkHbase();
    }

    private void checkHbase() throws HbaseException {
        Connection conn = null;
        try {
            conn = getConnection();
            Admin admin = conn.getAdmin();
            admin.getClusterStatus();
        }
        catch (Exception e) {
            throw new HbaseException("failed to get hbase cluster status", e);
        }
        finally {
            closeConnection(conn);
        }

    }

    public Connection getConnection() throws HbaseException {
        try {
            return ConnectionFactory.createConnection(conf);
        }
        catch (Exception e) {
            throw new HbaseException(
                    "create hbase connection failed:siteId=" + siteId + ",conf=" + conf, e);
        }
    }

    private void parseConf() throws HbaseException {
        logger.info("parse hbase Configuration, dataConf=" + dataConf);
        conf = HBaseConfiguration.create();
        if (null != dataConf) {
            for (Map.Entry<String, String> entry : dataConf.entrySet()) {
                conf.set(entry.getKey(), entry.getValue());
            }
        }
        else {
            throw new HbaseException("create HbaseDataService failed:siteId=" + siteId + ",siteUrl="
                    + siteUrl + ", dataConf=" + dataConf);
        }
    }

    // public Connection getConnection(Configuration customConf) throws
    // HbaseException {
    // Configuration usedConf = null;
    // try {
    // // if has customConf,use customConf,else use default
    // if (customConf.size() > 0) {
    // Configuration completeCustomConf = HBaseConfiguration.create();
    // completeCustomConf.addResource(customConf);
    // completeCustomConf.set(HBASE_ZK_QUORUM, this.conf.get(HBASE_ZK_QUORUM));
    // logger.debug("use ws customConf to get Conn");
    // usedConf = completeCustomConf;
    // return ConnectionFactory.createConnection(completeCustomConf);
    // }
    // logger.debug("use site defaultConf to getConn");
    // usedConf = conf;
    // return ConnectionFactory.createConnection(conf);
    // }
    // catch (Exception e) {
    // throw new HbaseException("create hbase connection failed:siteId=" +
    // siteId + ",conf="
    // + usedConf, e);
    // }
    // }

    public boolean checkAndPut(Table table, byte[] row, byte[] family, byte[] qualifier,
            byte[] value, Put put) throws HbaseException {
        try {
            return table.checkAndPut(row, family, qualifier, value, put);
        }
        catch (TableNotFoundException e) {
            // for create new Table,if need
            throw new HbaseException(HbaseException.HBASE_ERROR_TABLE_NOTEXIST,
                    "check and put data failed:siteId=" + siteId + ",table=" + table.getName()
                    + ",rowId=" + Bytes.toString(row) + ",family=" + Bytes.toString(family)
                    + ",qualifier=" + Bytes.toString(qualifier),
                    e);
        }
        catch (Exception e) {
            throw new HbaseException("check and put data failed:siteId=" + siteId + ",table="
                    + table.getName() + ",rowId=" + Bytes.toString(row) + ",family="
                    + Bytes.toString(family) + ",qualifier=" + Bytes.toString(qualifier), e);
        }
    }

    public void put(Table table, Put put) throws HbaseException {
        try {
            table.put(put);
        }
        catch (TableNotFoundException e) {
            throw new HbaseException(HbaseException.HBASE_ERROR_TABLE_NOTEXIST,
                    "put data failed:siteId=" + siteId + ",table=" + table.getName(), e);
        }
        catch (Exception e) {
            throw new HbaseException(
                    "put data failed:siteId=" + siteId + ",table=" + table.getName(), e);
        }
    }

    public Table getTable(Connection con, String name) throws HbaseException {
        try {
            return con.getTable(TableName.valueOf(name));
        }
        catch (TableNotFoundException e) {
            throw new HbaseException(HbaseException.HBASE_ERROR_TABLE_NOTEXIST,
                    "get table failed:siteId=" + siteId + ",table=" + name, e);
        }
        catch (Exception e) {
            throw new HbaseException("get table failed:siteId=" + siteId + ",table=" + name, e);
        }
    }

    public boolean createTable(Connection con, String name) throws HbaseException {
        try {
            Admin admin = con.getAdmin();
            HTableDescriptor desc = new HTableDescriptor(TableName.valueOf(name));
            HColumnDescriptor hcdescMeta = new HColumnDescriptor(
                    Bytes.toBytes(HbaseCommonDefine.HBASE_META_FAMILY));
            HColumnDescriptor hcdescData = new HColumnDescriptor(
                    Bytes.toBytes(HbaseCommonDefine.HBASE_DATA_FAMILY));
            desc.addFamily(hcdescData);
            desc.addFamily(hcdescMeta);
            logger.info("creating table:tableName=" + name + ",columnFamily=["
                    + HbaseCommonDefine.HBASE_META_FAMILY + ","
                    + HbaseCommonDefine.HBASE_DATA_FAMILY + "]");
            admin.createTable(desc);
            return true;
        }
        catch (TableExistsException e) {
            // ignore. if table exist, we assume create table success
            return false;
        }
        catch (Exception e) {
            throw new HbaseException("create table failed:siteId=" + siteId + ",table=" + name, e);
        }
        // try {
        // return con.getTable(TableName.valueOf(name));
        // }
        // catch (Exception e) {
        // throw new HbaseException("get table failed:siteId=" + siteId +
        // ",table=" + name, e);
        // }

    }

    public void delete(Table table, byte[] row) throws HbaseException {
        Delete delete = null;
        try {
            delete = new Delete(row);
            table.delete(delete);
        }
        catch (Exception e) {
            throw new HbaseException("delete record failed:siteId=" + siteId + ",table="
                    + table.getName() + ",rowId=" + Bytes.toString(row), e);
        }
    }

    public Result get(Table table, Get get) throws HbaseException {
        try {
            return table.get(get);
        }
        catch (TableNotFoundException e) {
            throw new HbaseException(HbaseException.HBASE_ERROR_TABLE_NOTEXIST,
                    "table not found:siteId=" + siteId + ",table=" + table.getName(), e);
        }
        catch (Exception e) {
            throw new HbaseException("get result failed:siteId=" + siteId + ",tableName="
                    + table.getName() + ",get=" + get.toString(), e);
        }
    }

    public void closeConnection(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        }
        catch (Exception e) {
            logger.warn("close connection failed,siteId=" + siteId, e);
        }
    }

    @Override
    public void clear() {
        conf = null;
    }

    @Override
    public String getType() {
        return "hbase";
    }

    public void deleteTable(String tableName) throws HbaseException {
        Connection conn = null;
        try {
            conn = getConnection();
            Admin admin = conn.getAdmin();
            TableName table = TableName.valueOf(tableName);
            admin.disableTable(table);
            admin.deleteTable(table);
        }
        catch (TableNotFoundException e) {
            // ignore it
        }
        catch (Exception e) {
            throw new HbaseException("delete table failed:tableName=" + tableName, e);
        }
        finally {
            closeConnection(conn);
        }
    }

}
