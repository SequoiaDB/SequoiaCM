<template>
  <div>
    <!-- 新增/编辑 用户对话框 -->
    <el-dialog
      :title="this.user + ' 用户角色分配'"
      :visible.sync="detailDialogVisible"
      width="750px">
        <el-transfer
          filterable
          filter-placeholder="请输入角色名称"
          v-model="hasRoles"
          :titles="['可选角色', '已有角色']"
          :button-texts="['移除', '添加']"
          :data="allRoles"
          @change="handleChange"
          style="margin-left: 50px;">
        </el-transfer>
        <span slot="footer" class="dialog-footer" style="border:1px soild red">
          <el-button id="btn_create_close" @click="close" size="mini">关 闭</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { revokeRoles, grantRoles } from "@/api/user"
export default {
  props: {
    user: { type: String },
    hasRoles: {
      type: Array,
      default: () => []
    },
    allRoles: {
      type: Array,
      default: () => []
    }
  }, 
  data() {
    return{
      detailDialogVisible: false,
    }
  },
  methods: {
    handleChange(value, direction, movedKeys) {
      if (direction === 'right') {
        grantRoles(this.user, movedKeys).then(res=>{
          this.$message.success(`角色分配成功`)
        })
      } else {
        revokeRoles(this.user, movedKeys).then(res=>{
          this.$message.success(`角色移除成功`)
        })
      }
    },
    show() {
      this.detailDialogVisible = true
    },
    close() {
      this.detailDialogVisible = false
    }
  }
}
</script>