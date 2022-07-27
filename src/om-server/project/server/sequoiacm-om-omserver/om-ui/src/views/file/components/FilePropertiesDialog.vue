<template>
  <div>
    <!-- 文件属性展示对话框 -->
    <el-dialog
      title="json 查询示例"
      class="file-propreties-dialog"
      :visible.sync="fieldDialogVisible"
      width="800px">
      <el-table
        :data="isWorkspaceView ? filePropertiesData : filePropertiesInBucekt"
        max-height="500px"
        style="width: 100%">
        <el-table-column
          prop="field"
          label="属性"
          width="160">
        </el-table-column>
        <el-table-column
          prop="desc"
          label="说明"
          width="200">
        </el-table-column>
        <el-table-column
          prop="sample"
          label="示例">
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
export default {
  props: {
    isWorkspaceView: {
      type: Boolean,
      default: true
    }
  },
  data() {
    const fileId = {
      field: 'id',
      desc: '文件 ID',
      sample: '{ "id" : "62b4116c400001003009f2fd" }'
    }
    const fileName = {
      field: 'name',
      desc: '文件名',
      sample: '{ "name" : "scmFile" }'
    }
    const title = {
      field: 'title',
      desc: '文件标题',
      sample: '{ "title" : "a new file of SequoiaCM" }'
    }
    const createUser = {
      field: 'user',
      desc: '创建用户',
      sample: '{ "user" : "admin" }'
    }
    const createTime = {
      field: 'create_time',
      desc: '创建时间',
      sample: '{ "create_time" : { "$gte" : 946654000000 } }'
    }
    const updateUser = {
      field: 'update_user',
      desc: '更新用户',
      sample: '{ "update_user" : "admin" }'
    }
    const updateTime = {
      field: 'update_time',
      desc: '更新时间',
      sample: '{ "update_time" : { "$gte" : 946654000000 } }'
    }
    const majorVersion = {
      field: 'major_version',
      desc: '文件的最大主版本号',
      sample: '{ "major_version" : 1 }'
    }
    const minorVersion = {
      field: 'minor_version',
      desc: '最新主版本的最大次版本号',
      sample: '{ "minor_version" : 0 }'
    }
    const tags = {
      field: 'tags',
      desc: '标签',
      sample: '{ "tags": { "$in" : [ "tagA" ] } }'
    }
    const dataId = {
      field: 'data_id',
      desc: '文件内容 ID',
      sample: '{ "data_id" : "62b4116c400001003009f2fd" }'
    }
    const fileSize = {
      field: 'size',
      desc: '文件大小',
      sample: '{ "size" : { "$gte" : 1024 } }'
    }
    const mimeType = {
      field: 'mime_type',
      desc: '文件内容 MIME 类型',
      sample: '{ "mime_type" : "application/octet-stream" }'
    }
    const md5 = {
      field: 'md5',
      desc: '文件 MD5 值',
      sample: '{ "md5" : "ICy5YqxZB1uWSwcVLSNLcA==" }'
    }
    const eTag = {
      field: 'etag',
      desc: '文件 ETag',
      sample: '{ "etag" : "93eda0c1adb1b68be7c7139ca3bd5e51" }'
    }
    const bucketId = {
      field: 'bucket_id',
      desc: '桶 ID',
      sample: '{ "bucket_id" : 1 }'
    }
    const directoryId = {
      field: 'directory_id',
      desc: '目录 ID',
      sample: '{ "directory_id" : "000000000000000000000000" }'
    }
    const batchId = {
      field: 'batch_id',
      desc: '文件关联的批次 ID',
      sample: '{ "batch_id" : "62b4065340006600a74c5a50" }'
    }
    const classId = {
      field: 'class_id',
      desc: '文件关联的自定义模型 ID',
      sample: '{ "class_id" : "62b405df40006600a74c5a4e" }'
    }
    const classProperties = {
      field: 'class_properties',
      desc: '文件自定义属性',
      sample: '{ "class_properties.ID_NUM" : "613435199105687894" }'
    }
    const siteList = {
      field: 'site_list',
      desc: '存储站点列表',
      sample: '{ "site_list" : { "$elemMatch" : { "site_id" : 1 , "create_time" : { "$gt" : 946654000000 } } } }'
    }
    return{
      fieldDialogVisible: false,
      filePropertiesInBucekt : [ 
        fileId, 
        fileName, 
        createUser, 
        createTime,
        updateTime, 
        majorVersion, 
        minorVersion, 
        fileSize, 
        mimeType, 
        eTag 
      ],
      filePropertiesData: [ 
        fileId, 
        bucketId, 
        directoryId,
        batchId, 
        fileName,
        title, 
        createUser, 
        createTime, 
        updateUser, 
        updateTime, 
        majorVersion, 
        minorVersion, 
        tags,
        dataId, 
        fileSize, 
        mimeType, 
        eTag,
        classId,
        classProperties,
        siteList
      ]
    }
  },
  methods: {
    show() {
      this.fieldDialogVisible = true
    },
    close() {
      this.fieldDialogVisible = false
    }
  }
}
</script>
