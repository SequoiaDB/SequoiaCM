<template>
  <div>
    <!-- 新增/编辑 用户对话框 -->
    <el-dialog
      :title="this.user + ' 用户角色分配'"
      :visible.sync="detailDialogVisible"
      width="850px">
        <el-transfer
          filterable
          filter-placeholder="请输入角色名称"
          v-model="hasRoles"
          :titles="['可选角色', '已有角色']"
          :button-texts="['移除', '添加']"
          :data="allRoles"
          @change="handleChange"
          class="transfer">
          <div slot-scope="{option}">
            <el-tooltip :content="option.key" placement="right-start">
              <span>{{option.key}}</span>
            </el-tooltip>
          </div>
        </el-transfer>
        <span slot="footer" class="dialog-footer">
          <el-button id="btn_create_close" @click="close" size="mini">关 闭</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { revokeRoles, grantRoles } from "@/api/user"
export default {
  props: {
    user: { type: String }
  },
  data() {
    return{
      hasRoles: [],
      allRoles: [],
      detailDialogVisible: false,
    }
  },
  methods: {
    async handleChange(value, direction, movedKeys) {
      if (direction === 'right') {
        await grantRoles(this.user, movedKeys).then(res=>{
          this.$message.success(`角色分配成功`)
        }).catch(error => {
          // 如果请求失败，穿梭框需要做数据回滚
          movedKeys.forEach(ele => {
            this.hasRoles.splice(this.hasRoles.indexOf(ele), 1)
          })
        })
      } else {
        await revokeRoles(this.user, movedKeys).then(res=>{
          this.$message.success(`角色移除成功`)
        }).catch(error => {
          // 如果请求失败，穿梭框需要做数据回滚
          movedKeys.forEach(ele => {
            this.hasRoles.push(ele)
          })
        })
      }
      this.$emit('onRoleChanged')
    },
    show(hasRoles, allRoles) {
      this.hasRoles = hasRoles
      this.allRoles = allRoles
      this.detailDialogVisible = true
    },
    close() {
      this.detailDialogVisible = false
    }
  }
}
</script>

<style scoped>
.transfer {
  display: flex;
  align-items: center;
  justify-content: center;
}
::v-deep .el-transfer-panel {
  width: 250px;
}
::v-deep .el-transfer-panel__list.is_filterable {
  width: 245px;
}
</style>