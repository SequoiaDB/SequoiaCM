<template>
  <div>
    <!-- 桶详情对话框 -->
    <el-dialog
      class="detail-dialog"
      title="桶信息"
      :visible.sync="detailDialogVisible"
      @close="clearRefresh"
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
        <el-row v-if="curBucketDetail.quota_info">
          <el-col :span="4"><span class="key">额度限制</span></el-col>
          <el-col :span="20"><span class="value">{{curBucketDetail.quota_info.enable ? '打开' : '关闭'}}</span></el-col>
        </el-row>
        <el-row v-if="curBucketDetail.quota_info  && curBucketDetail.quota_info.enable">
          <el-col :span="4"><span class="key">同步状态</span></el-col>
          <el-col :span="20"><span class="value">{{curBucketDetail.quota_info.sync_status || '未同步'}}</span></el-col>
        </el-row>
        <el-row v-if="curBucketDetail.quota_info  && curBucketDetail.quota_info.enable && curBucketDetail.quota_info.sync_status === 'syncing'">
          <el-col :span="4"><span class="key">预估同步时间</span></el-col>
          <el-col :span="20"><span class="value">{{curBucketDetail.quota_info.estimated_effective_time >=0 ? curBucketDetail.quota_info.estimated_effective_time + 'ms' : '预估中'}}</span></el-col>
        </el-row>
        <el-row v-if="curBucketDetail.quota_info && curBucketDetail.quota_info.enable">
          <el-col :span="4"><span class="key">已用存储容量</span></el-col>
          <el-col :span="8" v-if="curBucketDetail.quota_info.max_size > 0">
            <el-progress :text-inside="true" :stroke-width="18" :percentage="(Number)(parseFloat((curBucketDetail.quota_info.used_size/curBucketDetail.quota_info.max_size)*100).toFixed(2))"></el-progress>
          </el-col>
          <el-col :span="12"><span class="value"> {{sizeInfoText}}</span></el-col>
        </el-row>
        <el-row v-if="curBucketDetail.quota_info && curBucketDetail.quota_info.enable">
          <el-col :span="4"><span class="key">已用对象个数</span></el-col>
           <el-col :span="8" v-if="curBucketDetail.quota_info.max_objects > 0">
            <el-progress :text-inside="true" :stroke-width="18" :percentage="(Number)(parseFloat((curBucketDetail.quota_info.used_objects/curBucketDetail.quota_info.max_objects)*100).toFixed(2))"></el-progress>
          </el-col>
          <el-col :span="12"><span class="value">{{objectInfoText}}</span></el-col>
        </el-row>
      </div>
      <el-alert
        class="alert-msg"
        v-if="curBucketDetail.quota_info && curBucketDetail.quota_info.sync_status==='failed'"
        :title="'最近一次同步失败，请重新同步，失败消息：'+curBucketDetail.quota_info.error_msg"
        type="warning">
      </el-alert>
      <div class="tip" style="margin-top: 20px" v-if="curBucketDetail.quota_info && curBucketDetail.quota_info.enable && curBucketDetail.quota_info.sync_status != 'syncing'">
        <i class="el-icon-info"></i> 已用额度信息不准确？<a @click="handleSync" style="color: blue">点击</a>同步已使用额度。
      </div>
      <div class="tip" style="margin-top: 20px" v-if="curBucketDetail.quota_info && curBucketDetail.quota_info.enable && curBucketDetail.quota_info.sync_status == 'syncing'">
        <i class="el-icon-loading"></i> 已用额度信息正在同步中，<a @click="handleCancelSync" style="color: blue">点击</a>取消同步。
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button id="btn_file_detail_close" @click="close()" size="mini">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { cancelSyncBucketQuota, syncBucketQuota } from '@/api/quota'
import { queryBucketDetail} from '@/api/bucket'

export default {
  props: {
  },
  data() {
    return{
      detailDialogVisible: false,
      curBucketDetail: {},
      // 数据自动刷新时间间隔
      interval: 1000,
      refresh: ''
    }
  },
  methods: {
    show(data) {
      this.curBucketDetail = data
      this.detailDialogVisible = true
      if (this.curBucketDetail.quota_info && this.curBucketDetail.quota_info.enable) {
        this.setRefresh()
      }
      
    },

    close() {
      this.detailDialogVisible = false
    },

    setRefresh() {
      this.refresh = setTimeout(() => {
        queryBucketDetail(this.curBucketDetail.name).then(res => {
          this.curBucketDetail = JSON.parse(res.headers['bucket'])
          this.setRefresh()
        })
      }, this.interval)
    },

    clearRefresh() {
      if (this.refresh) {
        clearTimeout(this.refresh)
      }
    },

    handleCancelSync() {
      this.$confirm("确认取消同步吗？", '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        cancelSyncBucketQuota(this.curBucketDetail.name).then(res => {
          this.$message.success("取消同步成功")
        })
      }).catch(() => {

      })
    },

    handleSync() {
      this.$confirm("确认执行同步吗（如果桶内数据多，同步操作可能会比较耗时，建议在业务量较小的时候进行）？", '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        syncBucketQuota(this.curBucketDetail.name).then(res => {
          this.$message.success("正在执行同步，请稍后查看同步结果")
        })
      }).catch(() => {

      });

    },
  },
  computed: {
    sizeInfoText(){
      let res = this.$util.formatBytes(this.curBucketDetail.quota_info.used_size).text
      if (this.curBucketDetail.quota_info.max_size >= 0) {
        res = res +  '/'
        if (this.curBucketDetail.quota_info.max_size > 0) {
          res += this.$util.formatBytes(this.curBucketDetail.quota_info.max_size).text
        }else {
          res += '0'
        }
        let format =  this.$util.formatBytes(this.curBucketDetail.quota_info.max_size - this.curBucketDetail.quota_info.used_size)
        if (format.value >= 0) {
          res = res + ' (剩余 ' + format.text +')'
        } else {
          res = res + ' (超出 ' + Math.abs(format.value) + format.unit +')'
        }
      }
      else if(this.curBucketDetail.quota_info.max_size == 0) {
        res = res + '/0'
      }
      else {
        res = res + '/Unlimited'
      }
      return res
    },
    objectInfoText(){
      let res = this.curBucketDetail.quota_info.used_objects
      if (this.curBucketDetail.quota_info.max_objects >= 0) {
        res = res +  '/' + this.curBucketDetail.quota_info.max_objects
        let remain = this.curBucketDetail.quota_info.max_objects - this.curBucketDetail.quota_info.used_objects
        if (remain >= 0){
          res = res + ' (剩余 ' + remain +')'
        } else {
          res = res + ' (超出 ' + Math.abs(remain) +')'
        }
      }
      else {
        res = res + '/Unlimited'
      }
      return res
    },
  },
  beforeDestroy(){
    this.clearRefresh()
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
.alert-msg {
  margin-top: 5px;
  font-size: 25px;
  font-weight: 600;
}
</style>
