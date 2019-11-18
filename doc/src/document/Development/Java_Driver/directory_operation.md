这里介绍如何使用 Java 驱动接口编写使用目录功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。 
    
更多查看 [Java API][java_api]。


##示例##
* 创建目录

```lang-javascript
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);
// 根据路径创建目录： /a
ScmDirectory directory = ScmFactory.Directory.createInstance(workspace, "/a");
```
>  **Note:**
> 
>  * 创建目录时，父目录必须已经存在

* 获取目录，查询目录下的文件、目录

```lang-javascript
// 根据路径获取目录： /a
ScmDirectory directory = ScmFactory.Directory.getInstance(workspace, "/a");
// 查询该目下的文件（不会递归子目录查询）
ScmCursor<ScmDirectory> dirCursor = directory.listDirectories(null);
// 查询该目录下的子目录（不会递归子目录查询）
ScmCursor<ScmFileBasicInfo> fileCursor = directory.listFiles(null);
```

* 指定目录下创建文件，目录路径查询文件

```lang-javascript
// 获取目录
ScmDirectory directory = ScmFactory.Directory.getInstance(workspace, "/a");
// 创建文件
ScmFile file = ScmFactory.File.createInstance(workspace);
file.setFileName("subFile");
// 指定文件的父目录，不指定则为根目录
file.setDirectory(directory);
file.save();
// 按路径获取文件
file = ScmFactory.File.getInstanceByPath(workspace, "/a/subFile");
```

[java_api]:api/java/html/index.html





