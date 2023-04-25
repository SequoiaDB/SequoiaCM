<template>
  <div>
    <!-- 查看任务详情对话框 -->
    <el-dialog
      class="detail-dialog"
      title="任务详情"
      :visible.sync="detailDialogVisible"
      width="35%">
      <div class="detail-container">
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">任务ID：</span> <span class="value">{{taskDetail.task_id}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">所属工作区：</span> <span class="value">{{taskDetail.workspace}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">预估数量：</span> <span class="value">{{taskDetail.estimate_count}}</span></el-col>
          <el-col :span="12"><span class="key">实际数量：</span> <span class="value">{{taskDetail.actual_count}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">成功数量：</span> <span class="value">{{taskDetail.success_count}}</span></el-col>
          <el-col :span="12"><span class="key">失败数量：</span> <span class="value">{{taskDetail.fail_count}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">启动时间：</span> <span class="value">{{this.$util.parseTime(taskDetail.start_time)}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">停止时间：</span> <span class="value">{{this.$util.parseTime(taskDetail.stop_time)}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">任务状态：</span> <span class="value">{{getStatusText(taskDetail.status)}}</span></el-col>
        </el-row>
        <el-row v-if="taskDetail.content">
          <el-col :span="5"><span class="key" style="width:96%">文件查询条件：</span></el-col>
          <el-col :span="19">
            <el-input
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 2, maxRows: 10}"
              readonly
              :value="$util.toPrettyJson(taskDetail.content)">
            </el-input>
          </el-col>
        </el-row>
        <el-row v-if="taskDetail.extra_info">
          <el-col :span="5"><span class="key" style="width:96%">其它信息：</span></el-col>
          <el-col :span="19">
            <el-input
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 2, maxRows: 10}"
              readonly
              :value="$util.toPrettyJson(taskDetail.extra_info)">
            </el-input>
          </el-col>
        </el-row>
        <el-row v-if="taskDetail.detail">
          <el-col :span="5"><span class="key" style="width:96%">异常信息：</span></el-col>
          <el-col :span="19">
            <el-input
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 2, maxRows: 10}"
              readonly
              :value="$util.toPrettyJson(taskDetail.detail)">
            </el-input>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import {TASK_STATUS} from '@/utils/common-define'
export default {
  setup() {
  },
  props: {
    taskDetail: {
      type: Object,
      default: {}
    }
  },
  data() {
    return{
      detailDialogVisible: false,
      taskStatus: TASK_STATUS
    }
  },
  methods: {
    show() {
      this.detailDialogVisible = true
    },
    close() {
      this.detailDialogVisible = false
    },
    getStatusText(status) {
      if (status == 2) {
          return "运行中（" + this.taskDetail.progress + "%）"
      }
      for (const task of this.taskStatus) {
        if (task.value == status) {
          return task.text
        }
      }
    },
  }
}
</script>

<style  scoped>
.detail-dialog >>> .el-dialog__body {
  padding: 0px 10px 25px 10px;
}
.key {
  font-size: 13px;
  display: block;
  width: 40%;
  float: left;
  line-height: 15px;
  text-align: right;
  font-weight: 600;
  opacity: 0.8;
}
.value {
  display: block;
  width: 60%;
  line-height: 15px;
  font-size: 14px;
  float: left;
  color: #606266;
}
.detail-container >>> .el-row {
  margin-top: 8px !important;
}
</style>
