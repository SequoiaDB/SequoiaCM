<template>
  <div>
    <!-- 文件下载对话框 -->
    <el-dialog
      class="download-dialog"
      title="文件信息"
      :visible.sync="downloadDialogVisible"
      width="500px">
      <div class="download-container">
        <el-row>
          <el-col :span="5"><span class="key">文件ID</span></el-col>
          <el-col :span="19"><span class="value">{{curVersionFileDetail.id}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="5"><span class="key">文件名</span></el-col>
          <el-col :span="19"><span class="value">{{curVersionFileDetail.name}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="5"><span class="key">文件大小</span></el-col>
          <el-col :span="19"><span class="value">{{$util.convertFileSize(curVersionFileDetail.size)}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="5"><span class="key">MD5</span></el-col>
          <el-col :span="19"><span class="value">{{curVersionFileDetail.md5?curVersionFileDetail.md5:'未计算'}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="5"><span class="key" style="line-height:35px;">存储站点</span></el-col>
          <el-col :span="19">        
            <el-tooltip placement="top"
              v-for="siteItem of curVersionFileDetail.sites"
              :key="siteItem.site_name">
                <div slot="content">
                  数据迁入时间: {{$util.parseTime(siteItem.create_time)}}<br/><br/>
                  上次访问时间: {{$util.parseTime(siteItem.last_access_time)}}
                </div>
                <el-tag disable-transitions style="margin-right:0.2rem; margin-top:0.2rem">
                  {{siteItem.site_name}}
                </el-tag>
            </el-tooltip>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="5"><span class="key" style="line-height:30px;">版本</span></el-col>
          <el-col :span="19">
            <el-select 
              id="select_download_version"
              v-model="curVersion" 
              value-key="Id" 
              filterable 
              size="small" 
              style="width:100%"
              @change="handleVersionChange">
                <el-option
                  v-for="item in multiVofCurFile"
                  :key="item.major_version + ',' + item.minor_version"
                  :value="item.major_version + ',' + item.minor_version"
                  :label="'主版本: ' + item.major_version + '\t次版本: ' + item.minor_version">
                </el-option>
            </el-select>
          </el-col>
        </el-row>
      </div>

      <span slot="footer" class="dialog-footer" style="border:1px soild red">
        <el-divider></el-divider>
        <el-button id="btn_file_download" type="primary" @click="handleDownloadFile()" size="mini" icon="el-icon-download">下 载</el-button>
        <el-button id="btn_file_download_close" @click="close()" size="mini">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {queryFileDetail,getDownloadUrl} from '@/api/file'
export default {
  setup() {
  },
  props: {
    workspace: {
      type: String,
      default: ''
    },
    multiVofCurFile: {
      type: Array,
      default: () => []
    },
  },
  data() {
    return{
      downloadDialogVisible: false,
      curVersion: '',
      curVersionFile: {},
      curVersionFileDetail: {}
    }
  },
  methods: {
    show() {
      this.downloadDialogVisible = true
    },
    close() {
      this.downloadDialogVisible = false
    },
    handleVersionChange() {
      // 使用 value（主次版本号） 从 multiVofFile 列表中过滤出当前选中的版本
      this.curVersionFile = this.multiVofCurFile.find(
        item=>{
          return (item.major_version +',' + item.minor_version) === this.curVersion;
        }
      )
      this.refreshFileDetail()
    },
    // 下载文件
    handleDownloadFile() {
      var url = getDownloadUrl(this.workspace, this.curVersionFileDetail.sites[0].site_name, this.curVersionFile.id, this.curVersionFile.major_version, this.curVersionFile.minor_version);
      window.location.href = url;
    },
    // 刷新文件页面
    refreshFileDetail() {
      queryFileDetail(this.workspace, this.curVersionFile.id, this.curVersionFile.major_version, this.curVersionFile.minor_version).then(res => {
        this.curVersionFileDetail = JSON.parse(decodeURIComponent(res.headers['file']))
      })
    }
  },
  // 监听当前文件的版本列表，不为空时初始化选择器
  watch: {
    multiVofCurFile(value){
      if(value[0]){
        // major_version,minor_version 拼接主次版本号，保证选项唯一性
        this.curVersion = value[0].major_version + ',' + value[0].minor_version
        this.curVersionFile = value[0]
        this.refreshFileDetail()
      }
    }
  }
}
</script>

<style  scoped>
.download-dialog >>> .el-dialog__body {
  padding: 15px 20px 15px 15px
}
.download-container >>> .el-row {
  margin-top: 15px !important;
}
.dialog-footer >>> .el-divider {
  margin:16px 0;
}
.key {
  font-size: 14px;
  font-weight: 600;
  color: #888;
}
.value {
  font-size: 14px;
  color: #606266;
}
</style>