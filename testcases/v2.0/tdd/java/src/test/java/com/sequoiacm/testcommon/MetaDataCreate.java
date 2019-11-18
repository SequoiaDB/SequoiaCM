package com.sequoiacm.testcommon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.sequoiadb.base.CollectionSpace;
import com.sequoiadb.base.DBCollection;
import com.sequoiadb.base.Sequoiadb;

public class MetaDataCreate {
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private  void insertIntoSdb(String ip_port,String csName,String clName,List<BSONObject> basonlist){
       Sequoiadb sdb = new Sequoiadb(ip_port, "", "");
       CollectionSpace cs = sdb.getCollectionSpace(csName);
       DBCollection cl = cs.getCollection(clName);
       cl.truncate();
       cl.insert(basonlist);
       sdb.close();
    }
    private  void truncateSdb(String ip_port,String csName,String clName){
        Sequoiadb sdb = new Sequoiadb(ip_port, "", "");
        CollectionSpace cs = sdb.getCollectionSpace(csName);
        DBCollection cl = cs.getCollection(clName);
        cl.truncate();
        sdb.close();
     }
    private  List<BSONObject> creatClassInfo(){
        List<BSONObject> classlist = new ArrayList<BSONObject>();
        BSONObject bsonClass = new BasicBSONObject();
        bsonClass.put("id", "20180424");
        bsonClass.put("name", "身份证ID");
        bsonClass.put("create_time",new Date().getTime());
        bsonClass.put("creater_user", "testUser");
        BSONObject bsonClass1 = new BasicBSONObject();
        bsonClass1.put("id", "20180425");
        bsonClass1.put("name", "社保卡");
        bsonClass1.put("create_time", new Date().getTime());
        bsonClass1.put("creater_user", "testUser");
        BSONObject bsonClass2 = new BasicBSONObject();
        bsonClass2.put("id", "20180426");
        bsonClass2.put("name", "护照");
        bsonClass2.put("create_time", new Date().getTime());
        bsonClass2.put("creater_user", "testUser");
        classlist.add(bsonClass);
        classlist.add(bsonClass1);
        classlist.add(bsonClass2);
        return classlist;
    }
    private  List<BSONObject> creatAttrInfo(){
        List<BSONObject> attrlist = new ArrayList<BSONObject>();
        BSONObject bsonClass = new BasicBSONObject();
        bsonClass.put("name", "ID_NUM");
        bsonClass.put("desc", "证件证号码");
        bsonClass.put("type", "STRING");
        BSONObject rules2 = new BasicBSONObject();
        rules2.put("maxLength", 18);
        bsonClass.put("check_rules", rules2);
        bsonClass.put("required",true);
        BSONObject bsonClass1 = new BasicBSONObject();
        bsonClass1.put("name", "FILE_TYPE");
        bsonClass1.put("desc", "文件类型");
        bsonClass1.put("type", "STRING");
        //bsonClass1.put("check_rules", "");
        bsonClass1.put("required",true);
        BSONObject bsonClass2 = new BasicBSONObject();
        bsonClass2.put("name", "ID_NAME");
        bsonClass2.put("desc", "证件名称");
        bsonClass2.put("type", "STRING");
        //bsonClass2.put("check_rules", "");
        bsonClass2.put("required",true);
        BSONObject bsonClass3 = new BasicBSONObject();
        bsonClass3.put("name", "ID_ADD");
        bsonClass3.put("desc", "家庭住址");
        bsonClass3.put("type", "STRING");
        bsonClass3.put("check_rules", "");
        bsonClass3.put("required",false);
        BSONObject bsonClass5 = new BasicBSONObject();
        bsonClass5.put("name", "DATE_BEGIN");
        bsonClass5.put("desc", "开始日期");
        bsonClass5.put("type", "DATE");
        //bsonClass5.put("check_rules", "");
        bsonClass5.put("required",false);
        BSONObject bsonClass6 = new BasicBSONObject();
        bsonClass6.put("name", "DATE_END");
        bsonClass6.put("desc", "结束日期");
        bsonClass6.put("type", "DATE");
        //bsonClass6.put("check_rules", "");
        bsonClass6.put("required",false);
        BSONObject bsonClass7 = new BasicBSONObject();
        bsonClass7.put("name", "TIME_NUM");
        bsonClass7.put("desc", "时间期限");
        bsonClass7.put("type", "INTEGER");
        BSONObject rules = new BasicBSONObject();
        rules.put("min", 0);
        rules.put("min", 100);
        bsonClass7.put("check_rules", rules);
        bsonClass7.put("required",false);
        BSONObject bsonClass8 = new BasicBSONObject();
        bsonClass8.put("name", "HANDER_PRICE");
        bsonClass8.put("desc", "手工费");
        bsonClass8.put("type", "DOUBLE");
        BSONObject rules1 = new BasicBSONObject();
        rules1.put("min", 0.0);
        rules1.put("min", 10000.00);
        bsonClass8.put("check_rules", rules1);
        bsonClass8.put("required",false);
        attrlist.add(bsonClass);
        attrlist.add(bsonClass1);
        attrlist.add(bsonClass2);
        attrlist.add(bsonClass3);
        attrlist.add(bsonClass5);
        attrlist.add(bsonClass6);
        attrlist.add(bsonClass7);
        attrlist.add(bsonClass8);
        return attrlist;
    }
    
    private  List<BSONObject> creatClassAttrRel(String classIdArray){
        List<BSONObject> attrlist = new ArrayList<BSONObject>();
        String[] classArrray = classIdArray.split(",");
        for(int i=0;i<classArrray.length;i++){
            String classId = classArrray[i]; 
            BSONObject bsonClass = new BasicBSONObject();
            bsonClass.put("class_id", classId);
            bsonClass.put("attr_name", "ID_NUM");
            BSONObject bsonClass1 = new BasicBSONObject();
            bsonClass1.put("class_id", classId);
            bsonClass1.put("attr_name", "FILE_TYPE");
            BSONObject bsonClass2 = new BasicBSONObject();
            bsonClass2.put("class_id", classId);
            bsonClass2.put("attr_name", "ID_NAME");
            BSONObject bsonClass3 = new BasicBSONObject();
            bsonClass3.put("class_id", classId);
            bsonClass3.put("attr_name", "ID_ADD");
            BSONObject bsonClass5 = new BasicBSONObject();
            bsonClass5.put("class_id", classId);
            bsonClass5.put("attr_name", "DATE_BEGIN");
            BSONObject bsonClass6 = new BasicBSONObject();
            bsonClass6.put("class_id", classId);
            bsonClass6.put("attr_name", "DATE_END");
            BSONObject bsonClass7 = new BasicBSONObject();
            bsonClass7.put("class_id", classId);
            bsonClass7.put("attr_name", "TIME_NUM");
            BSONObject bsonClass8 = new BasicBSONObject();
            bsonClass8.put("class_id", classId);
            bsonClass8.put("attr_name", "HANDER_PRICE"); 
            attrlist.add(bsonClass);
            attrlist.add(bsonClass1);
            attrlist.add(bsonClass2);
            attrlist.add(bsonClass3);
            attrlist.add(bsonClass5);
            attrlist.add(bsonClass6);
            attrlist.add(bsonClass7);
            attrlist.add(bsonClass8);
        }
        
        return attrlist;
    }
    
    public  void createAndInsertMetaDate(String ip_prot,String wsName){
       // String workspaceName ="ws_default,ws_month,ws_none";
        String class_define ="CLASS_DEFINE";
        String  attr_define ="CLASS_ATTR_DEFINE";
        String  class_attr_definae ="CLASS_ATTR_REL";
        List<BSONObject>  classlist = creatClassInfo();
        List<BSONObject>  attrlist = creatAttrInfo();
        List<BSONObject>  classAttrlist = creatClassAttrRel("20180424,20180425,20180426");
            insertIntoSdb(ip_prot,wsName+"_META",class_define,classlist);
            insertIntoSdb(ip_prot,wsName+"_META",attr_define,attrlist);
            insertIntoSdb(ip_prot,wsName+"_META",class_attr_definae,classAttrlist); 
        
    }
    public void deleteMetaData(String ip_prot,String wsName){
        String class_define ="CLASS_DEFINE";
        String  attr_define ="CLASS_ATTR_DEFINE";
        String  class_attr_definae ="CLASS_ATTR_REL";
        truncateSdb(ip_prot,wsName+"_META",class_define);
        truncateSdb(ip_prot,wsName+"_META",attr_define);
        truncateSdb(ip_prot,wsName+"_META",class_attr_definae); 
        
    }
   

}
