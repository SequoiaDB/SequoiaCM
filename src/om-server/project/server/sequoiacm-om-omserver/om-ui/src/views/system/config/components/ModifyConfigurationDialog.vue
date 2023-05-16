<template>
  <div>
    <!-- 修改系统全局配置对话框 -->
    <el-dialog
      :title="form.configTitle"
      :visible.sync="detailDialogVisible"
      width="500px">
        <el-form ref="form" :model="form" size="small" label-width="120px">
            <el-form-item :label="form.configItem">
              <el-input id="input_file_value" v-model="form.configValue" placeholder="请输入 Domain"></el-input>
            </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button id="btn_config_close" @click="close" size="mini">关 闭</el-button>
          <el-button id="btn_config_change" @click="submit" type="primary" size="mini">保 存</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import {setGlobalConfig} from '@/api/system'
export default {
  data() {
    return{
      detailDialogVisible: false,
      form: {
        configTitle: '',
        configItem: '',
        configName: '',
        configValue: ''
      }
    }
  },
  methods: {
    submit() {
      if (this.form.configValue === '') {
        this.$message.warning("请填写" + this.form.configItem)
        return 
      }
      setGlobalConfig(this.form.configName, this.form.configValue).then(res => {
        this.$message.success("修改 " + this.form.configTitle + "-" + this.form.configItem + " 成功")
        this.close()
        this.$emit('onConfigChange')
      })
    },
    show(configTitle, configItem, configName, configValue) {
      this.form.configTitle = configTitle
      this.form.configItem = configItem
      this.form.configName = configName
      this.form.configValue = configValue
      this.detailDialogVisible = true
    },
    close() {
      this.clear()
      this.detailDialogVisible = false
    },
    clear() {
      setTimeout(()=>{
        // 重置表单
        this.clearForm()
      }, 500)
    },
    clearForm() {
      if (this.$refs['form']) {
        this.$refs['form'].resetFields()
      }
    }
  }
}
</script>
