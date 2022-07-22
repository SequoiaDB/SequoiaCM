refresh-accesskey 子命令提供重置 SequoiaCM 用户 Accesskey 的功能。

## 子命令选项 ##

|选项       |缩写 |描述                                                   |是否必填|
|-----------|-----|---------------------------------------------------- |--------|
|--target-user| -t    | 需要重置 Accesskey 的 SequoiaCM 用户 |是|
|--user     |-u   | 用于登录验证的用户名，需为管理员用户或需要重置的用户|是|
|--password |-p   | 用于登录验证的密码，不指定值表示采用交互式输入密码|是|
|--s3-url   |-s   | S3 节点地址|

### 示例 ###

重置 user1 的 Accesskey

   ```lang-javascript
   $  s3admin.sh   refresh-accesskey --target-user user1 --user admin --password --s3-url localhost:16000
   ```