<template>
  <div>
    <!-- 用户详情对话框 -->
    <el-dialog
      title="用户详情"
      :visible.sync="detailDialogVisible"
      width="750px">
      <el-row>
        <el-col :span="3"><span class="key">用户 ID</span></el-col>
        <el-col :span="21"><span class="value">{{user.user_id}}</span></el-col>
      </el-row>
      <el-row>
        <el-col :span="3"><span class="key">用户名</span></el-col>
        <el-col :span="21"><span class="value">{{user.user_name}}</span></el-col>
      </el-row>
      <el-row>
        <el-col :span="3"><span class="key">角色列表</span></el-col>
        <el-col :span="21">
          <el-table
            :data="user.roles"
            :show-header="false"
            class="customer-no-border-table"
            max-height="500px"
            @expand-change="listPrivilege"
            row-key="role_id" >
            <el-table-column type="expand">
              <template v-slot="{row}">
                <el-table
                  :data="row.children"
                  empty-text="该角色暂无权限"
                  max-height="300px"
                  v-loading="row.loading" >
                  <el-table-column prop="resource_type" label="资源类型" width="180" />
                  <el-table-column prop="resource_name" label="资源名称" width="180" />
                  <el-table-column
                      label="权限列表">
                      <template slot-scope="scope">
                          <el-tag
                              size="mini"
                              v-for="item of scope.row.privileges"
                              :key="item"
                              style="margin-right:0.2rem">
                              <el-tooltip content="系统内置权限类型" :disabled="item!=='LOW_LEVEL_READ'" placement="top-start" >
                                <template>
                                  <span> {{item}} </span>
                                </template>
                              </el-tooltip>
                          </el-tag>
                      </template>
                  </el-table-column>
                </el-table>
              </template>
            </el-table-column>
            <el-table-column prop="role_name" label="角色名称"/>
          </el-table>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import {listPrivilegesByRole} from "@/api/role"
export default {
  props: {
    user: {
      type: Object
    }
  },
  setup() {

  },
  data() {
    return{
      detailDialogVisible: false,
    }
  },
  methods: {
    listPrivilege(row, expanded) {
      // 判断让当前行展开
      if(JSON.stringify(expanded).includes(JSON.stringify(row)) === true) {
        this.$set(row, 'loading', true)
        this.user.roles.forEach(item => {
          // 找到当前展开的行，把获取到的数据赋值进去
          if(item.role_id === row.role_id) {
            item.children = []
            listPrivilegesByRole(row.role_name).then(res => {
              item.children = res.data
            })
          }
          setTimeout(() => {
            this.$set(row, 'loading', false)
          }, 500)
        })
      } else {
        delete row.children
        this.$set(row, 'loading', false)
        delete row.loading
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
<style scoped>
  .key {
    font-size: 14px;
    font-weight: 600;
    color: #888;
  }
  .value {
    font-size: 14px;
    overflow: hidden;
    color: #606266;
  }
  .el-row {
    margin-top: 20px !important;
  }
  .customer-no-border-table {
    width: 100%;
    margin-bottom: 50px;
  }
  .customer-no-border-table td{
    border: none;
  }
  </style>
