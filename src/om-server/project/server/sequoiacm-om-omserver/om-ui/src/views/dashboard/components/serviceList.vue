<template>
  <el-collapse >
    <el-collapse-item v-for="(service, name) in services" :key="name">
      <template slot="title">
        <div class="title">
          <div class="left">
            <i v-if="service.totalCount == service.upCount" class="el-icon-success success"></i>
            <i v-if="service.upCount > 0 && service.upCount < service.totalCount" class="el-icon-warning warning"></i>
            <i v-if="service.upCount == 0" class="el-icon-error error"></i>
            <span class="name">{{name}}</span>
            <i v-if="service.isContentServer" class="el-icon-edit" @click.stop="handleClickChangeConfig(service, name)"></i>
          </div>
          <div class="status">
            <el-tooltip content="健康节点" placement="top-start" >
              <el-tag size="mini" effect="dark" type="success" class="tag">{{service.upCount}}</el-tag>
            </el-tooltip>
            <el-tooltip v-if="service.stoppedCount > 0" content="已停止节点" placement="top-start">
              <el-tag size="mini" effect="dark" type="info" class="tag">{{service.stoppedCount}}</el-tag>
            </el-tooltip>
            <el-tooltip v-if="service.downCount > 0" content="异常节点" placement="top-start">
              <el-tag size="mini" effect="dark" type="danger" class="tag">{{service.downCount}}</el-tag>
            </el-tooltip>
          </div>
        </div>
      </template>
      <el-row v-for="(instance, index) in service.instances" :key="instance.instance_id" class="instance">
        <el-col :span="1" class="order">{{index + 1}}</el-col>
        <el-col :span="4">{{'http://'+ instance.host_name + ':' + instance.port}}</el-col>
        <el-col :span="4">{{instance.region}}/{{instance.zone}}</el-col>
        <el-col :span="2">
          <el-tag size="mini" effect="dark" :type="tagType[instance.status]" class="tag">{{instance.status}}</el-tag>
        </el-col>
        <el-col :span="4">
          <el-button v-if="instance.status == INSTANCE_STATUS.UP"  
            type="primary" 
            size="mini" 
            @click="$emit('onClickViewInstancesBtn', instance)" 
            plain>查看</el-button>
          <el-button v-if="instance.status == INSTANCE_STATUS.DOWN || instance.status == INSTANCE_STATUS.STOPPED" 
            type="info" 
            size="mini" 
            plain 
            @click="$emit('onClickIgnoreInstancesBtn', instance)">忽略</el-button>
        </el-col>
      </el-row>
    </el-collapse-item>
    <update-prop-dialog ref="updatePropDialog" :type="updatePropType" :name="selectService" :configProps="configProps"></update-prop-dialog>
  </el-collapse>
</template>

<script>
import { INSTANCE_STATUS,JOB_CONFIG_PROPS } from '@/utils/common-define'
import { getConfigInfo } from '@/api/monitor'
import UpdatePropDialog from './UpdatePropDialog.vue'
export default {
  components: {
    UpdatePropDialog
  },
  props:{
    services: Object
  },
  data() {
    return {
      tagType: {
        UP: 'success',
        DOWN: 'danger',
        STOPPED: 'info'
      },
      updatePropType: 'service',
      selectService: '',
      configProps: []
    }
  },
  methods: {
    handleClickChangeConfig(service, name) {
      // 查找站点下第一个节点的配置, 不存在则补全默认值
      if (service.instances.length > 0) {
        getConfigInfo(service.instances[0].instance_id).then(res => {
          let configInfo = res.data
          this.configProps = JSON.parse(JSON.stringify(JOB_CONFIG_PROPS))
          this.configProps.forEach((item)=>{
            if (configInfo[item.key]) {
              item.value = Number(configInfo[item.key])
            }
          })
        })
      }
      this.selectService = name
      this.$refs['updatePropDialog'].show()
    }
  },
  computed: {
    INSTANCE_STATUS(){
      return INSTANCE_STATUS
    }
  },
}
</script>

<style scoped>

.success {
  font-size: 20px;
  color: #67C23A;
}
.warning {
  font-size: 20px;
  color: #E6A23C;
}
.error {
  color: #F56C6C;
  font-size: 20px;
  
}
.title {
  display: flex;
  width: 100%;
}
.title .left {
  flex: 1;
}
@media (max-width:980px)  {
  .title .left {
    flex: 2;
  }
}
.title .name {
  margin-left: 3px;
  margin-right: 10px;
  font-size: 18px;
  font-weight: 700;
}
.title .status {
  flex: 4;
}

.tag {
  margin-left: 3px;
}
.status {
  margin-left: 5px;
  font-size: 12px;
  color: #606266;
}
.instance {
  font-size: 14px;
  color: #606266;
  margin-top: 3px;
  margin-left: 10px;
}
.instance .order {
  font-weight: 700;
}
</style>
