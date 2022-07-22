这里介绍如何使用 Java 驱动接口编写使用 S3 服务管理功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。 
    
更多查看 [Java API][java_api]。


##示例##
* 默认区域设置和查看

```lang-javascript
// 将指定工作区设置为 S3 服务的默认 Region （工作区需要禁用目录功能）
ScmFactory.S3.setDefaultRegion(s, "wsName");
// 查看 S3 服务的默认 Region
ScmFactory.S3.getDefaultRegion(s);
```


[java_api]:api/java/html/index.html