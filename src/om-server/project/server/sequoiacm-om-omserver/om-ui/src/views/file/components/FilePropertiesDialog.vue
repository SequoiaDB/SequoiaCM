<template>
  <div>
    <!-- 文件属性展示对话框 -->
    <el-dialog
      title="json 查询示例"
      class="file-propreties-dialog"
      :visible.sync="fieldDialogVisible"
      width="800px">
      <el-table
        :data="tableData"
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
  setup() {
  },
  data() {
    return{
      fieldDialogVisible: false,
      tableData: [{
        field: 'id',
        desc: '文件 ID',
        sample: '{ "id" : "62b4116c400001003009f2fd" }'
      }, {
        field: 'bucket_id',
        desc: '桶 ID',
        sample: '{ "bucket_id" : 1 }'
      }, {
        field: 'directory_id',
        desc: '父目录 ID',
        sample: '{ "directory_id" : "000000000000000000000000" }'
      }, {
        field: 'batch_id',
        desc: '文件关联的批次 ID',
        sample: '{ "batch_id" : "62b4065340006600a74c5a50" }'
      }, {
        field: 'name',
        desc: '文件名',
        sample: '{ "name" : "scmFile" }'
      }, {
        field: 'title',
        desc: '文件标题',
        sample: '{ "title" : "a new file of SequoiaCM" }'
      }, {
        field: 'author',
        desc: '文件作者',
        sample: '{ "author" : "SequoiaCM" }'
      }, {
        field: 'user',
        desc: '创建用户',
        sample: '{ "user" : "admin" }'
      }, {
        field: 'create_time',
        desc: '创建时间',
        sample: '{ "create_time" : { "$gte" : 946654000000 } }'
      }, {
        field: 'update_user',
        desc: '更新用户',
        sample: '{ "update_user" : "admin" }'
      }, {
        field: 'update_time',
        desc: '更新时间',
        sample: '{ "update_time" : { "$gte" : 946654000000 } }'
      }, {
        field: 'tags',
        desc: '标签',
        sample: '{ "tags": { "$in" : [ "tagA" ] } }'
      }, {
        field: 'major_version',
        desc: '文件的最大主版本号',
        sample: '{ "major_version" : 1 }'
      }, {
        field: 'minor_version',
        desc: '最新主版本的最大次版本号',
        sample: '{ "minor_version" : 0 }'
      }, {
        field: 'data_id',
        desc: '文件内容 ID',
        sample: '{ "data_id" : "62b4116c400001003009f2fd" }'
      }, {
        field: 'data_create_time',
        desc: '文件内容创建时间',
        sample: '{ "data_create_time" : { "$gte" : 946654000000 } }'
      }, {
        field: 'size',
        desc: '文件大小',
        sample: '{ "size" : { "$gte" : 1024 } }'
      }, {
        field: 'md5',
        desc: '文件 MD5 值',
        sample: '{ "md5" : "ICy5YqxZB1uWSwcVLSNLcA==" }'
      }, {
        field: 'mime_type',
        desc: '文件内容 MIME 类型',
        sample: '{ "mime_type" : "application/octet-stream" }'
      }, {
        field: 'class_id',
        desc: '文件关联的自定义模型 ID',
        sample: '{ "class_id" : "62b405df40006600a74c5a4e" }'
      }, {
        field: 'class_properties',
        desc: '文件自定义属性',
        sample: '{ "class_properties.ID_NUM" : "613435199105687894" }'
      }, {
        field: 'site_list',
        desc: '存储站点列表',
        sample: '{ "site_list" : { "$elemMatch" : { "site_id" : 1 , "create_time" : { "$gt" : 946654000000 } } } }'
      }]
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
