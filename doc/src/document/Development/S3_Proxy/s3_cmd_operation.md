这里介绍如何使用 S3 CMD 命令行工具访问 SequoiaCM S3 接口。

##s3 cmd 命令行工具安装##

1. 下载

```lang-bash
wget http://nchc.dl.sourceforge.net/project/s3tools/s3cmd/2.0.2/s3cmd-2.0.2.tar.gz
```

2. 安装

```lang-bash
tar -zxf s3cmd-2.0.2.tar.gz -C /usr/local/
mv /usr/local/s3cmd-2.0.2/ /usr/local/s3cmd/
ln -s /usr/local/s3cmd/s3cmd /usr/bin/s3cmd
```

##配置和连接##

配置 Access Key、Secret Key、S3 Endpoint、HTTPS protocol 和 DNS-style

```lang-text
$ ./s3cmd --configure

Enter new values or accept defaults in brackets with Enter.
Refer to user manual for detailed description of all options.

Access key and Secret key are your identifiers for Amazon S3. Leave them empty for using the env variables.
Access Key: 2W1H5GBU66KSKUY8DMPE
Secret Key: cs5PUa3O0fWlPn6L8TiS2XatTDShI0sqhwf3SaPG
Default Region [US]: test_ws

Use "s3.amazonaws.com" for S3 Endpoint and not modify it to the target Amazon S3.
S3 Endpoint [s3.amazonaws.com]: scmServer:8080 #填写网关地址

Use "%(bucket)s.s3.amazonaws.com" to the target Amazon S3. "%(bucket)s" and "%(location)s" vars can be used
if the target S3 system supports dns based buckets.
DNS-style bucket+hostname:port template for accessing a bucket [%(bucket)s.s3.amazonaws.com]: scmServer:8080/%(bucket)

Encryption password is used to protect your files from reading
by unauthorized persons while in transfer to S3
Encryption password: 
Path to GPG program [/usr/bin/gpg]: 

When using secure HTTPS protocol all communication with Amazon S3
servers is protected from 3rd party eavesdropping. This method is
slower than plain HTTP, and can only be proxied with Python 2.7 or newer
Use HTTPS protocol [Yes]: No

On some networks all internet access must go through a HTTP proxy.
Try setting it here if you can't connect to S3 directly
HTTP Proxy server name: 

New settings:
  Access Key: 2W1H5GBU66KSKUY8DMPE
  Secret Key: cs5PUa3O0fWlPn6L8TiS2XatTDShI0sqhwf3SaPG
  Default Region: test_ws
  S3 Endpoint: scmServer:8080
  DNS-style bucket+hostname:port template for accessing a bucket: scmServer:8080/%(bucket)
  Encryption password: 
  Path to GPG program: /usr/bin/gpg
  Use HTTPS protocol: False
  HTTP Proxy server name: 
  HTTP Proxy server port: 0

Test access with supplied credentials? [Y/n] 
Please wait, attempting to list all buckets...
Success. Your access key and secret key worked fine :-)

Now verifying that encryption works...
Not configured. Never mind.

Save settings? [y/N] y
Configuration saved to '/root/.s3cfg'
```


##操作桶和对象##

- 列举所有 bucket

```lang-bash
s3cmd ls
```

- 创建 bucket，且 bucket 名称是唯一的，不能重复

```lang-bash
s3cmd mb s3://my-bucket-name
```

- 删除空 bucket

```lang-bash
s3cmd rb s3://my-bucket-name
```

- 列举 bucket 中的内容

```lang-bash
s3cmd ls s3://my-bucket-name
```

- 上传 "file.txt" 到某个 bucket

```lang-bash
s3cmd put file.txt s3://my-bucket-name/file.txt
```

- 批量上传文件

```lang-bash
s3cmd put ./* s3://my-bucket-name/
```

- 下载文件

```lang-bash
s3cmd get s3://my-bucket-name/file.txt file.txt
```

- 批量下载

```lang-bash
s3cmd get s3://my-bucket-name/* ./
```

- 删除文件

```lang-bash
s3cmd del s3://my-bucket-name/file.txt
```

- 获取对应的 bucket 所占用的空间大小

```lang-bash
s3cmd du -H s3://my-bucket-name
```
