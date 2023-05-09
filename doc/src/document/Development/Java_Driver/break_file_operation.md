这里介绍如何使用 Java 驱动接口编写使用断点文件功能的程序。主要用于文件断点上传。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。

更多查看 [Java API][java_api]。

##示例##

* 上传断点文件 : 文件流 ( upload 接口示例 )

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

// 断点文件上传：文件流
InputStream inputStream = null;
try {
    inputStream = getDataInputStream();
    try {
        breakFile.upload(inputStream);
    } catch (Exception e) {
        breakFile = ScmFactory.BreakpointFile.getInstance(workspace, "test");
        // 重新获取完整数据流
        inputStream = getDataInputStream();
        breakFile.upload(inputStream);
    }
} finally {
    if (inputStream != null) {
        inputStream.close();
    }
}
```
>  **Note:**
>
>  * ScmChecksumType 校验方式：CRC32、NONE、ADLER32 ，默认为 NONE
>
>  * setBreakpointSize 接口配置上传增量，默认为 15M ，最小为 1M（在 Ceph S3 站点上创建断点文件该参数需要是 5m 的整数倍）
>
>  * upload接口 上传支持文件对象或流对象（流对象需要包含文件所有的数据）
>
>  * upload接口 内部使用增量续传实现，检验并跳过已上传文件数据，再对未上传数据按增量上传
>
>  * upload接口 上传文件流出现异常时，需要获取完整数据流，重新执行上传，示例如上


* 上传断点文件( incrementalUpload接口示例 )

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
// 断点文件上传：文件流
long offset = 0;
while(true){
    // getPartOfDataInputStream 模拟用户获取数据的函数:
    // 返回用户文件指定偏移量之后的一部分数据, data.inputStream() 表示数据流，data.isEnd() 表示是否为最后一部分用户数据）
    data = getPartOfDataInputStream(offset);
    try {
        if(data.isEnd()){
            // 最后一片用户数据置位 isLastContent = true
            breakFile.incrementalUpload(data.inputStream(), true);
            break;
        }
        else {
            breakFile.incrementalUpload(data.inputStream(), false);
            offset = breakFile.getUploadSize();
        }
    } catch (Exception e) {
        // 出现异常需要重新获取断点文件，根据断点文件的 uploadSize 续传数据,
        // 极端异常场景下，断点文件会丢失已经上传的数据，即 breakFile.getUploadSize() 返回 0
        // 此时用户需要从头开始上传文件
        logger.warn("failed to incremental upload file, try again", e);
        breakFile = ScmFactory.BreakpointFile.getInstance(workspace, "test");
        offset = breakFile.getUploadSize();
    }
}
```

>  **Note:**
>
>  * incrementalUpload接口 输入流不能包含已上传的数据；在 Ceph S3 站点上创建断点文件时，该接口只能接受 5m 整数倍的数据量（最后一段除外），可以使用 BreakpointFileType.BUFFERED 类型的 ScmBreakpointFile 来规避这个限制，此类型下 incrementalUpload 接口内部将会缓存数据并延迟发送
>
>  * incrementalUpload接口 最后一次增量上传，必须isLastContent设置为true，否则文件记录为不完整；若无特殊要求，优先采用upload接口进行上传；
> 
>  * incrementalUpload接口 上传出现异常时，需要获取断点文件实际偏移量，跳过数据流已上传部分，重新执行上传，示例如上

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







