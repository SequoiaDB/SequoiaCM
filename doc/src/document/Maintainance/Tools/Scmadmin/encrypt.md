encrypt 子命令提供加密明文密码的功能。

##子命令选项##

|选项       |缩写 |描述                                                                              |是否必填|
|-----------|-----|----------------------------------------------------------------------------------|--------|
|--user     |-u   |用户名                                                                            |是      |
|--password |-p   |明文密码                                                                             |是      |                                                        
> **Note:**
>
> * SequoiaCM 3.0 使用 SequoiaDB、CephS3、CephSwift 作为数据源时，数据源密码参数为密码文件路径，而该密码文件的内容即为 encrypt 命令生成的密文字符串。

###示例###

生成一个密码的密文字符串，该密码所属的的用户名为 adminUser，密码明文为 adminPassword

   ```lang-javascript
   $ scmadmin.sh encrypt -u adminUser -p adminPassword
   ```

> **Note:**
>
> * 数据源相关的密码文件，其内容即为该命令的标准输出