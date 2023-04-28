
本文档主要介绍通过 Java 驱动编写与[桶限额功能][quota_manage]相关的程序示例。下述示例仅供参考，详细使用方法可查看 [Java API][java_api]。

##示例##

- 开启限额

```lang-javascript
ScmEnableBucketQuotaConfig quotaConfig = ScmEnableBucketQuotaConfig.newBuilder("bucketName")
                .setMaxSize("100G") // 设置桶的最大存储容量，单位为 G 或 M，默认值为 -1，表示不限制桶的存储容量
                .setMaxObjects(100000) // 设置桶存储对象的最大数量，默认值为 -1，表示不限制桶存储对象的数量
                .build();
ScmFactory.Quota.enableBucketQuota(session, quotaConfig);
```

> **Note:**
>
> - 用户需拥桶所属工作区的所有权限（ALL）。
> - 设置限额后系统会自动触发额度同步，以统计当前桶已用的容量和对象数量。如果桶内的数据较多，统计时间也会相对较长，从而影响业务性能，因此在该情况下，建议用户在业务空闲时间段内开启限额。
> - 系统支持使用接口 setUsedQuota() 手动设置桶的已用额度信息，设置后则不会触发额度同步。

- 手动触发额度同步

```lang-javascript
ScmFactory.Quota.syncBucketQuota(session, "bucketName");
```

>**Note:**
>
> 用户需拥有桶所属工作区的所有权限（ALL）。

- 取消额度同步

```lang-javascript
ScmFactory.Quota.cancelSyncBucketQuota(session, "bucketName");
```

>**Note:**
>
> 用户需拥有桶所属工作区的所有权限（ALL）。

- 查看限额

```lang-javascript
ScmBucketQuotaInfo quotaInfo = ScmFactory.Quota.getBucketQuota(session, "bucketName");
System.out.println("enable: " + quotaInfo.isEnable());
System.out.println("maxObjects: " + quotaInfo.getMaxObjects());
System.out.println("maxSize: " + quotaInfo.getMaxSizeBytes());

// 开启限额后，可查看已用额度信息
System.out.println("usedObjects: " + quotaInfo.getUsedObjects());
System.out.println("usedSize: " + quotaInfo.getUsedSizeBytes());
// 获取同步状态，共有四种状态：syncing（同步中）、completed（同步完成）、canceled（同步取消）、failed（同步失败）
System.out.println("syncStatus: " + quotaInfo.getSyncStatus());
```

>  **Note:**
> 
> - 用户需拥有桶所属工作区的读权限（READ）。
> - 同步状态为 failed 时，用户需手动触发额度同步。
> - 如果在开启限额的过程中触发异常，系统会自动关闭限额，此时查看 enable 的取值为 false。在这种情况下，用户需排查异常后再次开启限额。

- 更新限额

```lang-javascript
ScmUpdateBucketQuotaConfig quotaConfig = ScmUpdateBucketQuotaConfig.newBuilder("bucketName")
                .setMaxSize("100G") // 设置最大存储容量为 100G
                .setMaxObjects(100000) // 设置最大对象数量为 10 万
                .build();
ScmFactory.Quota.updateBucketQuota(session, quotaConfig);
```

>**Note:**
>
> 用户需拥有桶所属工作区的所有权限（ALL）。

- 关闭限额

```lang-javascript
// 关闭限额后，桶将不再有额度上限，关闭后可以再次开启
ScmFactory.Quota.disableBucketQuota(session, quotaConfig);
```

>**Note:**
>
> 用户需拥有桶所属工作区的所有权限（ALL）。

[java_api]:api/java/html/index.html
[quota_manage]:Architecture/quota_manage.md