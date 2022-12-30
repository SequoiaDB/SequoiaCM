<template>
  <div>
    <!-- 创建工作区对话框 -->
    <el-dialog
      class="edit-dialog"
      top="30px"
      title="创建工作区"
      :visible.sync="dialogVisible"
      width="750px">
      <el-form ref="form" :rules="rules" :model="form" size="small" label-width="120px">
         <el-form-item label="工作区名称" prop="workspaceNames">
            <el-tag
              :key="name"
              v-for="name in form.workspaceNames"
              closable
              :disable-transitions="false"
              @close="removeWs(name)"
              class="ws-tag">
                <span>{{name}}</span>
            </el-tag>
            <el-row type="flex" justify="space-between">
              <el-col :span="19">
                <el-input
                  maxlength="50"
                  class="input-new-workspace"
                  placeholder="请输入工作区名称"
                  v-model="workspaceNameInputValue"
                  size="small">
                </el-input>
              </el-col>
              <el-col :span="4">
                <el-button class="button-new-workspace" size="small" @click="addWs">+ 继续添加</el-button>
              </el-col>
            </el-row>
          </el-form-item>

          <el-row>
            <el-col :span="12">
              <el-form-item>
                <template slot="label">
                  缓存策略
                  <el-tooltip effect="dark" placement="bottom">
                    <div slot="content">
                      文件缓存策略控制跨中心读时，文件数据是否缓存在途经的站点上，目前支持如下缓存策略：<br>
                      <b>ALWAYS</b>：文件在跨中心读时，文件数据总是会缓存在途经的站点上<br>
                      <b>NEVER</b>： 文件在跨中心读时，文件数据不会缓存在途经的站点上
                    </div>
                    <i class="el-icon-question"></i>
                  </el-tooltip>
                </template>
                <el-select
                    v-model="form.cacheStrategy"
                    size="small"
                    placeholder="无数据"
                    filterable
                    style="width:100%">
                    <el-option v-for="item in siteCacheStrategies"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value">
                    </el-option>
                  </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item >
                <template slot="label">
                  优先站点
                  <el-tooltip class="item" effect="dark" placement="bottom">
                    <div slot="content">
                      指定工作区的优先站点，当 S3 桶使用该工作区作为 region 时，对应的 S3 协议请求将转发至优先站点下的 S3 站点
                    </div>
                    <i class="el-icon-question"></i>
                  </el-tooltip>
                </template>
                <el-select v-model="form.preferred" placeholder="请选择优先站点" clearable>
                  <el-option v-for="site in sites" :key="site.id" :label="site.name" :value="site.name"></el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="工作区描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="请输入工作区描述"
              >
            </el-input>
          </el-form-item>

          <el-form-item label="元数据存储站点">
            <el-tabs :value="rootSiteName" type="border-card" style="margin-top:5px">
              <el-tab-pane :label="rootSiteName" :name="rootSiteName">
                <sdb-meta-location :siteName="rootSiteName" ref="metaLocation"/>
              </el-tab-pane>
            </el-tabs>
          </el-form-item>

          <el-form-item label="内容存储站点">
            <el-row>
              <el-col :span="15">
                <el-select v-model="selectedSite" placeholder="请选择站点" style="width:100%">
                  <el-option v-for="site in iterSites" :key="site.id" :label="site.name" :value="site.name"></el-option>
                </el-select>
              </el-col>
              <el-col :span="5">
                <el-button type="primary" style="margin-left:5px" @click="addSite(selectedSite)">添加站点</el-button>
              </el-col>
            </el-row>
            <el-tabs v-model="activeSite" type="border-card" class="datasource-tabs" closable @tab-remove="removeSite" style="margin-top:5px">
              <el-tab-pane
                v-for="(site, index) in addedSites"
                :key="index"
                :label="site.name"
                :name="site.name">
                <!-- sequoaidb -->
                <sdb-data-location v-if="site.datasource_type === 'sequoiadb'" :ref="'dataLocation_'+site.name" :siteName="site.name"/>

                <!-- s3 -->
                <ceph-s3-data-location v-if="site.datasource_type === 'ceph_s3'" :ref="'dataLocation_'+site.name" :siteName="site.name"/>

                <!-- hbase -->
                <hbase-data-location v-if="site.datasource_type === 'hbase'" :ref="'dataLocation_'+site.name" :siteName="site.name"/>

                <!-- swift -->
                <ceph-swift-data-location v-if="site.datasource_type === 'ceph_swift'" :ref="'dataLocation_'+site.name" :siteName="site.name"/>

                <!-- hdfs -->
                <hdfs-data-location v-if="site.datasource_type === 'hdfs'" :ref="'dataLocation_'+site.name" :siteName="site.name"/>

                <!-- sftp -->
                <sftp-data-location v-if="site.datasource_type === 'sftp'" :ref="'dataLocation_'+site.name" :siteName="site.name"/>
              </el-tab-pane>
            </el-tabs>
          </el-form-item>
          <el-collapse v-model="collapseValue" >
            <el-collapse-item title="其它配置" name="otherConfig">
              <el-form-item label="目录开关" >
                <el-switch v-model="form.directoryEnabled"></el-switch>
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="close" size="mini">关 闭</el-button>
        <el-button type="primary" @click="submitForm" :disabled="saveBtnDisabled" size="mini">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>

import {createWorkspace} from '@/api/workspace'
import {querySiteList} from '@/api/site'

import ScmShardingSelect from '@/components/selector/ScmShardingSelect'
import SdbDataLocation from './DataLocation/SdbDataLocation'
import CephS3DataLocation from './DataLocation/CephS3DataLocation'
import HbaseDataLocation from './DataLocation/HbaseDataLocation'
import CephSwiftDataLocation from './DataLocation/CephSwiftDataLocation'
import HdfsDataLocation from './DataLocation/HdfsDataLocation'
import SftpDataLocation from './DataLocation/SftpDataLocation'
import SdbMetaLocation from './DataLocation/SdbMetaLocation'

export default {
  components: {
    ScmShardingSelect,
    SdbDataLocation,
    CephS3DataLocation,
    HbaseDataLocation,
    CephSwiftDataLocation,
    HdfsDataLocation,
    SftpDataLocation,
    SdbMetaLocation
  },
  props: {
  },
  data() {
    let wsNamesValidation = (rule, value, callback) => {
      if (this.form.workspaceNames.length === 0 && !this.workspaceNameInputValue) {
        callback(new Error('请填写工作区名称'));
      } else {
        callback()
      }
    }
    return{
      saveBtnDisabled: false,
      collapseValue: '',
      selectedSite:'',
      rootSiteName: '',
      dialogVisible: false,
      workspaceNameInputValue: '',
      addedSites: [],
      activeSite:'',
      rules: {
        workspaceNames:{required: true, trigger: 'blur', validator: wsNamesValidation}
      },
      form: {
        workspaceNames: [],
        directoryEnabled:false,
        cacheStrategy:"ALWAYS",
        preferred: '',
        description: ''
      },
      sites: [],
      siteCacheStrategies: [{
        value : 'ALWAYS', label : 'ALWAYS'
      }, {
        value : 'NEVER', label : 'NEVER'
      }]
    }
  },
  methods: {
    // 打开弹框
    show() {
      if (this.sites.length <= 0) {
        querySiteList().then(res => {
          this.sites = res.data
          this.init()
          this.dialogVisible = true
        })
      } else {
        this.init()
        this.dialogVisible = true
      }
    },

    // 关闭弹框
    close() {
      this.dialogVisible = false
    },

    // 初始化弹框
    init() {
      for (const site of this.sites) {
        if (site.is_root_site) {
          if (!this.addedSites.some(item => item.is_root_site)) {
            this.addedSites.push(site)
          }
          this.rootSiteName = site.name
          this.activeSite = site.name
        }
      }
    },

    // 清除输入框数据
    clear() {
      setTimeout(()=>{
        if (this.$refs.metaLocation) {
          this.$refs.metaLocation.clear()
        }
        for (const site of this.addedSites) {
          let key = 'dataLocation_' + site.name
          if (this.$refs[key] && this.$refs[key][0]) {
            this.$refs[key][0].clear()
          }
        }
        this.collapseValue = '',
        this.selectedSite = '',
        this.rootSiteName = '',
        this.dialogVisible = false,
        this.workspaceNameInputValue = '',
        this.addedSites = [],
        this.activeSite = '',
        this.clearForm()
      }, 500)
    },

    // 添加内容存储站点
    addSite(){
      let siteName = this.selectedSite
      if (!siteName) {
        this.$message.warning("请选择站点")
        return
      }
      let selectedSite = {}
      for (const i in this.sites) {
        if (this.sites[i].name === siteName) {
          selectedSite = {...this.sites[i]}
        }
      }
      this.addedSites.push(selectedSite)
      this.activeSite = siteName
      this.selectedSite = ''
    },

    // 移除内容存储站点
    removeSite(targetName) {
      let tabs = this.addedSites
      if (this.activeSite === targetName) {
        tabs.forEach((tab, index) => {
          if (tab.name === targetName) {
            let nextTab = tabs[index + 1] || tabs[index - 1]
            if (nextTab) {
              this.activeSite = nextTab.name
            }
          }
        });
      }
      this.addedSites = tabs.filter(tab => tab.name !== targetName)
    },

     // 移除工作区名称
    removeWs(tag) {
      this.form.workspaceNames.splice(this.form.workspaceNames.indexOf(tag), 1)
    },

    // 添加工作区名称
    addWs() {
      let workspaceNameInputValue = this.workspaceNameInputValue
      if (workspaceNameInputValue) {
        if (this.form.workspaceNames.some(e => e === workspaceNameInputValue)){
          this.$message.error("已存在工作区：" + workspaceNameInputValue)
          return
        }
        this.form.workspaceNames.push(workspaceNameInputValue)
        this.workspaceNameInputValue = ''
      } else {
        this.$message.warning("请先输入工作区名称")
      }

    },

    // 提交表单（上传文件）
    submitForm() {
      if (this.saveBtnDisabled) {
        return
      }
      this.saveBtnDisabled = true
      this.$refs['form'].validate(valid => {
        if (valid) {
          let workspaceNames = [...this.form.workspaceNames]
          if (this.workspaceNameInputValue) {
            if (workspaceNames.some(item => item === this.workspaceNameInputValue)) {
              this.$message.warning("工作区名称重复：" + this.workspaceNameInputValue)
              this.saveBtnDisabled = false
              return
            } else {
              workspaceNames.push(this.workspaceNameInputValue)
            }
          }
          if (!this.$refs['metaLocation'].validate()) {
            this.saveBtnDisabled = false
            return
          }
          let formData = {
            workspace_names: workspaceNames,
            cache_strategy: this.form.cacheStrategy,
            directory_enabled: this.form.directoryEnabled,
            meta_location: this.$refs['metaLocation'].toFormData(),
            description: this.form.description
          }
          let dataLocations = []
          for (const site of this.addedSites) {
            let key = 'dataLocation_' + site.name
            if (!this.$refs[key][0].validate()) {
              this.activeSite = site.name
              this.saveBtnDisabled = false
              return
            }
            dataLocations.push(this.$refs[key][0].toFormData())
          }
          formData['data_locations'] = dataLocations
          if (this.form.preferred) {
            formData['preferred'] = this.form.preferred
          }
          createWorkspace(formData).then(res => {
            this.$util.showBatchOpMessage("创建工作区", res.data)
            this.$emit("onWorkspaceCreated")
            this.clear()
            this.close()
          }).finally(()=>{
            this.saveBtnDisabled = false
          })
        } else {
          this.saveBtnDisabled = false
        }
      })
    },

    // 重置表单
    clearForm() {
      if (this.$refs['form']) {
        this.$refs['form'].resetFields()
      }
      this.form = {
        workspaceNames: [],
        directoryEnabled:false,
        cacheStrategy:"ALWAYS",
        preferred: '',
        description: ''
      }
    },
  },

  mounted(){
    querySiteList().then(res => {
      this.sites = res.data
    })
  },

  computed: {
    iterSites(){
      return this.sites.filter(item => {
        for (const i in this.addedSites) {
          if (this.addedSites[i].name === item.name) {
            return false
          }
        }
        return true
      })
    }
  }
}
</script>

<style  scoped>
.edit-dialog >>> .el-dialog__body {
  padding: 20px;
}
.edit-container >>> .el-row {
  margin-top: 8px !important;
}
.el-tag {
  margin-left: 3px;
  margin-bottom: 3px;
}
.button-new-workspace {
  margin-left: 3px;
}
.site-label{
  font-size: 12px;
  color: #606266;
  font-weight: 700;
}
.required:before {
    content: '*';
    color: #F56C6C;
    margin-right: 4px;
}
.datasource-tabs >>> .el-tabs__nav .el-tabs__item:first-child .el-icon-close {
  display: none;
}
</style>
