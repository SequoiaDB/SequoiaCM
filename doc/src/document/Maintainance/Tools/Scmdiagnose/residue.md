当前 SCM 系统中写入、迁移对象等操作同时会操作数据表（如 lob 表）和元数据表，当在业务过程中出现节点失败等异常情况时，可能会出现数据残留，存在无用数据的问题。

residue 子命令提供检测集群残留数据的功能。

##子命令选项##

| 选项                 | 缩写  | 描述                                                 | 是否必填 |
|--------------------|-----|----------------------------------------------------|------|
| --work-path        |  -  | 指定检测结果的归档目录                   | 是    |
| --workspace        |  -  | 指定待检测文件所属的工作区                                    | 是    |
| --site             |  -  | 指定工作区对应的站点，目前仅支持指定数据源为 SequoiaDB 的站点 | 是    |
| --url              |  -  | 指定 SequoiaCM 集群的网关服务节点地址，格式为 `<IP>:<port>/<siteName>`         | 是    |
| --user             |  -  | 指定管理员用户名                                        | 是    |
| --passwd           |  -  | 指定管理员用户密码，取值为空时表示采用交互的方式输入密码                         | 是    |
| --data-table       |  -  | 指定待检测的数据表名，格式为 `<csName>.<clName>`<br>不指定该选项时，选项 --dataid-file-path 必填 | 否    |
| --dataid-file-path |  -  | 指定待检测的 dataId 清单文件路径<br>不指定该选项时，选项 --data-table 必填                     | 否    |
| --max              |  -  | 指定检测的最大数据量，默认值为 10000，取值范围为 [1,1000000]<br>如果数据量超出最大值，将检测失败 | 否    |
| --worker-count     |  -  | 指定检测任务的最大并发数，默认值为 1，取值范围为 [1,100]<br>用户需根据业务环境设置该选项，避免因取值过大影响系统性能   | 否    |

###示例###

1. 切换至工具所在路径，以 SequoiaCM 安装目录 `/opt/sequoiacm` 为例，执行语句如下：

    ```lang-bash
    $ cd /opt/sequoiacm/tools/sequoiacm-scm-diagnose
    ```

2. 编辑 dataId 清单文件，以 `/opt/data_id_list` 为例

   ```lang-bash
   $ vi /opt/data_id_list
   ```
   
   根据实际需求将需要检测的 dataId 以列表的形式写到 `/opt/data_id_list` 文件中

   ```lang-text
   6461f08e40000100136d6f61
   6461f08e40000100136d6f62
   6461f08e40000100136d6f63
   6461f08e40000100136d6f64
   6461f08e40000100136d6f65
   ...
   ```
   
3. 对 `/opt/data_id_list` 文件进行数据残留检测

    ```lang-bash
    $ ./bin/scmdiagnose.sh residue --work-path /opt/datacheck --workspace ws_default --site rootSite --url 192.168.17.183:8080/rootSite --user admin --passwd admin --dataid-file-path /opt/data_id_list
    ```

   检测完成后，结果将保存在归档目录下

    ```lang-text
    ...
    [16:41:38] Finish! workspace: ws_default, process: 8, success: 8, fail: 0, residue: 0, cost: 0 min 0 s 505 ms, the result in:/opt/datacheck/residue_result/2023-04-11-16-41-37/
    ```
4. 查看残留结果文件

   ```lang-bash
    $ vi /opt/datacheck/residue_result/2023-04-11-16-41-37/residue_list
   ```
   
   残留结果文件 `residue_list` 内容如下:

   ```lang-text
   6461f08e40000100136d6f61
   6461f08e40000100136d6f65
   ...
   ```

   >**Note:**
   >
   > * 残留结果文件只记录残留的数据。
   > 
   > * 残留结果文件中记录的残留数据可能有误，需进行再次确认！如在工具检测的过程中，新上传的文件，可能出现在写文件元数据到元数据表之前，被工具检测为残留数据。