<template>
  <div>
    <!-- 更新文件内容对话框 -->
    <el-dialog
      class="edit-dialog"
      title="更新文件"
      :visible.sync="editDialogVisible"
      width="600px">
        <el-form ref="form" :rules="rules" :model="form" size="small" label-width="110px" :disabled="updateForbidden">
          <el-form-item label="存储站点" prop="site">
            <el-select
              id="select_update_site"
              v-model="form.site"
              placeholder="请选择存储站点"
              filterable
              size="small"
              style="width:100%">
                <el-option
                  v-for="item in workspaceDetail.data_locations"
                  :key="item.site_name"
                  :label="item.site_name"
                  :value="item.site_name">
                </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="文件内容" prop="fileContentList">
            <el-upload
              ref="elUpload"
              id="select_update_file_content"
              :auto-upload="false"
              :file-list="form.fileContentList"
              :on-change="handleFileContentChange"
              :on-remove="handleFileRemove"
              :http-request="_updateFileContent"
              action=""
              drag>
                <i class="el-icon-upload"></i>
                <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            </el-upload>
          </el-form-item>
          <el-form-item label="更新配置项" prop="updateContentOption">
            <el-checkbox id="cb_update_need_md5" v-model="form.isNeedMd5Checked" label="计算MD5"></el-checkbox>
          </el-form-item>
        </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button id="btn_update_close" @click="close" size="mini">关 闭</el-button>
        <el-button id="btn_update_file_content" type="primary" @click="submitForm" size="mini" :disabled="updateForbidden">更新文件内容</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {updateFileContent} from '@/api/file'
export default {
  setup() {
  },
  props: {
    workspaceDetail: {
      type: Object,
      default: {}
    },
    fileId: {
      type: String,
      default: ''
    }
  },
  data() {
    let fileContentValidation = (rule, value, callback) => {
      if (this.form.fileContentList.length === 0) {
        callback(new Error('请选择文件内容'));
      } else {
        callback()
      }
    }
    return{
      editDialogVisible: false,
      updateForbidden: false,
      rules: {
        site: [
          { required: true, message: '请选择存储站点', trigger: 'none' }
        ],
        fileContentList: [
          { required: true, validator: fileContentValidation }
        ]
      },
      form: {
        fileContentList: [],
        isNeedMd5Checked: false,
      },
    }
  },
  methods: {
    show() {
      this.editDialogVisible = true
    },
    clear() {
      setTimeout(()=>{
        this.clearForm()
      }, 500)
    },
    close() {
      this.editDialogVisible = false
    },
    handleFileContentChange(file, fileList) {
      // 清除校验
      this.$refs['form'].clearValidate(['fileContentList'])
      // 限制文件内容只能选择一份， 重复添加时覆盖前面的文件
      if (file.status !== 'fail') {
        if (fileList.length > 1) {
          fileList.splice(0, 1)
        }
        this.form.fileContentList = fileList
      }
    },
    handleFileRemove() {
      this.form.fileContentList = []
    },
    _updateFileContent(data) {
      let ws = this.workspaceDetail.name
      let site = this.form.site
      let fileId = this.fileId
      let updateOption = {
        is_need_md5: this.form.isNeedMd5Checked
      }
      this.updateForbidden = true
      this.$emit('update', { status : true, fileId : this.fileId})
      updateFileContent(ws, fileId, site, updateOption, data).then(res=>{
        this.$message.success("文件内容更新成功")
        this.clear()
        this.close()
        this.$emit('refreshTable')
      }).finally(() => {
        this.updateForbidden = false
        this.$emit('update', { status : false, fileId: ''})
        if (this.form.fileContentList.length > 0) {
          this.form.fileContentList[0].status = 'ready'
        }
      })
    },
    // 提交表单（上传文件）
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.$refs.elUpload.submit()
        }
      })
    },
    // 重置表单
    clearForm() {
      if (this.$refs['form']) {
        this.$refs['form'].resetFields()
        this.form.isNeedMd5Checked = false
      }
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
</style>
