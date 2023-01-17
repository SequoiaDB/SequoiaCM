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
// 15 * 1024 * 1024 表示 15M 上传增量
ScmBreakpointFileOption option = new ScmBreakpointFileOption();
// option.setChecksumType(ScmChecksumType.CRC32); // 设置数据校验方式为 CRC32，不设置默认不做校验
option.setBreakpointSize(15 * 1024 * 1024);
// BreakpointFileType.DIRECTED 表示数据不在驱动缓存
ScmBreakpointFile breakFile = ScmFactory.BreakpointFile.createInstance(workspace, "test", option,
                ScmType.BreakpointFileType.DIRECTED);

// 断点文件上传：文件对象
File uploadFile = new File("E:/test/breakfile.txt");
breakFile.upload(uploadFile);
```
>  **Note:**
>
>  * ScmChecksumType 校验方式：CRC32、NONE、ADLER32 ，默认为 NONE
> 
>  * 上传增量 默认为 15M ，最小为 1M ，可自定义，在 Ceph S3 站点上创建断点文件该参数需要是 5m 的整数倍）
> 
>  * upload接口 需要保证文件和流对象包括文件所有数据
> 
>  * upload接口 使用增量续传实现，检验已上传文件数据，对未上传数据按增量上传
> 
>  * incrementalUpload接口 输入流不能包含已上传的数据；在 Ceph S3 站点上创建断点文件时，该接口只能接受 5m 整数倍的数据量（最后一段除外），可以使用 BreakpointFileType.BUFFERED 类型的 ScmBreakpointFile 来规避这个限制，此类型下 incrementalUpload 接口内部将会缓存数据并延迟发送
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







