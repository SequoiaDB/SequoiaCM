<template>
  <div>
    <!-- 动态刷新属性对话框 -->
    <el-dialog
      :title="'修改 ' + getResourceName() + ' 配置'"
      :visible.sync="updatePropDialogVisible"
      width="700px">
      <el-collapse v-model="activeNames" >
        <el-collapse-item
          title="基本参数配置"
          name="basic"
          v-if="service.isContentServer">
          <table class="table is-fullwidth">
            <tr v-for="(item, index) in configProps" :key="index">
              <td>
                <span v-text="item.key" />
              </td>
              <td>
                <el-input v-model="item.value"></el-input>
              </td>
            </tr>
          </table>
        </el-collapse-item>
        <el-collapse-item
          title="慢操作告警配置"
          name="slowlog"
          v-if="service.name === 'gateway' || service.isContentServer || service.isS3Server">
          <el-form label-width="80px">
            <el-form-item label="启用状态">
              <el-switch
                v-model="slowLog.enabled">
              </el-switch>
            </el-form-item>
            <el-form-item label="告警阈值">
              <el-tabs v-model="activeSlowLogFlag" type="border-card">
                <el-tab-pane
                  label="请求告警"
                  name="request">
                  <el-form-item label="全局请求">
                    <el-input maxlength="10" v-number-only v-model.number="slowLog.allRequest" placeholder="请输入全局请求告警阈值，单位：ms；默认：-1（表示无限大）" style="width:90%"></el-input> ms
                  </el-form-item>
                  <el-form-item label="单个请求" style="margin-top:5px">
                    <el-table :data="slowLog.requestList" size="mini" border>
                      <el-table-column label="请求路径" prop="edit" width="150%">
                        <template slot-scope="scope">
                          <el-input maxlength="60" placeholder="例:/api/v1/files" v-model="scope.row.path"></el-input>
                        </template>
                      </el-table-column>
                      <el-table-column label="请求方式" prop="edit">
                        <template slot-scope="scope">
                          <el-select v-model="scope.row.method" placeholder="">
                            <el-option label="POST" value="POST"></el-option>
                            <el-option label="GET" value="GET"></el-option>
                            <el-option label="HEAD" value="HEAD"></el-option>
                            <el-option label="DELETE" value="DELETE"></el-option>
                            <el-option label="PUT" value="PUT"></el-option>
                          </el-select>
                        </template>
                      </el-table-column>
                      <el-table-column label="告警阈值" prop="edit">
                        <template slot-scope="scope">
                          <el-input maxlength="10" v-number-only v-model.number="scope.row.value"  style="width:70%"></el-input> ms
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="90">
                        <template slot-scope="scope">
                          <el-button type="danger" size="mini" @click="removeSlowLogRequestConfig(scope.$index)">移除</el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                    <el-button type="primary" size="mini" @click="addSlowLogRequestConfig">+</el-button>
                  </el-form-item>
                </el-tab-pane>
                <el-tab-pane
                  label="操作告警"
                  name="operation">
                  <el-form-item label="全局操作">
                    <el-input v-model.number="slowLog.allOperation" v-number-only maxlength="10" placeholder="请输入全局操作告警阈值，单位：ms；默认：-1（表示无限大）" style="width:90%"></el-input> ms
                  </el-form-item>
                  <el-form-item label="单个操作"  style="margin-top:5px">
                    <el-table :data="slowLog.operationList" size="mini" border>
                      <el-table-column label="操作名" prop="edit">
                         <template slot-scope="{}" slot="header">
                          <span>操作名</span>
                          <el-tooltip effect="dark" placement="top" content="操作名的具体含义请参：用户手册->运维指南->诊断日志->慢操作日志">
                            <i class="el-icon-question"></i>
                          </el-tooltip>
                        </template>
                        <template slot-scope="scope">
                          <el-autocomplete
                            :maxlength="30"
                            class="inline-input"
                            v-model="scope.row.name"
                            :fetch-suggestions="queryOperationName"
                            placeholder="请输入操作名"
                          ></el-autocomplete>
                        </template>
                      </el-table-column>
                      <el-table-column label="告警阈值" prop="edit">
                        <template slot-scope="scope">
                          <el-input maxlength="10" v-number-only v-model.number="scope.row.value" placeholder="请输入告警阈值" style="width:80%"></el-input> ms
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="90">
                        <template slot-scope="scope">
                          <el-button type="danger" size="mini" @click="removeSlowLogOperationConfig(scope.$index)">移除</el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                    <el-button type="primary" size="mini" @click="addSlowLogOperationConfig">+</el-button>
                  </el-form-item>
                </el-tab-pane>
              </el-tabs>
            </el-form-item>
            <el-form-item label="输出位置">
              <el-checkbox-group v-model="slowLog.appender">
              <el-checkbox label="LOGGER">日志文件</el-checkbox>
              <el-checkbox label="TRACER">链路追踪系统</el-checkbox>
            </el-checkbox-group>
            </el-form-item>
          </el-form>
        </el-collapse-item>

        <el-collapse-item
          title="链路追踪配置"
          name="tracer"
          v-if="service.name === 'gateway'">
          <el-form label-width="80px">
            <el-form-item label="启用状态">
              <el-switch
                v-model="tracer.enabled">
              </el-switch>
            </el-form-item>
            <el-form-item label="采样率">
              <el-input maxlength="3" v-number-only="{minValue:0, maxValue:100}" v-model.number="tracer.samplePercentage" style="width:20%"></el-input> %
            </el-form-item>
           </el-form>
           <el-alert
            v-if="!isServiceTraceExist && tracer.enabled"
            title="当前系统中没有可用的链路追踪节点，以上配置可能不会起作用！"
            :closable="false"
            type="warning"
            show-icon>
          </el-alert>
        </el-collapse-item>
      </el-collapse>

      <span slot="footer" class="dialog-footer">
        <el-button id="btn_update_prop_close" size="mini" @click=close>关 闭</el-button>
        <el-button id="btn_update_properties" type="primary" size="mini" @click=handleUpdateProperties>保存配置</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {updateProperties} from '@/api/config-props'
import numberOnly from '@/directives/numberOnly'

export default {
  props:{
    type: String,
    service: Object,
    instance: Object,
    configProps: Array,
    allConfig: Object,
    isServiceTraceExist: {type: Boolean, default: false}
  },
  directives: {
    numberOnly
  },
  data() {
    return{
      updatePropDialogVisible: false,
      activeNames: ['basic'],
      slowLog: {
        enabled: false,
        allRequest: -1,
        allOperation: -1,
        appender: ["TRACER", "LOGGER"],
        requestList: [],
        operationList: [],
        oldRequestList: [],
        oldOperationList: []
      },
      activeSlowLogFlag: 'request',
      tracer: {
        enabled: false,
        samplePercentage: 10
      },
      operations:[
        {value: "openWriter"},
        {value: "writeData"},
        {value: "closeWriter"},
        {value: "calcMD5"},
        {value: "preCreate"},
        {value: "postCreate"},
        {value: "accessMeta"},
        {value: "openReader"},
        {value: "readData"},
        {value: "seekData"},
        {value: "closeReader"},
        {value: "acquireLock"},
        {value: "releaseLock"},
        {value: "feign"}
    ]
    }
  },
  methods: {
    // 获取需要更新的资源（服务/节点）名称
    getResourceName() {
      if (this.type === 'service') {
        return this.service.name
      } else if (this.type === 'instance') {
        return this.instance.instance_id
      }
      return ''
    },

    // 添加操作告警列表项
    addSlowLogOperationConfig() {
      this.slowLog.operationList.push({ name: '', value: ''});
    },

    // 添加请求告警列表项
    addSlowLogRequestConfig() {
      this.slowLog.requestList.push({ method: '', value: '', path:'' });
    },

    // 移除请求告警列表项
    removeSlowLogRequestConfig(index) {
      this.slowLog.requestList.splice(index, 1);
    },

    // 移除操作告警列表项
    removeSlowLogOperationConfig(index) {
      this.slowLog.operationList.splice(index, 1);
    },

    // 查询操作名
    queryOperationName(queryString, cb) {
      var operations = this.operations;
      var results = queryString ? operations.filter(this.createFilter(queryString)) : operations
      cb(results);
    },

    createFilter(queryString) {
      return (op) => {
        return (op.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0);
      }
    },

    show() {
      this.clearForm()
      this.processNodeConfig()
      this.updatePropDialogVisible = true
    },

    close() {
      this.updatePropDialogVisible = false
      setTimeout(() => {
        this.clearForm()
      }, 500);
    },

    // 解析节点配置
    processNodeConfig(){
      for (let [key, value] of Object.entries(this.allConfig)) {
        try {
          this.process(key, value)
        } catch(e) {
          log.error("failed to process key:" + key + ", value=" + value)
        }
      }
      this.slowLog.oldRequestList = JSON.parse(JSON.stringify(this.slowLog.requestList))
      this.slowLog.oldOperationList = JSON.parse(JSON.stringify(this.slowLog.operationList))
    },

    process(key, value) {
      if (key === 'scm.slowlog.enabled') {
        if ("true" === value.toLowerCase()) {
          this.slowLog.enabled = true
        } else {
          this.slowLog.enabled = false
        }
      }
      if (key === 'scm.trace.enabled') {
        if ("true" == value.toLowerCase()) {
          this.tracer.enabled = true
        } else {
          this.tracer.enabled = false
        }
      }
      if (key === 'scm.trace.samplePercentage') {
        this.tracer.samplePercentage = Number(value)
      }
      if (key === 'scm.slowlog.allRequest') {
        this.slowLog.allRequest = Number(value)
      }
      if (key === 'scm.slowlog.appender') {
        this.slowLog.appender = value.split(",")
      }
      let prefix = 'scm.slowlog.request.'
      if (key.length > prefix.length && key.substring(0, prefix.length) === prefix) {
        let req = key.substring(prefix.length)
        req = req.replace('[', '').replace(']', '')
        if (req.indexOf('/') !== -1) {
           let method = req.substring(0, req.indexOf('/'))
          let path = req.substring(req.indexOf('/'))
          this.slowLog.requestList.push({
            method: method.toUpperCase(),
            path: path,
            value: Number(value)
          })
        }
      }
      prefix = 'scm.slowlog.operation.'
      if (key.length > prefix.length && key.substring(0, prefix.length) === prefix) {
        let opName = key.substring(prefix.length)
        this.slowLog.operationList.push({
          name: opName,
          value: Number(value)
        })
      }
    },

    clearForm() {
      this.slowLog = {
        enabled: false,
        allRequest: -1,
        allOperation: -1,
        appender: ["TRACER", "LOGGER"],
        requestList: [],
        operationList: []
      }
      this.tracer = {
        enabled: false,
        samplePercentage: 10
      }
    },

    // 刷新配置
    handleUpdateProperties() {
      if (!this.checkParams()) {
        return
      }
      let confPropParam = {}
      confPropParam['target_type'] = this.type
      confPropParam['targets'] = [ this.getResourceName() ]

      let properties = {}
      let deletedKeys = []
      if (this.service.isContentServer) {
        this.configProps.forEach((item) =>{
          properties[item.key] = item.value
        })
      }
      if (this.service.isContentServer || this.service.isS3Server || this.service.name === 'gateway') {
        this.appendSlowLogProperties(properties, deletedKeys)
      }
      if (this.service.name == 'gateway') {
        this.appendTracerProperties(properties, deletedKeys)
      }
      confPropParam['update_properties'] = properties
      confPropParam['delete_properties'] = deletedKeys

      updateProperties(confPropParam).then(res => {
        this.updatePropDialogVisible = false
        this.$emit('refreshConfig')
        let result = res.data
        if (result.failures.length > 0) {
          this.$message.error("成功刷新 " + result.successes.length + " 个节点配置, 刷新 " + result.failures.length + " 个节点失败")
        }
        else {
          this.$message.success("成功刷新 " + result.successes.length + " 个节点配置")
        }
      })
    },

    // 添加链路追踪配置
    appendTracerProperties(properties, deletedKeys) {
      properties['scm.trace.enabled'] = this.tracer.enabled
      properties['scm.trace.samplePercentage'] = this.tracer.samplePercentage
    },

    // 添加慢操作配置
    appendSlowLogProperties(properties, deletedKeys) {
      properties['scm.slowlog.enabled'] = this.slowLog.enabled
      properties['scm.slowlog.allRequest'] = this.slowLog.allRequest
      properties['scm.slowlog.allOperation'] = this.slowLog.allOperation
      properties['scm.slowlog.appender'] = this.slowLog.appender.join(",")
      for (const request of this.slowLog.requestList) {
        if (!request.method && !request.value && !request.path) {
          continue
        }
        let key = `scm.slowlog.request.[${request.method}${request.path}]`
        properties[key] = request.value
      }
      for (const old of this.slowLog.oldRequestList) {
        if (!this.slowLog.requestList.some(item => item.path === old.path && item.method === old.method)) {
          let key = `scm.slowlog.request.[${old.method}${old.path}]`
          deletedKeys.push(key)
        }
      }
      for (const op of this.slowLog.operationList) {
        if (!op.name && !op.value) {
          continue
        }
        let key = `scm.slowlog.operation.${op.name}`
        properties[key] = op.value
      }
      for (const old of this.slowLog.oldOperationList) {
        if (!this.slowLog.operationList.some(item => item.name === old.name)) {
          let key = `scm.slowlog.operation.${old.name}`
          deletedKeys.push(key)
        }
      }
    },

    checkParams() {
      for (const request of this.slowLog.requestList) {
        if (!request.method && !request.value && !request.path) {
          continue
        } else if (!request.method || !request.value || !request.path){
          this.$message.warning("请求告警配置不完整")
          return false
        }else if (request.path && request.path.charAt(0) !== '/') {
          this.$message.warning(`请求路径:${request.path}不正确`)
          return false
        }
      }

      for (const op of this.slowLog.operationList) {
        if (!op.name && !op.value) {
          continue
        } else if (!op.name || !op.value) {
          this.$message.warning("操作告警配置不完整")
          return false
        }
      }

      return true
    }
  }
}
</script>

<style  scoped>
.table td, .table th {
    border: 1px solid #dbdbdb;
    border-width: 0 0 1px;
    padding: .5em .75em;
    vertical-align: top;
}
table td, table th {
    text-align: left;
    vertical-align: top;
}
td, th {
    padding: 0;
    text-align: left;
}
*, :after, :before {
    -webkit-box-sizing: inherit;
    box-sizing: inherit;
}
td {
    display: table-cell;
    vertical-align: inherit;
}
</style>

