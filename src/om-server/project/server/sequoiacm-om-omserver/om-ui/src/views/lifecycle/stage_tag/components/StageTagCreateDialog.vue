<template>
  <div>
    <!-- 新增阶段标签对话框 -->
    <el-dialog
      title="新增阶段标签"
      :visible.sync="detailDialogVisible"
      width="600px">
        <el-form ref="form" :rules="rules" :model="stageTag" size="small" label-width="110px">
            <el-form-item label="标签名称" prop="name">
              <el-input id="input_stage_tag_name" v-model="stageTag.name" maxlength="30" placeholder="请输入标签名"></el-input>
            </el-form-item> 
            <el-form-item label="标签描述" prop="description">
              <el-input id="input_stage_tag_description" v-model="stageTag.description" placeholder="请输入标签述信息"></el-input>
            </el-form-item>
          </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button id="btn_create_stage_tag" @click="close" size="mini">关 闭</el-button>
          <el-button id="btn_create_stage_tag" @click="submit" type="primary" size="mini">保 存</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { createStageTag } from "@/api/lifecycle"
export default {
  data() {
    return{
      detailDialogVisible: false,
      stageTag: {
        name: undefined,
        description: undefined
      },
      rules: {
        name: [ 
          { required: true, message: "阶段标签名不能为空", trigger: "blur" }
        ]
      },
    }
  },
  methods: {
    submit() { 
      this.$refs["form"].validate(
        valid => { if (valid) {
          createStageTag(this.stageTag.name, this.stageTag.description).then(response => {
            this.$message.success("阶段标签 " + this.stageTag.name + " 创建成功")
            this.close()
            this.$emit('onStageTagCreated')
          }); 
        } }
      ); 
    },
    show() {
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