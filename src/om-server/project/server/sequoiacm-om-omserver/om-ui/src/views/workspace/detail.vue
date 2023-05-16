<template>
  <div class="app-container">
      <el-tabs class="tabs" v-model="activeName" @tab-click="tabClick">
        <el-tab-pane label="基础信息" name="basic"></el-tab-pane>
        <el-tab-pane label="流量统计" name="statistics"></el-tab-pane>
      </el-tabs>
      <template v-if="activeName==='basic'">
        <el-collapse v-model="activePanel">
          <el-collapse-item name="basic_info">
            <template slot="title">
              <div class="title">
                基本信息
              </div>
            </template>
            <div class="info-container">
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
          </el-collapse-item>
          <el-collapse-item name="config">
            <template slot="title">
              <div class="title">
                工作区配置
              </div>
            </template>
            <div class="info-container">
              <el-row>
                <el-col :span="5" style="width:120px;margin-top:12px;font-size: 15px">
                  <el-tooltip effect="light" placement="top-start">
                    <div slot="content" class="tooltip">
                      文件缓存策略控制跨中心读时，文件数据是否缓存在途经的站点上，目前支持如下缓存策略：<br/>
                        <b>ALWAYS：</b>文件在跨中心读时，文件数据总是会缓存在途经的站点上<br/>
                        <b>NEVER：</b> 文件在跨中心读时，文件数据不会缓存在途经的站点上<br>
                        <b>AUTO：</b> 文件在跨中心读时，根据文件的访问频率决定是否将文件缓存在本地<br>
                    </div>
                    <span>缓存策略：</span>
                  </el-tooltip>
                </el-col>
                <el-col :span="19" style="width:200px;margin-top:12px">
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
              <el-row>
                <el-col :span="5" style="width:120px;margin-top:12px;font-size: 15px">
                  <span>标签检索：</span>
                </el-col>
                <el-col :span="19">
                  <el-radio-group v-model="tagRetrievalStatus" size="small" style="margin-top:12px;">
                    <el-radio-button label="enabled" v-if="tagRetrievalStatus!=='indexing'">
                      开启
                    </el-radio-button>
                    <el-radio-button label="indexing" v-if="tagRetrievalStatus==='indexing'">
                      开启中<i class="el-icon-loading"></i>
                    </el-radio-button>
                    <el-radio-button label="disabled" :disabled="tagRetrievalStatus==='indexing'">
                      关闭
                    </el-radio-button>
                  </el-radio-group>
                </el-col>
              </el-row>
              <el-row>
                <el-col :span="5" style="width:120px;margin-top:12px;font-size: 15px">
                  <span>标签库 Domain：</span>
                </el-col>
                <el-col :span="4">
                  <el-input v-model="workspaceInfo.tag_lib_domain" readonly style="margin-top:12px" size="small"></el-input>
                </el-col>
              </el-row>
              <el-row>
                <el-col :span="5" style="width:120px;margin-top:12px;font-size: 15px">
                  <span>数据流配置：</span>
                </el-col>
                <el-col :span="19">
                  <el-button plain
                    id="btn_ws_add_transtion"
                    size="small"
                    icon="el-icon-plus"
                    style="margin-bottom:10px;margin-top:5px"
                    @click="handleCreateBtnClick">
                      添加数据流
                  </el-button>
                  <el-table
                    :data="transitionList"
                    max-height="500px"
                    style="width: 100%"
                    v-if="transitionList.length > 0">
                    <el-table-column
                      prop="name"
                      label="数据流"
                      width="180">
                      <template slot-scope="scope">
                        {{scope.row.transition.name}}
                        <el-tag v-if="scope.row.is_customized" style="margin-left:5px" size="mini" type="info">副本</el-tag>
                        <el-tooltip content="当前数据流的状态与其生成的调度任务状态不一致，请检查！" placement="top">
                          <span><i class="el-icon-warning" v-if=scope.row.abnormalState style="margin-left:3px"></i></span>
                        </el-tooltip>
                      </template>
                    </el-table-column>
                    <el-table-column
                      prop="enabled"
                      width="120"
                      label="是否启用">
                      <template slot-scope="scope">
                        <el-switch
                          :id="'input-transition-switch-'+scope.row.name"
                          v-model="scope.row.enabled"
                          @click.native="handleChangeTransitionState($event,scope.row)"
                          :active-value="true"
                          :inactive-value="false"
                          disabled
                          active-color="#13ce66">
                        </el-switch>
                      </template>
                    </el-table-column>
                    <el-table-column
                      width="300"
                      label="操作">
                      <template slot-scope="scope">
                        <el-button-group>
                          <el-button id="btn_ws_transition_search" size="mini" @click="handleSearchTsBtnClick(scope.row)">查看</el-button>
                          <el-button id="btn_ws_transition_edit" size="mini" @click="handleUpdateTsBtnClick(scope.row)">编辑</el-button>
                          <el-button id="btn_ws_transition_delete" type="danger" size="mini" @click="handleDeleteTsBtnClick(scope.row)">移除</el-button>
                        </el-button-group>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-col>
              </el-row>
            </div>
          </el-collapse-item>
          <el-collapse-item name="site_info">
            <template slot="title">
              <div class="title">
                站点信息
              </div>
            </template>
            <div class="info-container">
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
          </el-collapse-item>
          <el-collapse-item name="meta_info">
            <template slot="title">
              <div class="title">
                元数据信息
              </div>
            </template>
            <div class="info-container">
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
          </el-collapse-item>
          <el-collapse-item name="statistics_info">
            <template slot="title">
              <div class="title">
                统计信息
              </div>
            </template>
            <div class="info-container">
              <el-row>
                <el-col :span="12"><span class="key">目录数：</span> <span class="value"><el-tag size="small" >{{workspaceInfo.directory_count}}</el-tag></span></el-col>
                <el-col :span="12"><span class="key">文件数：</span> <span class="value"><el-tag size="small">{{workspaceInfo.file_count}}</el-tag></span></el-col>
              </el-row>
              <el-row>
                <el-col :span="12"><span class="key">批次数：</span> <span class="value"><el-tag size="small">{{workspaceInfo.batch_count}}</el-tag></span></el-col>
              </el-row>
            </div>
          </el-collapse-item>
        </el-collapse>
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

      <!-- 编辑数据流弹框 -->
      <transition-edit-dialog ref="transitionEditDialog" @onTransitionEdited="queryTransitionList"></transition-edit-dialog>
      <!-- 数据流详情弹框 -->
      <transition-detail-dialog ref="transitionDetailDialog"></transition-detail-dialog>
  </div>
</template>
<script>
import FileUploadDownloadChart from './components/FileUploadDownloadChart.vue'
import FileCountDeltaChart from '@/views/workspace/components/FileCountDeltaChart'
import FileSizeDeltaChart from '@/views/workspace/components/FileSizeDeltaChart'
import Panel from '@/components/Panel'
import TransitionEditDialog from '../lifecycle/transition/components/TransitionEditDialog.vue'
import TransitionDetailDialog from '../lifecycle/transition/components/TransitionDetailDialog.vue'
import {queryWorkspaceDetail, updateWorkspace, queryWorkspaceTraffic, queryWorkspaceFileDelta, queryWorkspaceBasic} from '@/api/workspace'
import { Loading } from 'element-ui'
import {listTransitionByWs, removeTransition, changeTransitionState} from '@/api/lifecycle'
export default {
  components: {
    TransitionEditDialog,
    TransitionDetailDialog,
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
      timer: null,         // 定时检查标签开关状态
      tagRetrievalStatus: '',
      watchTagRetrievalStatus: false,
      siteCacheStrategies: [{
        value : 'ALWAYS', label : 'ALWAYS'
      }, {
        value : 'NEVER', label : 'NEVER'
      }, {
        value : 'AUTO', label : 'AUTO'
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
      wsName: this.$route.params.name,
      // 需要展开的面板（展开所有）
      activePanel: [
        'basic_info',
        'config',
        'site_info',
        'meta_info',
        'statistics_info'
      ],
      // 工作区下的数据流列表
      transitionList: []
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
      this.tagRetrievalStatus = this.workspaceInfo.tag_retrieval_status
      await this.queryTransitionList()
    },
    queryTransitionList() {
      listTransitionByWs(this.workspaceInfo.name).then(res => {
        this.transitionList = res.data
        // 检查数据流状态与调度任务是否一致
        this.transitionList.forEach(ele => {
          ele['abnormalState'] = this.isAbnormalState(ele)
        });
      })
    },

    // 刷新图表数据
    async refreshChart() {
      this.queryUploadDownloadChart()
      this.queryFileCountDeltaChart()
      this.queryFileSizeDeltaChart()
    },

    // 更改工作区的缓存策略
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
      this.$refs['fileSizeDeltaChart'].init(res.data['size_delta'])
    },

    async queryFileCountDeltaChart() {
      let res = await queryWorkspaceFileDelta(this.wsName, this.fileCountDeltaQueryCondition.beginTime, this.fileCountDeltaQueryCondition.endTime)
      this.$refs['fileCountDeltaChart'].init(res.data['count_delta'])
    },

    // 检查数据流下调度任务的状态是否正常
    isAbnormalState(transition) {
      let abnormalState = false
      if (transition.schedules && transition.schedules.length > 0) {
        transition.schedules.forEach(ele => {
          if (ele.enable != transition.enabled) {
            abnormalState = true
          }
        });
      }
      return abnormalState
    },
    // 点击更改数据流状态
    handleChangeTransitionState(e, row) {
      let action = row.enabled ? '禁用' : '启用'
      let confirmMsg = `您确认${action}数据流 ${row.transition.name} 吗`
      this.$confirm(confirmMsg, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_transition_confirmChangeState',
        type: 'warning'
      }).then(() => {
        changeTransitionState(row.workspace, row.transition.name, !row.enabled).then(res => {
          this.$message.success(`数据流${action}成功`)
          row.enabled = !row.enabled
        })
      })
    },
    // 点击新增数据流按钮
    handleCreateBtnClick() {
      this.$refs['transitionEditDialog'].show('ws_add', null, this.workspaceInfo.name)
    },
    // 点击查看数据流详情
    handleSearchTsBtnClick(row) {
      this.$refs['transitionDetailDialog'].show(row.transition, row)
    },
    // 点击编辑数据流
    handleUpdateTsBtnClick(row) {
      this.$refs['transitionEditDialog'].show('ws_update', row.transition, this.workspaceInfo.name)
    },
    // 点击移除数据流按钮
    handleDeleteTsBtnClick(row) {
      this.$confirm(`您确定移除数据流【${row.transition.name}】吗`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        let loadingInstance = Loading.service({ fullscreen: true, text: "正在删除中..." })
        removeTransition(row.workspace, row.transition.name).then(res => {
          this.$message.success(`删除成功`)
          this.queryTransitionList()
        }).finally(() => {
          loadingInstance.close()
        })
      })
    },
    async tagRetrievalStatusChecker() {
      let res = await queryWorkspaceBasic(this.$route.params.name, true)
      let workspaceInfo = JSON.parse(res.headers['workspace'])
      if (workspaceInfo.tag_retrieval_status !== 'indexing') {
        this.tagRetrievalStatus = workspaceInfo.tag_retrieval_status
      }
    }
  },
  created(){
    this.init()
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },
  watch: {
    async tagRetrievalStatus(newValue, oldValue) {
      if (!this.watchTagRetrievalStatus) {
        this.watchTagRetrievalStatus = true
        return
      }

      if (oldValue === 'indexing') {
        clearInterval(this.timer)
        this.timer = null
        if (newValue === 'enabled') {
          this.$message.success("开启工作区标签检索成功！")
        } else {
          this.$message.error("开启工作区标签检索失败！")
        }
        return
      }

      if (newValue === 'indexing') {
        this.timer = setInterval(() => {
          this.tagRetrievalStatusChecker()
        }, 2000)
        return
      }

      // 修改标签检索状态
      try {
        if (newValue === 'enabled') {
          let ws = { tagRetrievalEnabled : true}
          let res = await updateWorkspace(this.workspaceInfo.name, ws)
          this.$message.success("正在开启工作区标签检索")
          this.tagRetrievalStatus = 'indexing'
        } else {
          let ws = { tagRetrievalEnabled : false }
          let res = await updateWorkspace(this.workspaceInfo.name, ws)
          this.$message.success("关闭工作区标签检索成功")
        }
      } catch (error) {
		    // 修改失败时还原标签状态，并且本次状态的变更不需要 watch（避免进入死循环）
        this.tagRetrievalStatus = oldValue
        this.watchTagRetrievalStatus = false
      }
    }
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
.app-container >>> .el-switch.is-disabled{
  opacity: 1;
}
.app-container .el-switch.is-disabled >>>  .el-switch__core{
  cursor: pointer;
}
</style>
