这里介绍如何使用 Java 驱动接口编写使用桶功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。 
    
更多查看 [Java API][java_api]。


##示例##
* 创建桶

```lang-javascript
ScmWorkspace ws = ScmFactory.Workspace.getWorkspace("test_ws", session);
ScmBucket bucket = ScmFactory.Bucket.createBucket(ws, bucketName);
```
>  **Note:**
> 
>  * 工作区需要禁用目录，才能创建桶

* 获取桶

```lang-javascript
// 按名字获取指定桶
ScmBucket bucket = ScmFactory.Bucket.getBucket(session, bucketName);

// 列取指定工作区下，指定用户创建的桶
ScmCursor<ScmBucket> cursor = null;
try {
    cursor = ScmFactory.Bucket.listBucket(s, workspaceName, userName);
    while(cursor.hasNext()){
        System.out.println(cursor.getNext());
    }
}
finally {
    if (cursor != null) {
        cursor.close();
    }
}
```

* 桶的版本控制

```lang-javascript
ScmBucket bucket = ScmFactory.Bucket.getBucket(session, bucketName);
// 启用版本控制
bucket.enableVersionControl();
// 暂停版本控制
bucket.suspendVersionControl();
// 查看桶的版本控制
bucket.getVersionStatus();
```

* 桶下文件操作

```lang-javascript
ScmBucket bucket = ScmFactory.Bucket.getBucket(session, bucketName);

// 桶下创建文件，若桶开启版本控制，重复创建同名文件，将会为已有文件新建一个版本
ScmFile file = bucket.createFile("fileName");
file.setContent("./data");
file.save();

// 桶下获取文件（最新版本）
ScmFile file = bucket.getFile("fileName");


// 获取指定版本
ScmFile file = bucket.getFile("fileName", 1, 0);

// 获取 null 版本（该版本对应 S3 中 version id 为 null 的对象版本）
ScmFile file = bucket.getNullVersionFile("fileName");


// 物理删除文件（删除文件的所有版本，文件的所有相关数据都将被删除）
ScmFile file = bucket.getFile("fileName");
file.delete(true);

// 在版本控制下删除文件，根据桶的版本控制状态，将会有不同的表现：
// Disabled：与物理删除表现一致
// Suspended/Enabled：删除动作只是新增一个 DeletMarker 版本，历史版本仍然可以指定版本号进行访问
ScmFile file = bucket.getFile("fileName");
file.delete(false);
```

[java_api]:api/java/html/index.html
