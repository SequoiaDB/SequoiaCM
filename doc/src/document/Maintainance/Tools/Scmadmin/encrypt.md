encrypt 子命令提供加密明文密码的功能。

##子命令选项##

|选项       |缩写 |描述                                                                              |是否必填|
|-----------|-----|----------------------------------------------------------------------------------|--------|
|--user     |-u   |用户名                                                                            |是      |
|--password |-p   |明文密码，指定值则使用明文输入，不指定值则命令行提示输入                          |是      |

> **Note:**
>
> * SequoiaCM 3.0 使用 SequoiaDB、CephS3、CephSwift 作为数据源时，数据源密码参数为密码文件路径，而该密码文件的内容即为 encrypt 命令生成的密文字符串。

###示例###

生成一个密码的密文字符串

   ```lang-javascript
   $ scmadmin.sh encrypt -u adminUser -p
   password value:
   adminUser:encryptPassword
   
   $ echo "adminUser:encryptPassword" > /home/scmadmin/sdb.passwd
   ```

> **Note:**
> 
> * adminUser 为用户名，encryptPassword 为 encrypt 命令对密码加密后生成的密文字符串
> 
> * CephS3 生成密码的密文字符串时，用户名、密码分别为 accesskey、secretkey
> 
> * /home/scmadmin/sdb.passwd 为密码文件路径