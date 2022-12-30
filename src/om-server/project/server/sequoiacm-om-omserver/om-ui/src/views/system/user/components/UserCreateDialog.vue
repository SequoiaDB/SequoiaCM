<template>
  <div>
    <!-- 新增/编辑 用户对话框 -->
    <el-dialog
      title="新增用户"
      :visible.sync="detailDialogVisible"
      width="650px">
        <el-form ref="form" :model="user" :rules="rules" size="small" label-width="80px">
            <el-form-item label="用户名" prop="name">
              <el-input id="input_file_title" v-model="user.name" placeholder="请输入用户名"></el-input>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input id="input_user_password" v-model="user.password" placeholder="请输入密码" type="password" show-password/>
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input id="input_user_confirm_password" v-model="user.confirmPassword" placeholder="请再次输入密码" type="password" show-password/>
            </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button id="btn_create_close" @click="close" size="mini">关 闭</el-button>
          <el-button id="btn_create_user" @click="submit" type="primary" size="mini">保 存</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { createUser } from "@/api/user"
export default {
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.user.password !== value) {
        callback(new Error("两次输入的密码不一致"));
      }
      else {
        callback();
      }
    }
    return{
      detailDialogVisible: false,
      user: {
        password: undefined,
        confirmPassword: undefined
      },
      // 表单校验
      rules: {
        name: [
          { required: true, message: "用户名不能为空", trigger: "blur" },
        ],
        password: [
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
          createUser(this.user.name, this.user.password).then(response => {
            this.$message.success("用户 " + this.user.name + " 创建成功")
            this.close()
            this.$emit('onUserCreated')
          })
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
