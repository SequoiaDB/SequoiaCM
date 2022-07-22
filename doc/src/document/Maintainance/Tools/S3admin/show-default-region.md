show-default-region 子命令提供查看 S3 服务当前默认区域的功能。

## 子命令选项 ##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--url      |     | 网关地址|是|
|--user     |-u   | 用户名|是|
|--password |-p   | 密码，不指定值表示采用交互式输入密码|是|

### 示例 ###

查看默认区域

   ```lang-javascript
   $  s3admin.sh show-default-region --user admin --password --url localhost:8080
   ```