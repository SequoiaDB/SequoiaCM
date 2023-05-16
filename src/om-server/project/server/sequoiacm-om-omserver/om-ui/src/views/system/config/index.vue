<template>
  <div class="app-container">
    <div class="title">标签库</div>
    <div style="margin-left: 5px;">
      <el-row style="margin-top: 10px;">
        <el-col :span="2" style="margin-top: 8px; margin-right: 5px;">
          <el-tooltip placement="bottom-start">
            <div slot="content" class="tooltip">
              当创建工作区未指定标签库集合的 Domain 时，使用默认 Domain
            </div>
            <span>默认 Domain：</span>
          </el-tooltip>
        </el-col>
        <el-col :span="4">
          <el-input v-model="defaultDomain" readonly>
            <i
              class="el-icon-edit el-input__icon"
              slot="suffix"
              @click="handleConfigBtnClick('标签库', '默认 Domain', 'scm.tagLib.defaultDomain', defaultDomain)">
            </i>
          </el-input>
        </el-col>
      </el-row>
    </div>
    <!-- 修改配置弹框 -->
    <modify-configuration-dialog ref="modifyConfigurationDialog" @onConfigChange="init"></modify-configuration-dialog>
  </div>
</template>

<script>
import {getGlobalConfig} from '@/api/system'
import ModifyConfigurationDialog from './components/ModifyConfigurationDialog.vue'
export default {
  components: {
    ModifyConfigurationDialog
  },

  data(){
    return {
      defaultDomain: ''
    }
  },

  methods: {
    // 修改配置
    handleConfigBtnClick(configTitle, configItem, configName, configValue) {
      this.$refs['modifyConfigurationDialog'].show(configTitle, configItem, configName, configValue)
    },
    // 初始化
    init() {
      // 获取全局默认的标签库
      let configName = 'scm.tagLib.defaultDomain'
      getGlobalConfig(configName).then(res => {
        this.defaultDomain = res.data[configName]
      })
    }
  },
  
  activated() {
    this.init()
  }
}
</script>

<style scoped>
.title {
  font-size: 18px;
  height: 22px;
  line-height: 22px;
  margin-bottom: 15px;
  font-weight: 600;
  text-indent: 5px;
  border-left: 5px solid #409EFF;
}
.sub-title {
  font-size: 16px;
  height: 22px;
  line-height: 22px;
  margin-bottom: 5px;
  text-indent: 5px;
}
</style>