
SequoiaCM 的文件标签用于描述文件属性和简化文件的管理，以帮助用户更好地归类、整理和检索工作区下的文件。

##基本架构##

SequoiaCM 集群中的文件标签集中管理在标签库中，文件在内部存储时无需保存标签数据，只需关联标签库中的标签即可实现打标签。标签库在 SequoiaDB 中使用单独的集合进行存储，同时通过建立全文索引加速 SequoiaCM 内部对标签库的模糊查询，优化文件标签检索的性能。基本架构图如下：

![标签架构][tag_arch]

只有开启工作区的标签检索功能后，标签库集合才会建立全文索引。用户可以为工作区的标签库配置独立的 Domain，从而更灵活的管理 SequoiaDB 标签库全文索引。

> **Note:**
>
> 工作区标签库的默认 Domain 可在创建工作区时指定，也可通过[配置服务管理工具][update-global-config] 修改。

##标签类型##

SequoiaCM 的标签类型分为自由标签（Custom Tag）和多值标签（Tags），具体说明如下：

- 自由标签：自由标签是指键值对形式的标签属性，例如一个证件文件，用户可将“地区”、“证件类型”等作为自由标签键，“上海”、“身份证”等作为自由标签值。
- 多值标签：多值标签是指字符串形式的标签属性，例如一个文档文件，用户可将“人工智能”、“技术”等作为多值标签。

> **Note:**
>
> 文件的标签属性可同时包含一个或多个类型的标签。

##标签检索##

SequoiaCM 提供文件的标签检索功能，用户可通过 [OM 管理服务][OM]或[Java 驱动][Java]快速查找符合条件的文件。该功能依赖于 SequoiaDB 巨杉数据库的全文检索能力，用户需确保 SequoiaDB 为 v3.6.1 及以上版本。

##标签检索语法##

SequoiaCM 支持通过 JSON 格式指定更复杂的标签检索条件，支持的语法如下：

- 包含/不包含多值标签：检索包含/不包含多值标签的文件，支持使用通配符进行模糊匹配，支持指定是否忽略大小写
- 包含/不包含自由标签：检索包含/不包含自由标签的文件，支持使用通配符进行模糊匹配，支持指定是否忽略大小写
- 逻辑与：检索同时包含所有标签的文件
- 逻辑或：检索包含任意一个标签的文件

> **Note:**
>
> - 标签检索支持使用通配符星号（*）和问号（?）进行模糊匹配，其中星号表示匹配任意个字符，问号表示匹配单个字符。
> - 如果标签中包含其他通配符，可使用转义符（\）对其进行转义。

###包含/不包含多值标签###

**语法**

```lang-json
{tags: {$contains: <多值标签>, [$enable_wildcard: true|false], [$ignore_case: true|false}]}
{tags: {$not_contains: <多值标签>, [$enable_wildcard: true|false], [$ignore_case: true|false}]}
```

**示例**

- 检索包含多值标签 book 的文件

    ```lang-json
    {tags: {$contains: "book"}}
    ```

- 使用通配符进行模糊匹配，检索包含对应多值标签的文件

    ```lang-json
    {tags: {$contains: "date-2022-*", $enable_wildcard: true, $ignore_case: true}}
    ```

    > **Note:**
    >
    > - $enable_wildcard 默认值为 false，取值为 true 时表示开启通配。
    > - $ignore_case 默认值为 false，取值为 true 时表示忽略大小写。

- 使用通配符进行模糊匹配，检索不包含对应多值标签的文件

    ```lang-json
    {tags: {$not_contains: "date-2022-*", $enable_wildcard: true, $ignore_case: true}}
    ```

###包含/不包含自由标签###

**语法**

```lang-json
{custom_tag: {$contains: <key>:<value>, [$enable_wildcard: true|false], [$ignore_case: true]}}
{custom_tag: {$not_contains: <key>:<value>, [$enable_wildcard: true|false], [$ignore_case: true]}}
```

**示例**

- 检索包含自由标签 language: "chinese" 的文件

    ```lang-json
    {custom_tag: {$contains: {language: "chinese"}}
    ```

- 使用通配符进行模糊匹配，检索包含对应自由标签的文件

    ```lang-json
    {custom_tag: {$contains: {language: "chi*"}, $enable_wildcard: true, $ignore_case: true}
    ```

    > **Note:**
    >
    > - $enable_wildcard 默认值为 false，取值为 true 时表示开启通配。
    > - $ignore_case 默认值为 false，取值为 true 时表示忽略大小写。

- 使用通配符进行模糊匹配，检索不包含对应自由标签的文件

    ```lang-json
    {custom_tag: {$not_contains: {language: "chi*"}, $enable_wildcard: true, $ignore_case: true}
    ```

###逻辑与###

**语法**

```lang-json
{$and: [{tags: {$contains: <多值标签1>}}, {tags: {$contains: <key1>:<value1>}}, ...]}
```

**示例**

检索同时包含多值标签 book 和自由标签 language: "chinese" 的文件

```lang-json
{$and: [{tags: {$contains: "book"}}, {custom_tag: {$contains: {language: "chinese"}}]}
```

###逻辑或###

**语法**

```lang-json
{$or: [{tags: {$contains: <多值标签1>}}, {tags: {$contains: <key1>:<value1>}}, ...]}
```

**示例**

检索包含多值标签 book 或自由标签 language: "chinese" 的文件

```lang-json
{$or: [{tags: {$contains: "book"}}, {custom_tag: {$contains: {language: "chinese"}}]}
```

##限制##

- 自由标签键的最大长度限制 128 个字符，自由标签值的最大长度限制为 256 个字符。
- 文件关联的标签过多会导致文件操作性能下降，建议单个文件最多关联 1000 个标签。

[tag_arch]:Architecture/tag_arch.png
[update-global-config]:Maintainance/Tools/Confadmin/update-global-config.md
[OM]:Om/overview.md
[Java]:Development/Java_Driver/file_tag_operation.md