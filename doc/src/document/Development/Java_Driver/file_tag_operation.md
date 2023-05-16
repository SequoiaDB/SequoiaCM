
本文档主要通过 Java 驱动接口编写与标签检索功能相关的程序示例。下述示例仅供参考，详细使用方法可查看 [Java API][java_api]。

##示例##

* 按标签检索文件

```lang-javascript
// 检索包含自由标签 key1=value1 的文件，且文件所属用户为 user1
ScmTagCondition tagCond = ScmTagConditionBuilder.builder().customTag()
		.contains("key1", "value1").build();
ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.Tag.searchFile(ws,
		ScmType.ScopeType.SCOPE_CURRENT, tagCond, new BasicBSONObject(ScmAttributeName.File.USER, "user1"), null, 0, -1);
try {
	while (cursor.hasNext()) {
		System.out.println(cursor.getNext());
	}
}
finally {
	cursor.close();
}

// 检索包含自由标签 key1=value* 的文件
ScmTagCondition tagCond = ScmTagConditionBuilder.builder().customTag()
         // 设置自由标签值不忽略大小写，启用通配
		.contains("key1", "value*", false, true).build();
ScmCursor<ScmFileBasicInfo> cursor = ScmFactory.Tag.searchFile(ws,
		ScmType.ScopeType.SCOPE_CURRENT, tagCond, new BasicBSONObject(ScmAttributeName.File.USER, "user1"), null, 0, -1);
...

// 检索包含 date-2022-*、date-2023-* 标签的文件，忽略大小写匹配
ScmTagCondition tagCond = ScmTagConditionBuilder.builder().tags().contains("date-2022-*", true, true)
			.contains("date-2023-*", true, true).build();
...

// 检索包含任意给定标签的文件 tag1 和 tag2
ScmTagCondition tagCond = ScmTagConditionBuilder.builder()
		.or(ScmTagConditionBuilder.builder().tags().contains("tag1").build(),
				ScmTagConditionBuilder.builder().tags().contains("tag2").build())
		.build();
...

// 通过 BSONObject 手动构造标签检索条件，检索包含 tag* 的文件，忽略大小写
ScmTagCondition tagCond = ScmTagConditionBuilder.builder()
		.fromBson((BSONObject) JSON.parse("{tags: {$contains: \"tag*\", $enable_wildcard: true, $ignore_case: true}}"));
...

```

> **Note:**
>
> - 标签检索支持使用通配符星号（*）和问号（?）进行模糊匹配，其中星号表示匹配任意个字符，问号表示匹配单个字符。
> - 如果标签中包含其他通配符，可使用转义符（\）对其进行转义。
> - 自由标签为键值对形式，目前仅支持对自由标签值进行模糊匹配。
> - 如果需要手动构造标签检索条件，可以参考[标签检索语法介绍][tag_search]。

* 查询标签库

```lang-javascript
// 获取指定标签
ScmTag tag = ScmFactory.Tag.getTag(ws, "tag1");
ScmCustomTag customTag = ScmFactory.CustomTag.getCustomTag(ws, "key1", "value1");

// 列取标签，支持使用通配符
ScmCursor<ScmTag> tagCursor = ScmFactory.Tag.listTag(ws, "tag*", 0, -1);
try {
	while (tagCursor.hasNext()) {
		System.out.println(tagCursor.getNext());
	}
}
finally {
	tagCursor.close();
}

// 列取自由标签
ScmCursor<ScmCustomTag> tagCursor = ScmFactory.CustomTag.listCustomTag(ws, "key*", "value*", 0, -1);
...
```

[java_api]:api/java/html/index.html
[tag_search]:Architecture/tag.md