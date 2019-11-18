这里介绍如何使用 Java 驱动接口编写使用断点文件功能的程序。主要用于文件断点上传。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。

更多查看 [Java API][java_api]。

##示例##
* 上传断点文件

```lang-javascript
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);

// 创建断点文件:
// ScmChecksumType 表示数据校验方式
// 4 * 1024 * 1024 表示 4M 上传增量
ScmBreakpointFile breakFile = ScmFactory.BreakpointFile.createInstance(workspace, "test",
        ScmChecksumType.CRC32, 4 * 1024 * 1024);

// 断点文件上传：文件对象
File uploadFile = new File("E:/test/breakfile.txt");
breakFile.upload(uploadFile);
```
>  **Note:**
>
>  * ScmChecksumType 校验方式：CRC32、NONE、ADLER32 ，默认为 NONE
> 
>  * 上传增量 默认为 16M ，最小为 1M ，可自定义
> 
>  * upload接口 需要保证文件和流对象包括文件所有数据
> 
>  * upload接口 使用增量续传实现，检验已上传文件数据，对未上传数据按增量上传
> 
>  * incrementalUpload接口 输入流不能包含已上传的数据
> 
>  * incrementalUpload接口 最后一次增量上传，必须isLastContent设置为true，否则文件记录为不完整

* 上传断点文件转换成普通文件

```lang-javascript
ScmFile file = ScmFactory.File.createInstance(workspace);
file.setContent(breakFile);
file.setFileName("breakTest");
file.save();
```
>    **Note:**
>
>   *   断点文件转换成普通文件，必须保证断点文件的完整性
> 
>   *   断点文件转换成普通文件，断点文件元数据会被删除

[java_api]:api/java/html/index.html







