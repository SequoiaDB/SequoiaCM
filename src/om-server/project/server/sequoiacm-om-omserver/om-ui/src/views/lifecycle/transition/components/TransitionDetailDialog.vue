<template>
  <div>
    <!-- 查看数据流详情对话框 -->
    <el-dialog
      class="detail-dialog"
      title="数据流详情"
      :visible.sync="detailDialogVisible"
      width="35%">
      <div class="detail-container" v-if="transitionDetail">
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">数据流名称：</span> <span class="value">{{transitionDetail.name}}</span></el-col>
        </el-row>
        <el-row v-if="transitionSch">
          <el-col :span="24"><span class="key" style="width:20%">工作区名称：</span> <span class="value">{{transitionSch.workspace}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">起始阶段：</span> <span class="value">{{transitionDetail.source}}</span></el-col>
          <el-col :span="12"><span class="key">目标阶段：</span> <span class="value">{{transitionDetail.dest}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">快速启动：</span> <span class="value">{{transitionDetail.is_quick_start?'是':'否'}}</span></el-col>
          <el-col :span="12"><span class="key">数据校验级别：</span> <span class="value">{{transitionDetail.data_check_level==='week'?'弱校验':'强校验'}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">回收空间：</span> <span class="value">{{transitionDetail.is_recycle_space?'开启':'关闭'}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key" style="width:20%">文件范围：</span> <span class="value">{{transitionDetail.scope}}</span></el-col>
        </el-row>
        <el-row v-if="transitionDetail.transition_triggers">
          <el-col :span="5"><span class="key" style="width:96%">流转配置：</span></el-col>
          <el-col :span="19">
            <el-card class="box-card">
              <el-row>
                <el-col :span="5"><span class="key" style="width:96%">迁移条件: </span></el-col>
                <el-col :span="19"><span class="value"> {{getModeStr(transitionDetail.transition_triggers.mode)}} </span></el-col>
              </el-row>
              <div v-for="trigger in transitionDetail.transition_triggers.triggers" :key="trigger.id" class="text item">
                <template>
                  <el-collapse>
                    <el-collapse-item>
                      <template slot="title">
                        规则 {{trigger.id}}
                      </template>
                      <el-row>
                        <el-col :span="5"><span class="key-inner">迁移条件: </span></el-col>
                        <el-col :span="19"><span class="value"> {{getModeStr(trigger.mode)}} </span></el-col>
                      </el-row>
                      <el-row>
                        <el-col :span="5"><span class="key-inner">文件创建时间: </span></el-col>
                        <el-col :span="19"><span class="value"> {{converTime(trigger.create_time)}} </span></el-col>
                      </el-row>
                      <el-row>
                        <el-col :span="5"><span class="key-inner">上次访问时间: </span></el-col>
                        <el-col :span="19"><span class="value"> {{converTime(trigger.last_access_time)}} </span></el-col>
                      </el-row>
                      <el-row>
                        <el-col :span="5"><span class="key-inner">文件停留时间: </span></el-col>
                        <el-col :span="19"><span class="value"> {{converTime(trigger.build_time)}} </span></el-col>
                      </el-row>
                    </el-collapse-item>
                  </el-collapse>
                </template>
              </div>
            </el-card> 
            <el-card class="box-card" v-if="transitionDetail.clean_triggers">
              <el-row>
                <el-col :span="5"><span class="key" style="width:96%">清理条件: </span></el-col>
                <el-col :span="19"><span class="value"> {{getModeStr(transitionDetail.clean_triggers.mode)}} </span></el-col>
              </el-row>
              <div v-for="trigger in transitionDetail.clean_triggers.triggers" :key="trigger.id" class="text item">
                <template>
                  <el-collapse>
                    <el-collapse-item>
                      <template slot="title">
                        规则 {{trigger.id}}
                      </template>
                      <el-row>
                        <el-col :span="5"><span class="key-inner" style="width:90%">清理条件: </span></el-col>
                        <el-col :span="19"><span class="value"> {{getModeStr(trigger.mode)}} </span></el-col>
                      </el-row>
                      <el-row>
                        <el-col :span="5"><span class="key-inner" style="width:90%">上次访问时间: </span></el-col>
                        <el-col :span="19"><span class="value"> {{converTime(trigger.last_access_time)}} </span></el-col>
                      </el-row>
                      <el-row>
                        <el-col :span="5"><span class="key-inner" style="width:90%">文件停留时间: </span></el-col>
                        <el-col :span="19"><span class="value"> {{converTime(trigger.transition_time)}} </span></el-col>
                      </el-row>
                    </el-collapse-item>
                  </el-collapse>
                </template>
              </div>
            </el-card> 
          </el-col>
        </el-row>
        <el-row v-if="transitionDetail.matcher">
          <el-col :span="5"><span class="key" style="width:96%">文件匹配条件：</span></el-col>
          <el-col :span="19">
            <el-input
              type="textarea"
              :rows="2"
              :autosize="{ minRows: 2, maxRows: 10}"
              readonly
              :value="$util.toPrettyJson(transitionDetail.matcher)">
            </el-input>
          </el-col>
        </el-row>
        <el-row v-if="transitionSch">
          <el-col :span="5"><span class="key" style="width:96%">调度任务列表</span></el-col>
          <el-col :span="19">
            <el-card class="box-card">
              <div v-for="o in transitionSch.schedules" :key="o" class="text item">
                {{o.name}} <el-button icon="el-icon-info" size="mini" style="margin-left:5px" @click="jumpToSchedule(transitionSch.workspace, o.name)"></el-button>
              </div>
            </el-card>            
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
  data() {
    return{
      transitionDetail: { },
      transitionSch: { },
      detailDialogVisible: false,
      fileScopeTypes: FILE_SCOPE_TYPES,
      taskTypes: TASK_TYPES,
    }
  },
  methods: {
    getModeStr(mode) {
      return mode === 'ALL' ? '符合以下所有条件' : '符合以下任一条件'
    },
    // 数据的类型转换 xxd -> xx 天
    converTime (time) {
      return Number(time.slice(0, time.length - 1)) + " 天"
    },
    // 跳转到调度服务页面
    jumpToSchedule(ws, scheduleName) {
      const { href } = this.$router.resolve({
        path: "/lifecycle/schedule",
        query: {
          target: "checkSchedule",
          workspace: ws,
          schedule: scheduleName
        }
      })
      window.open(href, "_blank")
    },
    show(transition, transitionSch) {
      this.transitionDetail = transition
      this.transitionSch = transitionSch
      this.detailDialogVisible = true
    },
    close() {
      this.detailDialogVisible = false
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
.key-inner {
  font-size: 13px;
  display: block;
  width: 90%;
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
