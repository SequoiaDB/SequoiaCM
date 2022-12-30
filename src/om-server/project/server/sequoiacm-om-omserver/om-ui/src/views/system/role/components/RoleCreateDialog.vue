<template>
  <div>
    <!-- 新增/编辑 角色对话框 -->
    <el-dialog
      title="新增角色"
      :visible.sync="detailDialogVisible"
      width="600px">
        <el-form ref="form" :rules="rules" :model="role" size="small" label-width="110px">
            <el-form-item label="角色名" prop="name">
              <el-input id="input_role_name" v-model="role.name" placeholder="请输入角色名"></el-input>
            </el-form-item>
            <el-form-item label="角色描述" prop="description">
              <el-input id="input_role_description" v-model="role.description" placeholder="请输入角色描述信息"></el-input>
            </el-form-item>
          </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button id="btn_create_close" @click="close" size="mini">关 闭</el-button>
          <el-button id="btn_create_role" @click="submit" type="primary" size="mini">保 存</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { createRole } from "@/api/role"
export default {
  data() {
    return{
      detailDialogVisible: false,
      role: {
        name: undefined,
        description: undefined
      },
      rules: {
        name: [
          { required: true, message: "角色名不能为空", trigger: "blur" }
        ]
      },
    }
  },
  methods: {
    submit() {
      this.$refs["form"].validate(
        valid => { if (valid) {
          createRole(this.role.name, this.role.description).then(response => {
            this.$message.success("角色 " + this.role.name + " 创建成功")
            this.close()
            this.$emit('onRoleCreated')
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
