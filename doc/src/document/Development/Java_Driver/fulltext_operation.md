这里介绍如何使用 Java 驱动接口编写使用全文索引功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。 
    
更多查看 [Java API][java_api]。


##示例##
* 创建全文索引

```lang-javascript
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);
BasicBSONObject fulltextMatcher = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_TITLE,
            "my-title");
// test_ws 工作区开启全文索引，对 TITLE 为 my-title 的文件建立全文索引，同时工作区后续创建文件时，对满足 fulltextMatcher 的文件异步建立全文索引
ScmFactory.Fulltext.createIndex(workspace,
        new ScmFulltextOption(fulltextMatcher, ScmFulltextMode.async));
// 查看已存在文件的索引建立进度
ScmFulltexInfo wsIndexInfo = ScmFactory.Fulltext.getIndexInfo(workspace);
System.out.println(wsIndexInfo.getJobInfo());
```

>  **Note：**
> 
>  * 全文检索服务依赖文件的 Mime Type 属性分辨文件类型，用户需要确保文件内容的格式与 Mime Type 属性一致
>  * 支持建立索引的文件类型请查看[全文检索服务章节][fulltext_server]

* 全文检索

```lang-javascript
// 检索文件内容中包含关键字 apple、peach，同时不包含关键字 grape 的文件，如果文件内容中包含关键字 banana，那么该文件的相关度会更高
ScmFulltextSimpleSearcher searcher = ScmFactory.Fulltext.simpleSeracher(workspace);
searcher.match("apple", "peach");
searcher.shouldMatch("banana");
searcher.notMatch("grape");

// 检索结果需要返回高亮内容
searcher.highlight(new ScmFulltextHighlightOption());

// 检索结果按相关度从大到小排序返回
ScmCursor<ScmFulltextSearchResult> c = searcher.search();
while(c.hasNext()) {
    System.out.println(c.getNext());
}
```

* 删除全文索引

```lang-javascript
ScmFactory.Fulltext.dropIndex(workspace);
```


* 修改全文索引

```lang-javascript
// 修改全文索引，对文件 TITLE 为 my-title2 的文件建立索引，同时工作区后续创建文件时，对满足索引条件的文件同步建立全文索引
BasicBSONObject fulltextMatcher = new BasicBSONObject(FieldName.FIELD_CLFILE_FILE_TITLE,
            "my-title2");
ScmFactory.Fulltext.alterIndex(workspace, new ScmFulltextModifiler().newFileCondition(fulltextMatcher)
                .newMode(ScmFulltextMode.sync));
```

[java_api]:api/java/html/index.html
[fulltext_server]:Architecture/Microservice/fulltext_server.md