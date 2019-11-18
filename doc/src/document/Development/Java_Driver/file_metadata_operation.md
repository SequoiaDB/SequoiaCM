这里介绍如何使用 Java 驱动接口编写使用文件元数据功能的程序。为了简单起见，下面的示例不全部是完整的代码，只起示例性作用。 
    
更多查看 [Java API][java_api]。


##示例##
* 创建元数据模型

```lang-javascript
ScmSession session = ScmFactory.Session.createSession(
        new ScmConfigOption("scmserver:8080/rootsite", "test_user", "scmPassword"));
ScmWorkspace workspace = ScmFactory.Workspace.getWorkspace("test_ws", session);
// 创建“身份证”模型
ScmClass scmClass = ScmFactory.Class.createInstance(workspace, "Bank Card", "身份证模型");
```

* 创建元数据属性

```lang-javascript
ScmAttributeConf attrConf = new ScmAttributeConf()
        // 英文属性名，实际设置元数据时使用
        .setName("ID_NUM")
        // 中文属性名，可用于前端展示
        .setDisplayName("身份证号")
        // 描述信息
        .setDescription("身份证号")
        // 设为必填属性
        .setRequired(true)
        // 属性值为'String'类型
        .setType(AttributeType.STRING)
        // 检验规则：最大长度18
        .setCheckRule(new ScmStringRule(18));
ScmAttribute stringAttr = ScmFactory.Attribute.createInstance(workspace, attrConf);
```

>  **Note:**
> 
>  AttributeType 共有如下五种类型：
>	
>  * AttributeType.STRING  字符串类型，检验规则类：ScmStringRule
>
>  * AttributeType.INTEGER  整型，检验规则类：ScmIntegerRule
>
>  * AttributeType.DOUBLE  浮点型，检验规则类：ScmDoubleRule
>
>  * AttributeType.BOOLEAN  布尔型，不需指定检验规则，可使用字符串"true"/"false"表示（不区分大小写）。
>
>  * AttributeType.DATE  日期类型，不需指定检验规则，默认日期格式：yyyy-MM-dd-HH:mm:ss.SSS

* 模型关联/解除属性

```lang-javascript
// 根据classID获取模型
ScmClass scmClass = ScmFactory.Class.getInstance(workspace, classID);
// 根据attrID获取属性
ScmAttribute scmAttr = ScmFactory.Attribute.getInstance(workspace, attrID);
// 模型关联属性
scmClass.attachAttr(scmAttr.getId());
// 获取模型已关联的属性
List<ScmAttribute> attrs = scmClass.listAttrs();
System.out.println(attrs);
// 模型解除属性
scmClass.detachAttr(scmAttr.getId());
```
>  **Note:**
>
>  * 一个模型可关联多个属性，一个属性可以被多个模型关联


* 创建文件，设置元数据
	
```lang-javascript 
ScmFile file = ScmFactory.File.createInstance(workspace);
file.setFileName("scm-file");
// 使用上面例子创建的scmClass
ScmClassProperties properties = new ScmClassProperties(scmClass.getId().get());
properties.addProperty("ID_NUM", "613435199105687894");
file.setClassProperties(properties);
file.save();
```

[java_api]:api/java/html/index.html