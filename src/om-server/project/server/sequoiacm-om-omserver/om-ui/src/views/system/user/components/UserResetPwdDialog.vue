<template>
  <div>
    <!-- 重置用户密码对话框 -->
    <el-dialog
      title="重置用户密码"
      :visible.sync="resetPwdDialogVisible"
      width="650px">
      <template> 
        <el-form ref="form" :model="user" :rules="rules" label-width="80px"> 
          <el-form-item label="用户名" prop="username"> 
            <el-input v-model="username" disabled/>
          </el-form-item>
          <el-form-item label="旧密码" prop="oldPassword" v-if="isAdminUser()"> 
            <el-input v-model="user.oldPassword" placeholder="重置管理员用户密码，需要输入旧密码" type="password" show-password/>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword"> 
            <el-input v-model="user.newPassword" placeholder="请输入新密码" type="password" show-password/>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="user.confirmPassword" placeholder="请确认新密码" type="password" show-password/>
          </el-form-item>
        </el-form>
      </template>
        <span slot="footer" class="dialog-footer" style="border:1px soild red">
          <el-button id="btn_reset_pwd" @click="submit" type="primary" size="mini">重置密码</el-button>
          <el-button id="btn_reset_cancel" @click="close" size="mini">关 闭</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { updatePwd } from "@/api/user"
export default {
  props: { 
    username: { type: String },
    roles: { type: Array }
  },
  data() {
    const equalToPassword = (rule, value, callback) => { 
      if (this.user.newPassword !== value) { 
        callback(new Error("两次输入的密码不一致")); 
      } 
      else { 
        callback(); 
      } 
    }
    return{
      resetPwdDialogVisible: false,
      user: { 
        oldPassword: undefined,
        newPassword: undefined, 
        confirmPassword: undefined 
      }, 
      // 表单校验 
      rules: { 
        newPassword: [ 
          { required: true, message: "密码不能为空", trigger: "blur" },
        ], 
        confirmPassword: [ 
          { required: true, message: "确认密码不能为空", trigger: "blur" },
          { required: true, validator: equalToPassword, trigger: "blur" } 
        ] 
      }
    }
  },
  methods: {
    submit() { 
      this.$refs["form"].validate(
        valid => { if (valid) {
          updatePwd(this.username, this.user.oldPassword, this.user.newPassword).then(response => {
            this.$message.success("密码重置成功"); 
            this.close()
          }); 
        } }
      ); 
    }, 
    // 判断当前用户是否是管理员用户
    isAdminUser() {
      return this.roles.includes('ROLE_AUTH_ADMIN')
    },
    show() {
      this.resetPwdDialogVisible = true
    },
    close() {
      this.clear()
      this.resetPwdDialogVisible = false
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