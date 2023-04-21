<template>
  <div>
    <!-- 设置限额对话框 -->
    <el-dialog
      class="edit-dialog"
      title="设置限额"
      :visible.sync="visible"
      width="600px">
        <el-form ref="form" :rules="rules" :model="form" size="small" label-width="110px" >
            <el-form-item label="限额状态">
              <el-radio-group v-model="form.enable">
                <el-radio :label="true">开启</el-radio>
                <el-radio :label="false">关闭</el-radio>
              </el-radio-group>
            </el-form-item>
            <template v-if="form.enable">
              <el-form-item label="最大存储容量" prop="maxSize">
                <el-row>
                  <el-col :span="20">
                    <el-input v-model="form.maxSize" placeholder="请输入桶最大存储容量限制，-1 表示无限制" maxlength="9" v-number-only></el-input>
                  </el-col>
                  <el-col :span="4">
                    <el-select v-model="sizeUnit">
                      <el-option label="M" value="M"></el-option>
                      <el-option label="G" value="G"></el-option>
                    </el-select>
                  </el-col>
                </el-row>
              </el-form-item>
              <el-form-item label="最大对象个数" prop="maxObjects">
                <el-input v-model="form.maxObjects" placeholder="请输入桶最大对象数量限制，-1 表示无限制" maxlength="9" v-number-only></el-input>
              </el-form-item>
            </template>
        </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="close" size="mini">关 闭</el-button>
        <el-button type="primary" size="mini" :disabled="saveBtnDisabled" @click="submitForm">保 存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import numberOnly from '@/directives/numberOnly'
import {enableBucketQuota, updateBucketQuota, disableBucketQuota} from '@/api/quota'

export default {
  directives: {
    numberOnly
  },
  data() {
    return{
      saveBtnDisabled: false,
      sizeUnit: 'G',
      visible: false,
      oldQuotaInfo: {},
      rules: {
       maxSize: {required: true, trigger: 'blur', message: '请输入最大容量限制'},
       maxObjects: {required: true, trigger: 'blur', message: '请输入最大对象数量限制'}
      },
      form: {
        enable: false,
        maxSize: '',
        maxObjects: ''
      },
    }
  },
  methods: {
    show(quotaInfo) {
      this.oldQuotaInfo = quotaInfo
      if (quotaInfo.enable) {
        let maxSizeBytes = quotaInfo.max_size
        if (maxSizeBytes >= 0) {
          let res = this.$util.formatBytes(maxSizeBytes)
          this.form.maxSize = res.value
          if (this.form.maxSize != 0) {
            this.sizeUnit = res.unit
          }
        }else {
          this.form.maxSize = -1
        }
        this.form.maxObjects = quotaInfo.max_objects
      }
      this.form.enable = quotaInfo.enable
      this.visible = true

    },

    close() {
      this.visible = false
      setTimeout(()=>{
        this.clearForm()
      }, 500)
    },


    // 提交表单
    submitForm() {
      if (this.saveBtnDisabled) {
        return
      }
      this.saveBtnDisabled = true
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (!this.form.enable) {
            disableBucketQuota(this.oldQuotaInfo.bucket_name).then(res => {
              this.$message.success("禁用限额成功")
              this.$emit("onQuotaChange")
              this.close()
            }).finally(() => {
              this.saveBtnDisabled = false
            })
          } else {
            let maxSize = this.form.maxSize >= 0 ? this.form.maxSize + this.sizeUnit : -1
            if (this.oldQuotaInfo.enable) {
              updateBucketQuota(this.oldQuotaInfo.bucket_name, this.form.maxObjects, maxSize).then(res => {
                this.$message.success("更新限额成功")
                this.$emit("onQuotaChange")
                this.close()
              }).finally(() => {
                this.saveBtnDisabled = false
              })
            } else {
              enableBucketQuota(this.oldQuotaInfo.bucket_name, this.form.maxObjects, maxSize).then(res => {
                this.$message.success("开启限额成功")
                this.$emit("onQuotaChange")
                this.close()
              }).finally(() => {
                this.saveBtnDisabled = false
              })
            }
          }
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
        enable: false,
        maxSize: '',
        maxObjects: ''
      }
    },
  }
}
</script>

<style  scoped>
.edit-dialog >>> .el-dialog__body {
  padding: 20px;
}
</style>
