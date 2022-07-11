<template>
  <div>
    <!-- 文件详情对话框 -->
    <el-dialog
      class="detail-dialog"
      title="文件信息"
      :visible.sync="detailDialogVisible"
      width="720px">
      <div class="detail-container">
        <el-row>
          <el-col :span="3"><span class="key">ID</span></el-col>
          <el-col :span="21"><span class="value">{{curVersionFileDetail.id}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="3"><span class="key">文件名</span></el-col>
          <el-col :span="21"><span class="value">{{curVersionFileDetail.name}}</span></el-col>
        </el-row>
        <el-row v-if="curVersionFileDetail.bucket_id">
          <el-col :span="3"><span class="key">所属桶 ID</span></el-col>
          <el-col :span="21"><span class="value">{{curVersionFileDetail.bucket_id}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="3"><span class="key">标题</span></el-col>
          <el-col :span="9"><span class="value">{{curVersionFileDetail.title?curVersionFileDetail.title:'无'}}</span></el-col>
          <el-col :span="2"><span class="key">作者</span></el-col>
          <el-col :span="10"><span class="value">{{curVersionFileDetail.author?curVersionFileDetail.author:'无'}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="3"><span class="key">所属批次</span></el-col>
          <el-col :span="9"><span class="value">{{curVersionFileDetail.batch_name?curVersionFileDetail.batch_name:'无'}}</span></el-col>
          <template v-if="curVersionFileDetail.directory_id">
            <el-col :span="2"><span class="key">位置</span></el-col>
            <el-col :span="10"><span class="value">{{curVersionFileDetail.directory_path}}</span></el-col>
          </template>
        </el-row>
        <el-row>
          <el-col :span="3"><span class="key">创建人</span></el-col>
          <el-col :span="9"><span class="value">{{curVersionFileDetail.user}}</span></el-col>
          <el-col :span="3"><span class="key">创建时间</span></el-col>
          <el-col :span="9"><span class="value">{{$util.parseTime(curVersionFileDetail.create_time)}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="3"><span class="key">更新人</span></el-col>
          <el-col :span="9"><span class="value">{{curVersionFileDetail.update_user}}</span></el-col>
          <el-col :span="3"><span class="key">更新时间</span></el-col>
          <el-col :span="9"><span class="value">{{$util.parseTime(curVersionFileDetail.update_time)}}</span></el-col>
        </el-row>
        <el-row v-if="curVersionFileDetail.tags.length!==0">
          <el-col :span="3"><span class="key" style="line-height:30px;">标签</span></el-col>
          <el-col :span="21">
            <el-tag
              v-for="tag in curVersionFileDetail.tags"
              :key="tag">
              <el-tooltip :content="tag" placement="top">
                <span class="tag-text">{{tag}}</span>
              </el-tooltip>
            </el-tag>
          </el-col>
        </el-row>
        <template v-if="curVersionFileDetail.class_name">
          <el-row >
            <el-col :span="3"><span class="key">元数据模型</span></el-col>
            <el-col :span="21"><span class="value">{{curVersionFileDetail.class_name}}</span></el-col>
          </el-row>
          <el-row>
            <el-col :span="3"><span class="key">元数据属性</span></el-col>
            <el-col :span="21">
              <el-input
                type="textarea"
                readonly
                :rows="2"
                autosize
                :value="$util.toPrettyJson(curVersionFileDetail.class_properties)">
              </el-input>
            </el-col>
          </el-row>
        </template>
        <el-divider></el-divider>
        <el-row>
          <el-col :span="3"><span class="key" style="line-height:30px;">版本</span></el-col>
          <el-col :span="21">
            <el-select
              id="select_file_detail_version"
              v-model="curVersion" 
              value-key="Id" 
              filterable 
              size="small" 
              style="width:50%" 
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
        <el-row>
          <el-col :span="3"><span class="key">内容类型</span></el-col>
          <el-col :span="21"><span class="value">{{curVersionFileDetail.mime_type?curVersionFileDetail.mime_type:'无'}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="3"><span class="key">大小</span></el-col>
          <el-col :span="21"><span class="value">{{curVersionFileDetail.size}} ({{$util.convertFileSize(curVersionFileDetail.size)}})</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="3"><span class="key">MD5</span></el-col>
          <el-col :span="21"><span class="value">{{curVersionFileDetail.md5?curVersionFileDetail.md5:'未计算'}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="3">
            <span class="key" style="line-height:35px;">存储站点</span> 
          </el-col>
          <el-col :span="21">        
            <el-tooltip placement="top"
              v-for="siteItem of this.curVersionFileDetail.sites"
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
      </div>

      <span slot="footer" class="dialog-footer" style="border:1px soild red">
        <el-button id="btn_export_metadata" type="primary" @click="handleExportMetadata" size="mini" icon="el-icon-printer">导出元数据</el-button>
        <el-button id="btn_file_detail_close" @click="close()" size="mini">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {queryFileDetail} from '@/api/file'
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
      detailDialogVisible: false,
      curVersion: '',
      curVersionFile: {},
      curVersionFileDetail: {
        tags: []
      }
    }
  },
  methods: {
    show() {
      this.detailDialogVisible = true
    },
    close() {
      this.detailDialogVisible = false
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
    refreshFileDetail() {
      queryFileDetail(this.workspace, this.curVersionFile.id, this.curVersionFile.major_version, this.curVersionFile.minor_version).then(res => {
        this.curVersionFileDetail = JSON.parse(decodeURIComponent(res.headers['file']))
      })
    },
    handleExportMetadata() {
      let metadata = this.$util.toPrettyJson(this.curVersionFileDetail)
      var blob = new Blob([metadata], {type: 'text/json'})
      let link = document.createElement('a')
      link.style.display = "none"
      link.setAttribute('download', this.curVersionFile.name + ".metadata.json")
      link.href = window.URL.createObjectURL(blob)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
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
.el-tag {
  margin-right: 10px;
  margin-bottom: 5px;
}
.tag-text {
  display: inline-block;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>