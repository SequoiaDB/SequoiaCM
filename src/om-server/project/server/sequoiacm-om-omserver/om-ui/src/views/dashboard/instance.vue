<template>
  <div>
    <div class="details-header" v-show="instanceInfo.service_name">
      <div class="title">{{instanceInfo.service_name}}</div>
      <div class="sub-title">{{('http://' + instanceInfo.host_name + ':' + instanceInfo.port)}}</div>
      <el-tabs class="tabs" v-model="activeName">
        <el-tab-pane label="基础信息" name="basic"></el-tab-pane>
        <el-tab-pane label="节点配置" name="nodeConfig"></el-tab-pane>
      </el-tabs>
    </div>
    <div class="app-container">
      <!-- 基础信息 -->
      <template v-if="activeName == 'basic'">
        <div class="panel-group">
          <div class="panel-item">
            <instance-info :instanceInfo="instanceInfo"></instance-info>
            <process-info :instanceId="$route.params.id"></process-info>
          </div>
          <panel title="连接数" class="panel-item limit-height">
            <connection-chart :instanceId="$route.params.id" class="chart"/>
          </panel>
        </div>
        <div class="panel-group">
          <panel title="堆内存" class="panel-item">
            <memory-chart :instanceId="$route.params.id" class="chart"/>
          </panel>
          <panel title="线程数" class="panel-item">
            <thread-chart :instanceId="$route.params.id" class="chart"/>
          </panel>
        </div>
      </template>

      <!-- 配置信息 -->
      <template v-if="activeName == 'nodeConfig'">
        <div class="search-button">
          <el-input
            placeholder="key/value filter"
            v-model="filter"
            clearable>
          </el-input>
        </div>
        <panel class="node_config">
          <table class="table is-fullwidth">
            <tr v-for="(value, key) in filteredConfigInfo" :key="key">
              <td>
                <span v-text="key" /><br>
              </td>
              <td>
                <span class="is-breakable" v-text="value" />
                <span v-if="isUpdatableProp(key)" class="el-icon-edit" @click="handleClickChangeConfig"/>
              </td>
            </tr>
          </table>
        </panel>
      </template>
    </div>
    <update-prop-dialog ref="updatePropDialog" :type="updatePropType" :name="currentInstance" :configProps="configProps" @refreshConfig="refreshConfigInfo"></update-prop-dialog>
  </div>
</template>

<script>
import Panel from '@/components/Panel'
import ConnectionChart from '@/views/dashboard/components/connectionChart'
import MemoryChart from '@/views/dashboard/components/memoryChart'
import ThreadChart from '@/views/dashboard/components/threadChart'
import ProcessInfo from './components/processInfo.vue'
import InstanceInfo from './components/instanceInfo.vue'
import { getInstanceInfo, getConfigInfo } from '@/api/monitor'
import UpdatePropDialog from './components/UpdatePropDialog.vue'
import {JOB_CONFIG_PROPS} from '@/utils/common-define'

export default {
  components: {
    Panel,
    ConnectionChart,
    ThreadChart,
    MemoryChart,
    ProcessInfo,
    InstanceInfo,
    UpdatePropDialog
  },

  data(){
    return {
      updatePropType: 'instance',
      instanceInfo: {},
      currentInstance: '',
      configProps: [],
      configInfo: {},
      activeName: 'basic',
      filter: ''
    }
  },
  async created() {
    this.currentInstance = this.$route.params.id
    await getInstanceInfo(this.$route.params.id).then(res => {
      this.instanceInfo = res.data
    })
    await this.refreshConfigInfo()
  },
  methods: {
    isUpdatableProp(key) {
      for (var i = 0; i < JOB_CONFIG_PROPS.length; i++) {
        if (JOB_CONFIG_PROPS[i].key === key) {
          return true
        }
      }
      return false
    },
    handleClickChangeConfig() {
      this.$refs['updatePropDialog'].show()
    },
    refreshConfigInfo() {
      getConfigInfo(this.$route.params.id).then(res => {
        this.configInfo = res.data
        if (this.instanceInfo.metadata.isContentServer) {
          this.configInfo = res.data
          this.configProps = JOB_CONFIG_PROPS
          this.configProps.forEach((item)=>{
            if (this.configInfo[item.key]) {
              item.value = Number(this.configInfo[item.key])
            }
          })
        }
      })
    }
  },
  computed: {
    filteredConfigInfo() {
      if (!this.filter) {
        return this.configInfo
      }
      const filtered = Object.keys(this.configInfo)
            .filter(key => {
              if (key.toString().includes(this.filter)) {
                return true
              }
              let value = this.configInfo[key]
              if (value && value.toString().includes(this.filter)) {
                return true
              }
              return false
            })
            .reduce((obj, key) => {
              return {
                ...obj,
                [key]: this.configInfo[key]
              }
            },{})
      return filtered
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "~@/styles/variables.scss";

  .app-container {
    margin-top: 90px;
  }
  .details-header {
    text-align: center;
    position: fixed;
    top: 50px;
    background: #fff;
    z-index: 9;
    transition: width 0.28s;
    width: calc(100% - #{$sideBarWidth});
  }

  .hideSidebar .details-header {
      width: calc(100% - 54px);
  }

  .details-header .title {
    color: #363636;
    font-weight: 600;
    display: inline;
    font-size: 25px;
  }
  .details-header .sub-title {
    color: #4a4a4a;
    font-size: 15px;
  }
  .tabs {
    width: 98%;
    margin: auto auto;
  }
  .search-button {
    margin:5px auto 10px auto;
  }
  .panel-group {
    width: 100%;
    display: flex;
    justify-content: space-around;
  }
  .panel-item {
    width: 49%;
  }
  .limit-height {
    max-height: 418px;
  }
 @media (max-width:600px) {
    .panel-group {
      flex-flow: column;
    }
    .panel-item {
      width: 98%;
    }
  }
  .chart {
    height: 300px;
  }
  .node_config {
    width: 100%;
  }

  .table.is-fullwidth {
      width: 100%;
  }
  .table {
      background-color: #fff;
      color: #363636;
  }
  *, :after, :before {
      -webkit-box-sizing: inherit;
      box-sizing: inherit;
  }
  table {
      display: table;
      border-collapse: separate;
      box-sizing: border-box;
      text-indent: initial;
      border-spacing: 2px;
      border-color: grey;
  }
  table > tr {
      vertical-align: middle;
  }
  tr {
      display: table-row;
      vertical-align: inherit;
      border-color: inherit;
  }
  .table td, .table th {
      border: 1px solid #dbdbdb;
      border-width: 0 0 1px;
      padding: .5em .75em;
      vertical-align: top;
  }
  .is-breakable {
      word-break: break-all;
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
