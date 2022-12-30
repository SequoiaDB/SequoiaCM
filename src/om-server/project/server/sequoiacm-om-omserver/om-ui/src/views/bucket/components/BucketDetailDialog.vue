<template>
  <div>
    <!-- 桶详情对话框 -->
    <el-dialog
      class="detail-dialog"
      title="桶信息"
      :visible.sync="detailDialogVisible"
      width="720px">
      <div class="detail-container">
        <el-row >
          <el-col :span="4"><span class="key">ID</span></el-col>
          <el-col :span="20"><span class="value">{{curBucketDetail.id}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="4"><span class="key">桶名</span></el-col>
          <el-col :span="20"><span class="value">{{curBucketDetail.name}}</span></el-col>
        </el-row>
        <el-row >
          <el-col :span="4"><span class="key">所属region</span></el-col>
          <el-col :span="20"><span class="value">{{curBucketDetail.workspace}}</span></el-col>
        </el-row>
        <el-row >
          <el-col :span="4"><span class="key">版本控制状态</span></el-col>
          <el-col :span="20"><span class="value">{{curBucketDetail.version_status}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="4"><span class="key">创建人</span></el-col>
          <el-col :span="8"><span class="value">{{curBucketDetail.create_user}}</span></el-col>
          <el-col :span="4"><span class="key">创建时间</span></el-col>
          <el-col :span="8"><span class="value">{{$util.parseTime(curBucketDetail.create_time)}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="4"><span class="key">更新人</span></el-col>
          <el-col :span="8"><span class="value">{{curBucketDetail.update_user}}</span></el-col>
          <el-col :span="4"><span class="key">更新时间</span></el-col>
          <el-col :span="8"><span class="value">{{$util.parseTime(curBucketDetail.update_time)}}</span></el-col>
        </el-row>
      </div>

      <span slot="footer" class="dialog-footer">
        <el-button id="btn_file_detail_close" @click="close()" size="mini">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  props: {
  },
  data() {
    return{
      detailDialogVisible: false,
      curBucketDetail: {}
    }
  },
  methods: {
    show(data) {
      this.curBucketDetail = data
      this.detailDialogVisible = true
    },
    close() {
      this.detailDialogVisible = false
    }

  }
}
</script>

<style  scoped>
.detail-dialog >>> .el-dialog__body {
  max-height: 800px;
  overflow-y: auto;
  padding: 0px 10px 25px 0px;
  margin-left:20px;
}
.detail-dialog >>> .el-row {
  margin-top: 12px !important;
}
.key {
  font-size: 14px;
  font-weight: 600;
  color: #888;
}
.value {
  font-size: 14px;
  overflow: hidden;
  color: #606266;
}
</style>
