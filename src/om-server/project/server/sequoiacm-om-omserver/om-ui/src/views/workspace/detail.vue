<template>
  <div class="app-container">
      <el-tabs class="tabs" v-model="activeName" @tab-click="tabClick">
        <el-tab-pane label="基础信息" name="basic"></el-tab-pane>
        <el-tab-pane label="流量统计" name="statistics"></el-tab-pane>
      </el-tabs>
      <template v-if="activeName==='basic'">
        <div class="info-container">
          <div class="title">基本信息</div>
          <el-row>
            <el-col :span="24"><span class="key">工作区名称：</span> <span class="value">{{workspaceInfo.name}}</span></el-col>
          </el-row>
          <el-row>
            <el-col :span="24"><span class="key">工作区描述：</span> <span class="value">{{workspaceInfo.description}}</span></el-col>
          </el-row>
          <el-row>
            <el-col :span="12"><span class="key">创建人：</span> <span class="value">{{workspaceInfo.create_user}}</span></el-col>
            <el-col :span="12"><span class="key">创建时间：</span> <span class="value">{{workspaceInfo.create_time|parseTime}}</span></el-col>
          </el-row>
          <el-row>
            <el-col :span="12"><span class="key">更新人：</span> <span class="value">{{workspaceInfo.update_user}}</span></el-col>
            <el-col :span="12"><span class="key">更新时间：</span> <span class="value">{{workspaceInfo.update_time|parseTime}}</span></el-col>
          </el-row>
        </div>
        <el-divider></el-divider>
        <div class="info-container">
          <div class="title">工作区配置</div>
          <el-row>
            <el-col :span="2" style="width:75px;margin-top:8px;font-size: 15px">
              <el-tooltip effect="light" placement="top-start">
                <div slot="content" class="tooltip">
                  文件缓存策略控制跨中心读时，文件数据是否缓存在途经的站点上，目前支持如下缓存策略：<br/>
                    <b>ALWAYS：</b>文件在跨中心读时，文件数据总是会缓存在途经的站点上<br/>
                    <b>NEVER：</b> 文件在跨中心读时，文件数据不会缓存在途经的站点上
                </div>
                <span class="config-key">缓存策略：</span>
              </el-tooltip>
            </el-col>
            <el-col :span="3" style="width:200px;">
              <span>
                <el-select 
                  id="select_ws_site_cache_strategy"
                  v-model="cacheStrategy" 
                  size="small" 
                  placeholder="无数据" 
                  filterable
                  @change="changeSiteCacheStrategy"
                  style="width:100%">
                  <el-option
                    v-for="item in siteCacheStrategies"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value">
                  </el-option>
                </el-select>
              </span>
            </el-col>
          </el-row>
        </div>
        <el-divider></el-divider>
        <div class="info-container">
          <div class="title">站点信息</div>
          <el-table
            :data="siteList"
            border
            style="width: 100%">
            <el-table-column
              type="index"
              label="序号"
              width="55">
            </el-table-column>
            <el-table-column
              prop="site_name"
              label="站点名称"
              width="180">
            </el-table-column>
            <el-table-column
              prop="site_type"
              label="数据源类型"
              width="180">
            </el-table-column>
            <el-table-column
              label="站点配置">
              <template slot-scope="scope">
                {{scope.row.options}}
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-divider></el-divider>
        <div class="info-container">
          <div class="title">元数据信息</div>
          <div class="code">
            <el-input
              type="textarea"
              readonly
              :rows="2"
              autosize
              :value="$util.toPrettyJson(workspaceInfo.meta_options)">
            </el-input>
          </div>
        </div>
        <el-divider></el-divider>
        <div class="info-container">
        <div class="title">统计信息</div>
        <el-row>
          <el-col :span="12"><span class="key">目录数：</span> <span class="value"><el-tag size="small" >{{workspaceInfo.directory_count}}</el-tag></span></el-col>
          <el-col :span="12"><span class="key">文件数：</span> <span class="value"><el-tag size="small">{{workspaceInfo.file_count}}</el-tag></span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">批次数：</span> <span class="value"><el-tag size="small">{{workspaceInfo.batch_count}}</el-tag></span></el-col>
        </el-row>
        </div>
      </template>
      <template v-if="activeName==='statistics'">
        <panel title="上传下载请求" class="panel-item">
          <div slot="title-right" class="time-selector">
            <el-date-picker
              v-model="uploadDownloadQueryRange"
              type="daterange"
              align="right"
              unlink-panels
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :picker-options="pickerOptions">
            </el-date-picker>
            <el-button type="primary" size="small" icon="el-icon-search" class="btn-query" @click="handleQueryUploadDownloadBtnClick">查询</el-button>
          </div>
          <file-upload-download-chart ref="uploadDownloadChart"/>
        </panel>
        <panel title="文件数增量" class="panel-item">
          <div slot="title-right" class="time-selector">
            <el-date-picker
              v-model="fileCountDeltaQueryRange"
              type="daterange"
              align="right"
              unlink-panels
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :picker-options="pickerOptions">
            </el-date-picker>
            <el-button type="primary" size="small" icon="el-icon-search" class="btn-query" @click="handleQueryFileCountDeltaBtnClick">查询</el-button>
          </div>
          <file-count-delta-chart ref="fileCountDeltaChart"></file-count-delta-chart>
        </panel>
        <panel title="文件大小增量" class="panel-item">
          <div slot="title-right" class="time-selector">
            <el-date-picker
              v-model="fileSizeDeltaQueryRange"
              type="daterange"
              align="right"
              unlink-panels
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :picker-options="pickerOptions">
            </el-date-picker>
            <el-button type="primary" size="small" icon="el-icon-search" class="btn-query"  @click="handleQueryFileSizeDeltaBtnClick">查询</el-button>
          </div>
          <file-size-delta-chart ref="fileSizeDeltaChart"></file-size-delta-chart>
        </panel>
      </template>
  </div>
</template>
<script>

import FileUploadDownloadChart from './components/FileUploadDownloadChart.vue'
import FileCountDeltaChart from '@/views/workspace/components/FileCountDeltaChart'
import FileSizeDeltaChart from '@/views/workspace/components/FileSizeDeltaChart'
import Panel from '@/components/Panel'
import {queryWorkspaceDetail, updateWorkspace, queryWorkspaceTraffic, queryWorkspaceFileDelta} from '@/api/workspace'
export default {
  components: {
    FileUploadDownloadChart,
    FileCountDeltaChart,
    FileSizeDeltaChart,
    Panel
  },
  data(){
    return{
      activeName:'basic',
      workspaceInfo: {},
      siteList: [],
      originalCacheStrategy: '',
      cacheStrategy: '',
      siteCacheStrategies: [{ 
        value : 'ALWAYS', label : 'ALWAYS'
      }, { 
        value : 'NEVER', label : 'NEVER'
      }],
      pickerOptions: {
        shortcuts: [{
          text: '最近一周',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
            picker.$emit('pick', [start, end]);
          }
        }, {
          text: '最近一个月',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
            picker.$emit('pick', [start, end]);
          }
        }, {
          text: '最近三个月',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 90);
            picker.$emit('pick', [start, end]);
          }
        }]
      },
      uploadDownloadQueryRange: '',
      uploadDownloadQueryCondition: {
        beginTime: null,
        endTIme: null
      },
      fileSizeDeltaQueryRange: '',
      fileSizeDeltaQueryCondition: {
        beginTime: null,
        endTIme: null
      },
      fileCountDeltaQueryRange: '',
      fileCountDeltaQueryCondition: {
        beginTime: null,
        endTIme: null
      },
      fileSizeDeltaChartData: null,
      fileCountDeltaChartData: null,
      wsName: this.$route.params.name
    }
  },
  computed:{
   
  },
  methods:{
    //初始化
    init(){
      this.queryWorkspaceDetail()
    },

    // 查询工作区详细信息
    async queryWorkspaceDetail(){
      let res = await queryWorkspaceDetail(this.$route.params.name)
      this.workspaceInfo = res.data
      if(this.workspaceInfo && this.workspaceInfo.data_locations){
        this.siteList = this.workspaceInfo.data_locations
      }
      if (this.workspaceInfo.site_cache_strategy) {
        this.cacheStrategy = this.workspaceInfo.site_cache_strategy
      }
      else {
        this.cacheStrategy = 'ALWAYS'
      }
      this.originalCacheStrategy = this.cacheStrategy
    },

    // 刷新图表数据
    async refreshChart() {
      this.queryUploadDownloadChart()
      this.queryFileCountDeltaChart()
      this.queryFileSizeDeltaChart()
    },

    changeSiteCacheStrategy() {
      let ws = {
        siteCacheStrategy : this.cacheStrategy
      }
      updateWorkspace(this.workspaceInfo.name, ws).then(res => {
        this.originalCacheStrategy = this.cacheStrategy
        this.$message.success("更新成功，工作区缓存策略调整为 " + this.cacheStrategy)
      }).catch(() => {
        this.cacheStrategy = this.originalCacheStrategy
      })
    },

    tabClick() {
      if (this.activeName === 'statistics') {
        this.refreshChart()
      }
    },

    // 点击文件上传下载流量查询按钮
    handleQueryUploadDownloadBtnClick() {
      this.uploadDownloadQueryCondition = {
        beginTime: this.uploadDownloadQueryRange ? this.uploadDownloadQueryRange[0].getTime() : null,
        endTime: this.uploadDownloadQueryRange ? this.$util.getMaxTimestampForDay(this.uploadDownloadQueryRange[1].getTime()) : null
      }
      this.queryUploadDownloadChart()
    },

    // 点击文件大小增量查询按钮
    handleQueryFileSizeDeltaBtnClick() {
      this.fileSizeDeltaQueryCondition = {
        beginTime: this.fileSizeDeltaQueryRange ? this.fileSizeDeltaQueryRange[0].getTime() : null,
        endTime: this.fileSizeDeltaQueryRange ? this.$util.getMaxTimestampForDay(this.fileSizeDeltaQueryRange[1].getTime()) : null
      }
      this.queryFileSizeDeltaChart()
    },

    // 点击文件数量增量查询按钮
    handleQueryFileCountDeltaBtnClick() {
      this.fileCountDeltaQueryCondition = {
        beginTime: this.fileCountDeltaQueryRange ? this.fileCountDeltaQueryRange[0].getTime() : null,
        endTime: this.fileCountDeltaQueryRange ? this.$util.getMaxTimestampForDay(this.fileCountDeltaQueryRange[1].getTime()) : null
      }
      this.queryFileCountDeltaChart()
    },

    async queryUploadDownloadChart() {  
      let res = await queryWorkspaceTraffic(this.wsName, this.uploadDownloadQueryCondition.beginTime, this.uploadDownloadQueryCondition.endTime)
      let downloadTraffic = res.data['file_download_traffic']
      let uploadTraffic = res.data['file_upload_traffic']
      this.$refs['uploadDownloadChart'].init(uploadTraffic, downloadTraffic)
    },

    async queryFileSizeDeltaChart() {  
      let res = await queryWorkspaceFileDelta(this.wsName, this.fileSizeDeltaQueryCondition.beginTime, this.fileSizeDeltaQueryCondition.endTime)
      this.$refs['fileSizeDeltaChart'].init(res.data['file_size_delta'])
    },

    async queryFileCountDeltaChart() {  
      let res = await queryWorkspaceFileDelta(this.wsName, this.fileCountDeltaQueryCondition.beginTime, this.fileCountDeltaQueryCondition.endTime)
      this.$refs['fileCountDeltaChart'].init(res.data['file_count_delta'])
    }
  },
  created(){
    this.init()
  }
  
}
</script>
<style  scoped>
.title {
  font-size: 16px;
  height: 20px;
  line-height: 20px;
  margin-bottom: 10px;
  font-weight: 600;
  text-indent: 3px;
  border-left: 5px solid #409EFF;
}
.info-container >>> .el-row {
  margin-top: 5px !important;
}
.key {
  font-size: 13px;
  font-weight: 600;
  opacity: 0.8;
}
.value {
  font-size: 14px;
  color: #606266;
}
.code {
  padding-left: 10px;
  font-size: 14px;
  color: #606266;

}
.tooltip {
  font-size: 12px;
  line-height: 18px;
}
.time-selector{
  display: flex;
  height: 100%;
  align-items: center;
  padding-right: 10px;
}
.btn-query {
  margin-left:10px;
}
</style>