这里介绍如何使用 Java 驱动接口编写使用文件版本功能的程序。主要用于保存历史文件信息。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。

更多查看 [Java API][java_api]。

##示例##
* 增加文件版本

```lang-javascript
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);

// 创建文件
ScmFile file = ScmFactory.File.createInstance(workspace);
// 上传文件：生成文件版本，默认 major_version = 1,minor_version = 0
file.setContent("E:/test/version1.txt");
file.setFileName("test");
file.save();
System.out.println("create:dataId=" + file.getDataId() 
        + ",fileId=" + file.getFileId()
        + ",version=" + file.getMajorVersion() 
        + "." + file.getMinorVersion());

// 更新文件内容：增加文件主版本 major_version = 2,minor_version = 0
file.updateContent("E:/test/version2.txt");
System.out.println("update:dataId=" + file.getDataId() 
        + ",fileId=" + file.getFileId()
        + ",version=" + file.getMajorVersion() 
        + "." + file.getMinorVersion());
```
>  **Note:**
>
>  * 当桶开启版本控制时，桶下创建同名文件也会为已有文件新增版本，见[桶操作示例][bucket_operation]
>
>  * major_version 表示主版本号
> 
>  * minor_version 表示次版本号 
> 
>  * 更新文件内容 主版本号加 1，次版本号不变
>  
>  * 更新文件内容 文件元数据 ID 不变，文件内容 ID 改变，旧版本文件添加至历史表，新版文件更新至原有文件表
> 
>  * 删除文件 会删除所有版本文件

* 获取指定版本文件

```lang-javascript
//获取文件：根据版本号
ScmFile fileVersion2 = ScmFactory.File.getInstance(workspace, fileID, 2, 0);
System.out.println("get:dataId=" + fileVersion2.getDataId()
     + ",fileId=" + fileVersion2.getFileId() 
     + ",version=" + fileVersion2.getMajorVersion() 
     + "." + fileVersion2.getMinorVersion() 
     + ",size=" + fileVersion2.getSize());
```

* 查询指定版本类型文件

```lang-javascript
// 查询文件列表：根据ScopeType
ScmCursor<ScmFileBasicInfo> fileCursor = ScmFactory.File.listInstance(workspace,
        ScopeType.SCOPE_CURRENT,
        ScmQueryBuilder.start(ScmAttributeName.File.FILE_NAME).is("test").get());
while (fileCursor.hasNext()) {
    System.out.println("list:" + fileCursor.getNext());
}
fileCursor.close();
```
>  **Note:**
>
>  * ScopeType类型 SCOPE_CURRENT（当前最新版本）、SCOPE_HISTORY（历史版本）、SCOPE_ALL（所有版本）


[java_api]:api/java/html/index.html
[bucket_operation]:Development/Java_Driver/bucket_operation.md




