<template>
  <div>
    <!-- 创建桶对话框 -->
    <el-dialog
      class="edit-dialog"
      title="创建桶"
      :visible.sync="visible"
      width="600px">
        <el-form ref="form" :rules="rules" :model="form" size="small" label-width="110px" >
            <el-form-item label="桶名" prop="bucketNames">
                <el-tag
                :key="name"
                v-for="name in form.bucketNames"
                closable
                :disable-transitions="false"
                @close="removeBucket(name)">
                  <span>{{name}}</span>
                </el-tag>
                <el-row type="flex" justify="space-between">
                  <el-col :span="17">
                    <el-input
                      class="input-new-bucket"
                      maxlength="50"
                      v-model="bucketNameInputValue"
                      placeholder="请输入桶名"
                      size="small">
                    </el-input>
                  </el-col>
                  <el-col :span="6">
                    <el-button size="small" @click="addBucket">+ 继续添加</el-button>
                  </el-col>
                </el-row>
            </el-form-item>

            <el-form-item  prop="workspace">
                <template slot="label">
                  region
                  <el-tooltip class="item" effect="dark" placement="bottom">
                    <div slot="content">
                      仅可选择拥有 CREATE 权限的工作区作为 region
                    </div>
                    <i class="el-icon-question"></i>
                  </el-tooltip>
                </template>
                <el-select
                    v-model="form.workspace"
                    placeholder="请选择region"
                    filterable
                    size="small"
                    style="width:100%">
                    <el-option v-for="item in workspaces"
                      :key="item.name"
                      :label="item.name"
                      :value="item.name"/>
                </el-select>
            </el-form-item>
            <el-form-item label="版本控制" prop="versionStatus">
                <el-radio-group v-model="form.versionStatus">
                    <el-radio-button label="Disabled" ></el-radio-button>
                    <el-radio-button label="Enabled"></el-radio-button>
                </el-radio-group>
            </el-form-item>
            <el-form-item label="开启限额">
              <el-switch v-model="form.quotaEnable"></el-switch>
            </el-form-item>

            <el-row v-if="form.quotaEnable">
              <el-col :span="20">
                <el-form-item label="最大存储容量" prop="maxSize">
                  <el-input v-model.number="form.maxSize" placeholder="请输入桶最大存储容量限制，-1 表示无限制" maxlength="9" v-number-only></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-select v-model="sizeUnit" size="small">
                  <el-option label="M" value="M"></el-option>
                  <el-option label="G" value="G"></el-option>
                </el-select>
              </el-col>
            </el-row>
            <template v-if="form.quotaEnable">
              <el-form-item label="最大对象个数" prop="maxObjects">
                <el-input v-model.number="form.maxObjects" placeholder="请输入桶最大对象数量限制，-1 表示无限制" maxlength="9" v-number-only></el-input>
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
import {queryCreatePrivilegeWsList} from '@/api/workspace'
import {createBucket} from '@/api/bucket'
import numberOnly from '@/directives/numberOnly'

export default {
   directives: {
    numberOnly
  },
  data() {
    let bucketNamesValidation = (rule, value, callback) => {
      if (this.form.bucketNames.length === 0 && !this.bucketNameInputValue) {
        callback(new Error('请填写桶名称'));
      } else {
        callback()
      }
    }
    return{
      saveBtnDisabled: false,
      sizeUnit: 'G',
      visible: false,
      bucketNameInputValue: '',
      rules: {
       bucketNames: {required: true, trigger: 'blur', validator: bucketNamesValidation},
       workspace: {required: true, trigger: 'blur', message: '请选择region'},
       maxSize: {required: true, trigger: 'none', message: '请输入最大容量限制'},
       maxObjects: {required: true, trigger: 'none', message: '请输入最大对象数量限制'}
      },
      workspaces: [],
      form: {
        workspace:'',
        bucketNames: [],
        versionStatus: 'Disabled',
        maxSize: '',
        maxObjects: '',
        quotaEnable: false
      },
    }
  },
  methods: {
    show() {
      this.visible = true
      queryCreatePrivilegeWsList().then(res => {
        this.workspaces = res.data
      })
    },

    close() {
      this.visible = false
      setTimeout(()=>{
        this.clearForm()
      }, 500)
    },

     // 移除桶
    removeBucket(tag) {
      this.form.bucketNames.splice(this.form.bucketNames.indexOf(tag), 1);
    },

    // 添加桶
    addBucket() {
      let bucketNameInputValue = this.bucketNameInputValue
      if (bucketNameInputValue) {
        if (this.form.bucketNames.some(e => e === bucketNameInputValue)){
          this.$message.error("已存在桶：" + bucketNameInputValue)
          return
        }
        this.form.bucketNames.push(bucketNameInputValue)
      } else {
        this.$message.warning("请输入桶名")
      }
      this.bucketNameInputValue = ''
    },

    // 提交表单
    submitForm() {
      if (this.saveBtnDisabled) {
        return
      }
      this.saveBtnDisabled = true
      this.$refs['form'].validate(valid => {
        if (valid) {
          let bucketNames = [...this.form.bucketNames]
          if (this.bucketNameInputValue) {
            if (bucketNames.some(item => item === this.bucketNameInputValue)) {
              this.$message.warning("桶名重复：" + this.bucketNameInputValue)
            } else {
              bucketNames.push(this.bucketNameInputValue)
            }
          }
          let formData = {
            bucket_names: bucketNames,
            workspace: this.form.workspace,
            version_status: this.form.versionStatus,
            enable_quota: this.form.quotaEnable,
            max_objects: this.form.maxObjects,
            max_size: this.form.maxSize + this.sizeUnit
          }
          createBucket(formData).then(res => {
            let resList = res.data
            this.$util.showBatchOpMessage("创建桶", resList)
            this.$emit('onBucketCreated')
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
        workspace:'',
        bucketNames: [],
        versionStatus: 'Disabled',
        maxSize: '',
        maxObjects: '',
        quotaEnable: false
      }
      this.bucketNameInputValue = ''
    },
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
.button-new-bucket {
  margin-left: 5px;
}
</style>
