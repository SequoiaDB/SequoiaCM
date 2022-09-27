<template>
  <div class="app-container">
    <!-- 顶部卡片 -->
    <card-panel :statisticsData="statisticsData"/>

    <el-alert
      class="alert-msg"
      v-if="services['service-center'] && services['service-center'].upCount == 0"
      title="当前没有可用的 service-center 节点，节点状态可能无法正常更新。"
      type="warning"
      show-icon>
    </el-alert>

    <!-- 显示方式 -->
    <div class="show-type">
      <div class="tip">显示方式: </div>
      <el-radio-group v-model="showType" size="mini">
        <el-radio-button label="default">默认</el-radio-button>
        <el-radio-button label="byRegionZone">按机房</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 默认显示 -->
    <div v-if="showType=='default'">
      <service-list :services="services" @onClickViewInstancesBtn="toInstancePage" @onClickIgnoreInstancesBtn="handleIgnoreBtnClick"></service-list>
    </div>

    <!-- 按物理机房显示 -->
    <div v-if="showType=='byRegionZone'">
      <div v-for="(key, index) in Object.keys(sameRegionZoneServices).sort()" :key="index">
        <div class="location">
          {{key}}
        </div>
        <service-list :services="sameRegionZoneServices[key]" @onClickViewInstancesBtn="toInstancePage" @onClickIgnoreInstancesBtn="handleIgnoreBtnClick"></service-list>
      </div>
    </div>

  </div>
</template>

<script>
import CardPanel from './components/cardPanel'
import ServiceList from './components/serviceList'

import { queryInstanceList, getVersion, deleteInstance } from '@/api/monitor'
import { INSTANCE_STATUS } from '@/utils/common-define'

export default {
  components: {
    CardPanel,
    ServiceList
  },
  data() {
    return {
      instances: [],
      showType: 'default',
      statisticsData: {
        version: '',
        services: 0,
        instances: 0,
        errorInstances: 0
      },
      services: {},
      sameRegionZoneServices: {}
    }
  },
  methods: {
    init(){
      queryInstanceList().then( res => {
        this.instances = res.data
        this.services = this.groupByService(res.data)
        this.statisticsData.instances = res.data.length
        this.statisticsData.services = Object.keys(this.services).length
        this.statisticsData.errorInstances = res.data.filter(item => item.status == INSTANCE_STATUS.DOWN).length

        this.sameRegionZoneServices = {}
        this.instances.forEach(item => {
          let key = item.region + '/' + item.zone
          if (!this.sameRegionZoneServices[key]) {
            this.sameRegionZoneServices[key] = this.groupByService(res.data.filter(i => i.region == item.region && i.zone == item.zone))
          }
        })
      })
      getVersion().then(res => {
        this.statisticsData.version = 'v' + res.data.version
      })
    },
    groupByService(instances) {
      let result = {}
      instances.forEach(item => {
        let serviceName = item['service_name']
        if (!result[serviceName]) {
          result[serviceName] = {
            isContentServer: item.metadata.isContentServer,
            upCount: 0,
            downCount: 0,
            stoppedCount: 0,
            totalCount: 0,
            instances: []
          }
        }
        result[serviceName].instances.push(item)
        result[serviceName].totalCount ++
        if (item.status == INSTANCE_STATUS.DOWN) {
          result[serviceName].downCount ++
        } else if (item.status == INSTANCE_STATUS.UP) {
          result[serviceName].upCount ++
        }else if (item.status == INSTANCE_STATUS.STOPPED) {
          result[serviceName].stoppedCount ++
        }
      })
      return result
    },
    // 跳转到节点状态页面
    toInstancePage(instance) {
      this.$router.push("/dashboard/instance/" + encodeURIComponent(instance.instance_id))
    },
    // 点击忽略按钮
    handleIgnoreBtnClick(row) {
      this.$confirm(`您确认忽略此节点吗？忽略后，该节点暂时不显示在服务列表中，待节点恢复后会自动加入。`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_instance_confirmIgnore',
        type: 'warning'
      }).then(() => {
        deleteInstance(row.instance_id).then(() => {
          this.$message.success('操作成功')
          this.init()
        })
      }).catch(() => {
      });
    }
  },
  activated() {
    this.init()
  },
  computed: {
    INSTANCE_STATUS(){
      return INSTANCE_STATUS
    }
  }
}
</script>

<style scoped>
.alert-msg {
  margin-bottom: 5px;
  font-size: 25px;
  font-weight: 600;
}
.location {
  font-weight: 700;
  margin-bottom: 5px;
  margin-top: 15px;
  color: rgba(0, 0, 0, 0.45);
}
.show-type .tip {
  font-size: 13px;
  line-height: 28px;
  margin-right: 5px;
  font-weight: 700;
}
.show-type {
  display: flex;
  margin-bottom: 5px;
  justify-content: flex-end;
}

</style>
