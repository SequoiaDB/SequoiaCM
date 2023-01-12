refresh-accesskey 子命令提供重置 SequoiaCM 用户 Accesskey 的功能。

## 子命令选项 ##

| 选项            |缩写 | 描述                                                       |是否必填|
|---------------|---|----------------------------------------------------------|--------|
| --target-user | -t  | 需要重置 Accesskey 的 SequoiaCM 用户                            |是|
| --user        |-u | 用于登录验证的用户名，需为管理员用户或需要重置的用户                               |是|
| --password    |-p | 用于登录验证的密码，不指定值表示采用交互式输入密码                                |是|
| --url         |   | 网关节点地址                                                   |
| --accesskey   |   | 用于指定需要重置 SequoiaCM 用户的 Accesskey                         |
| --secretkey   |   | 用于指定需要重置 SequoiaCM 用户的 Secretkey，不指定值表示采用交互式输入 Secretkey |

> **Note:**
>
> - 管理员用户不可重置其他管理员用户的 Accesskey
>
> - 没有指定 Accesskey 和 Secretkey 时，则由 SCM 生成 Accesskey 和 Secretkey
>
> - 不能指定已经被其他用户占用的 Accesskey

### 示例 ###

重置 user1 的 Accesskey 和 Secretkey

   ```lang-javascript
   $  s3admin.sh refresh-accesskey --target-user user1 --user admin --password --url localhost:8080
      password for admin: 
      username     accesskey             secretkey
      user1        K50ROMMY2IDJI3TB9WBZ  HTzCKUQnVw6iEYxcD6x5sQqqOz9ykFyGjj46cran
   ```

指定 user1 的 Accesskey 和 Secretkey

   ```lang-javascript
   $  s3admin.sh refresh-accesskey --target-user user1 --user admin --password --accesskey userAccesskey --secretkey --url localhost:8080
      password for admin: 
      secretkey for userAccesskey: 
      username     accesskey      secretkey
      user1        userAccesskey  userSecretkey
   ```