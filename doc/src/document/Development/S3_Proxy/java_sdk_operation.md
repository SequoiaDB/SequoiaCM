这里介绍如何使用 S3 Java SDK 访问 SequoiaCM S3 接口。

## S3 Java SDK 依赖##

-  **通过 Maven 引入 SDK 依赖**

```lang-xml
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-s3</artifactId>
    <version>1.11.343</version>
</dependency>
```

##创建连接##
----

以下示例中的 accessKey 和 secretKey 可以通过 S3 服务管理工具 [refresh-accessKey][refresh-accessKey] 生成。

```lang-java
String accessKey="2W1H5GBU66KSKUY8DMPE";
String secretKey="cs5PUa3O0fWlPn6L8TiS2XatTDShI0sqhwf3SaPG";
// 填写网关地址
String endPoint = "http://scmServer:8080";


AWSCredentials credentials = new BasicAWSCredentials(accessKey,secretKey);
AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(endPoint, null);
AmazonS3 s3 = AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(endpointConfiguration)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
```

创建存储桶
----

在 test_ws 区域（工作区）中创建一个名为 bucket1 的桶

```lang-java
s3.createBucket(new CreateBucketRequest("bucket1", "test_ws"));
```

上传对象
----

从本地上传一个名为 "example.png" 的文件到存储桶中，并命名为 "objectname"

```lang-java
PutObjectRequest request = new PutObjectRequest(bucketName, objectName, file);
s3.putObject(request);
```

获取对象
----

从存储桶中获得对象内容，并将对象内容存储在本地文件中

```lang-java
String filePath = "example.png";
GetObjectRequest request = new GetObjectRequest("bucket11", "bucket11");
S3Object result = s3.getObject(request);
S3ObjectInputStream s3is = result.getObjectContent();
FileOutputStream fos = new FileOutputStream(new File(filePath));
byte[] read_buf = new byte[1024];
int readLen = 0;
while ((readLen = s3is.read(read_buf)) != -1) {
    fos.write(read_buf, 0, readLen);
}
s3is.close();
fos.close();
```

复制对象
----

复制源对象到目标对象

```lang-java
CopyObjectRequest request = new CopyObjectRequest(sourceBucket, sourceObject, destBucket, destObject);
CopyObjectResult result = s3.copyObject(request);
```

获取指定版本的对象
----

获取指定版本的对象，当不指定 versionId 时，获取最新版本的对象

```lang-java
GetObjectRequest request = new GetObjectRequest(bucketName, objectName, versionId);
S3Object object = s3.getObject(request);
```

查询桶内对象列表
----

查询存储桶中所有对象

```lang-java
ListObjectsV2Result result = s3.listObjectsV2(bucketName);
```

查询桶中所有版本
----

查询指定存储桶中的所有版本的对象信息，包括历史版本以及删除标记，当桶中版本记录过多，可以进行多次分批查询。

```lang-java
ListVersionsRequest request = new ListVersionsRequest()
                                  .withBucketName(bucketName);
VersionListing result = s3.listVersions(request);
if (result.isTruncated())
{
   result = s3.listNextBatchOfVersions(result);
}
```

删除对象
----

删除指定对象

版本功能未开启时，删除指定对象会直接将对象内容从系统中删除，版本功能开启后，删除操作会在系统中生成一个对象的删除标记，原对象内容会作为历史记录保留在系统中

```lang-java
s3.deleteObject(bucketName, objectName);
```

删除指定版本的对象
----

删除指定版本的对象，可以删除历史版本或删除标记，该操作会彻底删除系统中关于该版本的记录

```lang-java
s3.deleteVersion(bucketName, objectName, versionId);
```

删除桶
----

删除指定存储桶

```lang-java
s3.deleteBucket(bucketName);
```

[refresh-accesskey]:Maintainance/Tools/S3admin/refresh-accesskey.md