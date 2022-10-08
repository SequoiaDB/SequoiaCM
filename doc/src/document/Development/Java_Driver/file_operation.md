这里介绍如何使用 Java 驱动接口编写使用文件功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。
    
更多查看 [Java API][java_api]。

##示例##

* 创建文件

```lang-javascript 
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);
// 创建文件
ScmFile file = ScmFactory.File.createInstance(workspace);
// 设置文件相关属性及内容
file.setFileName("testFile");
file.setAuthor("SequoiaCM");
file.setContent("E:\\test\\upload_file.txt");
// 保存文件并获取文件 Id ，根据此文件 Id 可以进行获取文件、下载文件、删除文件等操作
ScmId fileID = file.save();
```
> ScmId 类表示全局唯一标识（GUID，Globally Unique Identifier）值的不可变类，其内部主要是由一个24个数字或字母组成的字符串构成。其作用是作为某一资源的唯一标识，比如通过 ScmId 获取文件、批次、元数据模型、任务调度等实例。
>
> 以获取文件实例为例：
> 
> 在需要获取文件实例时，通过执行 ScmFactory.File.getInstance 方法并传入指定的工作区和目标文件的 ScmId 来获取文件实例。
> 
> 补充：ScmId 在保存文件时获取

* 获取文件

```lang-javascript 
// 通过 workspace 和 fileID 获取文件实例
ScmFile file = ScmFactory.File.getInstance(workspace, fileID);
// 如果没有 fileID，可以通过 目录+文件名 的方式获取文件实例，对应的参数形式为“目录/文件名”，创建文件时不指定目录的情况下，文件默认所在的目录为根目录“/”
// ScmFile file = ScmFactory.File.getInstanceByPath(workspace, "/testFile");
System.out.println(file.toString());

// 下载文件内容
file.getContent("E:\\test\\download_file.txt");
// 通过工作区、文件 ID 直接获取文件内容
InputStream fileData = ScmFactory.File.getInputStream(ws, fileID);

// 按匹配条件查询文件：查询所有 Author 为 SequoiaCM 的文件
BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.File.AUTHOR).is("SequoiaCM")
        .get();
ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(workspace,
        ScopeType.SCOPE_CURRENT, condition);
while (cursor.hasNext()) {
    ScmFileBasicInfo fileInfo = cursor.getNext();
    System.out.println(fileInfo.getFileName());
}
```
>  **Note:**
> 
>  listInstance 的参数 scopeType 共有如下三种类型：
> 
>  * ScopeType.SCOPE_ALL  在所有版本中查询文件
> 
>  * ScopeType.SCOPE_CURRENT 在当前版本中查询文件
> 
>  * ScopeType.SCOPE_HISTRORY 在历史版本中查询文件 

*	下载文件，跨站点读时不做缓存

```lang-javascript 
// 通过 workspace 和 fileID 获取文件实例
ScmFile file = ScmFactory.File.getInstance(workspace, fileID);
OutputStream os = null;
try {
    os = new FileOutputStream("E:\\test\\download_file_force_no_cache.txt");
    // 下载文件内容，指定跨站点读时不做缓存
    file.getContent(os, CommonDefine.ReadFileFlag.SCM_READ_FILE_FORCE_NO_CACHE);
}
finally {
    if (os != null) {
        os.close();
    }
}
```

> **Note:**
>
> * 假设文件存储于站点 A，驱动请求站点 B 下载此文件，站点 B 不缓存文件内容。若站点网络模型为星型，且 A、B 均为分站点，则需要主站点作为中继站点完成内容转发，此时主站点、分站点 B 都不会缓存文件内容。

*	文件输入流下载文件

```lang-javascript
//通过 workspace 和 fileID 获取文件实例
ScmFile file = ScmFactory.File.getInstance(workspace, fileID);
//创建 ScmInputStream 实例
ScmInputStream is = ScmFactory.File.createInputStream(InputStreamType.UNSEEKABLE, file);
//从 ScmInputStream 中读取数据，写至本地文件
OutputStream os = null;
try {
    os = new FileOutputStream("E:\\test\\in_download_file.txt");
    //如需使用seek，请在创建输入流时指定输入流类型为：InputStreamType.SEEKABLE
    //is.seek(SeekType.SCM_FILE_SEEK_SET, 1000);            
    is.read(os);
}
finally {
    is.close();
    if (os != null) {
        os.close();
    }
}
```
>**Note:**
> 
>  InputStreamType 支持如下两种类型：
> 
>  * SEEKABLE:创建一个支持 seek 操作的实例，允许从特定的位置开始读取文件内容，若读取的文件不在本地站点，创建实例后，第一次读取有效数据时，系统会将文件一次性全部缓存至本地站点
> 
>  * UNSEEKABLE:创建一个不支持 seek 操作的实例，若读取的文件不在本地站点，客户端每次调用实例的 read 方法时会将这部分数据缓存至本地站点

*	文件输出流上传文件

```lang-javascript
//创建 ScmFile 实例
ScmFile file = ScmFactory.File.createInstance(workspace);
//设置文件相关属性
file.setFileName("fileName");
file.setAuthor("SequoiaCM");
//创建 ScmOuputStream 实例
ScmOutputStream os = ScmFactory.File.createOutputStream(file);
//写入数据
FileInputStream is = null;
try {
    is = new FileInputStream("E:\\test\\out_upload_file.txt");
    byte[] buf = new byte[1024];
    while (true) {
        int len = is.read(buf, 0, 1024);
        if (len == -1) {
            break;
        }
        os.write(buf, 0, len);
    }
}
catch (Exception e) {
    //异常取消，放弃创建 ScmFile，并关闭 ScmOutputStream 
    os.cancel();
    throw e;
}
finally {
    if (is != null) {
        is.close();
    }
}
//提交创建 ScmFile，并关闭 ScmOutputStream  
os.commit();
//文件创建成功，scmFile 对象的 fileId 等属性已被设置
System.out.println(file.toString());
```

* 文件的查询条件优化

```lang-javascript 
// 以下示例试图查询文件：FileID 为 id1 或 id2 ，AUTHOR 为 SequoiaCM
ScmId id1 = new ScmId("598c207b00000200002000b3");
ScmId id2 = new ScmId("598c207b00000200002500b2");
java.util.List<String> idList = new ArrayList<>();
idList.add(id1.get());
idList.add(id2.get());
// 取出 id1、id2 创建的月份,并构建月份条件
List<String> createMonthList = new ArrayList<>();
createMonthList.add(ScmUtil.Id.getCreateMonth(id1));
createMonthList.add(ScmUtil.Id.getCreateMonth(id2));
// 组装id条件、AUTHOR条件，并加入月份条件优化本次查询
BSONObject cond = ScmQueryBuilder.start(ScmAttributeName.File.FILE_ID).in(idList)
        .and(ScmAttributeName.File.AUTHOR).is("SequoiaCM")
        .and(ScmAttributeName.File.CREATE_MONTH).in(createMonthList).get();
// 查询遍历
ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.File.listInstance(workspace,
        ScopeType.SCOPE_CURRENT, cond);
while (cursor.hasNext()) {
    System.out.println(cursor.getNext().getFileName());
}
```
>  **Note:**
> 
>  * 在上述示例的查询条件中之所以加入 CREATE_MONTH 字段的条件，是因为 SequoiaCM 的文件元数据是储存在 SequoiaDB 的主子表中的，而分表的 shardingKey 使用的是 CREATE_MONTH 字段，所以查询条件加入 CREATE_MONTH 能避免查询在所有子表上进行，从而提升查询效率
>
>  * 加入 CREATE_MONTH 条件仅起优化查询的作用，不加入该字段同样可以达到目的
>
>  * CREATE_MONTH 可以通过 ScmUtil.Id 中提供的接口从 ScmId 中获取

* 删除文件
	
```lang-javascript 
//通过 workspace 和 fileID 物理删除文件
ScmFactory.File.deleteInstance(workspace, fileID, true);
//通过 ScmFile 实例物理删除文件
ScmFile file = ScmFactory.File.getInstance(workspace, fileID);
file.delete(true);
```
>  **Note:**
>
>  * 删除接口的 isPhysical 参数为 true 时表示物理删除（删除该文件所有版本），false 表示在版本控制下删除（仅当文件处于桶下时可以执行该操作，根据桶的版本控制状态进行处理）;

* 异步缓存文件
	
```lang-javascript 
// 假设文件内容存储在另一个站点，利用异步缓存接口将文件内容缓存至 rootSite
// 获取到 rootSite 的 Session
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);   
// 缓存指定文件 ID 的文件内容至 rootSite
ScmFactory.File.asyncCache(workspace, fileID);
```

[java_api]:api/java/html/index.html