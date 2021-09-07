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
          <el-col :span="12"><span class="key">任务类型：</span> <span class="value">{{getTaskTypeName(taskDetail.type)}}</span></el-col>
          <el-col v-if="taskDetail.type == 'clean_file'" :span="12"><span class="key">清理站点：</span> <span class="value">{{taskDetail.source_site}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">调度任务ID：</span> <span class="value">{{taskDetail.schedule_id}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">任务名称：</span> <span class="value">{{taskDetail.name}}</span></el-col>
        </el-row>
        <el-row v-if="taskDetail.type == 'copy_file'">
          <el-col :span="12"><span class="key">源站点：</span> <span class="value">{{taskDetail.source_site}}</span></el-col>
          <el-col :span="12"><span class="key">目的站点：</span> <span class="value">{{taskDetail.target_site}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">创建人：</span> <span class="value">{{taskDetail.create_user}}</span></el-col>
          <el-col :span="12"><span class="key">创建时间：</span> <span class="value">{{taskDetail.create_time|parseTime}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">所属工作区：</span> <span class="value">{{taskDetail.workspace}}</span></el-col>
          <el-col :span="12"><span class="key">cron：</span> <span class="value">{{taskDetail.cron}}</span></el-col>
        </el-row>
        <el-row v-if="taskDetail.preferred_region">
          <el-col :span="12"><span class="key">优先region：</span> <span class="value">{{taskDetail.preferred_region}}</span></el-col>
          <el-col :span="12"><span class="key">优先zone：</span> <span class="value">{{taskDetail.preferred_zone}}</span></el-col>
        </el-row>
        <el-row>
          <el-col  v-if="taskDetail.max_stay_time" :span="12"><span class="key">文件停留时间：</span> <span class="value">{{taskDetail.max_stay_time.slice(0, taskDetail.max_stay_time.length-1)}}天</span></el-col>
          <el-col :span="12"><span class="key">任务超时时间：</span> <span class="value">{{taskDetail.max_exec_time}}ms</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">文件范围：</span> <span class="value">{{getFileScopeText(taskDetail.scope_type)}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">任务描述：</span> <span class="value" style="width:80%">{{taskDetail.description}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="5"><span class="key" style="width:96%">文件查询条件：</span></el-col>
          <el-col :span="19">
            <el-input
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 2, maxRows: 10}"
              readonly
              :value="$util.toPrettyJson(taskDetail.condition)">
            </el-input>
          </el-col>
        </el-row>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import {TASK_TYPES, FILE_SCOPE_TYPES} from '@/utils/common-define'
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
      fileScopeTypes: FILE_SCOPE_TYPES,
      taskTypes: TASK_TYPES,
    }
  },
  methods: {
    show() {
      this.detailDialogVisible = true
    },
    close() {
      this.detailDialogVisible = false
    },
    getTaskTypeName(taskType) {
      for (const task of this.taskTypes) {
        if (task.value == taskType){
          return task.label
        }
      }
    },
    getFileScopeText(scopeCode) {
      for (const scope of this.fileScopeTypes) {
        if (scope.value == scopeCode){
          return scope.label
        }
      }
    }
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
