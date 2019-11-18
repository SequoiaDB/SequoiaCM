这里介绍如何使用 Java 驱动接口编写使用批次功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。 
    
更多查看 [Java API][java_api]。


##示例##
* 创建批次

```lang-javascript
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);
// 创建批次"Batch"，并设置自定义标签
ScmBatch batch = ScmFactory.Batch.createInstance(workspace);
ScmTags tags = new ScmTags();
tags.addTag("tagKey", "tagValue");
batch.setName("Batch");
batch.setTags(tags);
batch.save();
```

* 获取批次

```lang-javascript 
// 通过 batchID 获取批次实例
ScmBatch batch = ScmFactory.Batch.getInstance(workspace, batchID);
System.out.println(batch.getName());
System.out.println(batch.getTags());

// 按匹配条件查询批次：查询 name 为 Batch 的文件
BSONObject condition = ScmQueryBuilder.start(ScmAttributeName.Batch.NAME).is("Batch").get();
ScmCursor<ScmBatchInfo> cursor = ScmFactory.Batch.listInstance(workspace, condition);
while (cursor.hasNext()) {
    ScmBatchInfo batchInfo = cursor.getNext();
    System.out.println(batchInfo.getName());
}
```

* 批次关联/解除文件

```lang-javascript
// 获取批次
ScmBatch batch = ScmFactory.Batch.getInstance(workspace, batchID);
// 创建文件
ScmFile file = ScmFactory.File.createInstance(workspace);
file.setFileName("SequoiaCM");
file.save();
// 批次关联文件
batch.attachFile(file.getFileId());
// 获取批次已关联的文件
List<ScmFile> files = batch.listFiles();
// 批次解除文件
batch.detachFile(file.getFileId());
```
>  **Note:**
>
>  * 关联的文件必须已存在
>  
>  * 文件只能关联到一个批次

* 删除批次
	
```lang-javascript 
//通过 batchID 删除批次
ScmFactory.Batch.deleteInstance(workspace, batchID);

//通过 ScmBatch 实例删除批次
ScmBatch batch = ScmFactory.Batch.getInstance(workspace, batchID);
batch.delete();
```
>  **Note:**
>
>  * 删除批次，批次关联的文件也会被删除

[java_api]:api/java/html/index.html
