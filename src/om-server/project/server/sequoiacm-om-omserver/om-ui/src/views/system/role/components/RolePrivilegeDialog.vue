<template>
  <div>
    <!-- 角色赋权对话框 -->
    <el-dialog
      title="角色赋权"
      :visible.sync="rolePrivilegeDialogVisible"
      width="750px">
        <el-select
            id="select_resource_type"
            size="small"
            placeholder="请选择资源类型"
            filterable
            v-model="currentResourceType"
            style="width:150px"
            @change="onResourceTypeChange">
            <el-option
              v-for="item in resourceType"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
        </el-select>
        <el-select
          id="select_resource"
          size="small"
          placeholder="请选择资源"
          filterable
          v-model="currentResource"
          v-if="currentResourceType !== '' && currentResourceType !== 'workspace_all'"
          style="width:350px; margin-left:10px">
          <el-option
            v-for="item in resourceList"
            :key="item.name"
            :label="item.name"
            :value="item.name">
          </el-option>
        </el-select>
        <el-select
          id="select_privilege_type"
          size="small"
          placeholder="权限类型"
          filterable
          v-model="privilegeType"
          style="width:100px; margin-left:10px">
          <el-option
            v-for="item in privilegeTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value">
          </el-option>
        </el-select>
        <el-button id="btn_create_role" type="primary" icon="el-icon-plus" size="mini" @click="handleGrantPrivilege" style="margin-left:10px">添 加</el-button>
        <el-table
            size="mini"
            :data="privilegeList"
            style="width: 100%; margin-top: 20px;"
            max-height="250"
            empty-text="该角色暂无权限" >
            <el-table-column
                prop="resource_type"
                label="资源类型"
                width="80">
            </el-table-column>
            <el-table-column
                prop="resource_name"
                label="资源名称"
                width="210">
            </el-table-column>
            <el-table-column
              show-overflow-tooltip
              label="权限列表">
              <template slot-scope="scope">
                <el-tag
                    size="mini"
                    v-for="item of scope.row.privileges"
                    :key="item"
                    :closable="item!=='LOW_LEVEL_READ'"
                    @close="handleRevokePrivilege(scope.row, item)"
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
        <span slot="footer" class="dialog-footer">
          <el-button id="btn_create_close" @click="close" size="mini">关 闭</el-button>
        </span>
    </el-dialog>
  </div>
</template>
<script>
import { grantPrivilege, revokePrivilege, listPrivilegesByRole } from "@/api/role"
import { queryBucketList } from '@/api/bucket'
import { queryWorkspaceList } from '@/api/workspace'
export default {
  props: {
    role: {
      type: Object
    },
    privilegeList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return{
      rolePrivilegeDialogVisible: false,
      currentResourceType: '',
      resourceType: [
        { label: '所有工作区', value: 'workspace_all'},
        { label: '工作区', value: 'workspace'},
        { label: '桶', value: 'bucket'}
      ],
      currentResource: '',
      resourceList: [],
      privilegeType: '',
      privilegeTypes: [
        { label: '读', value: 'READ' },
        { label: '写', value: 'CREATE' },
        { label: '更新', value: 'UPDATE' },
        { label: '删除', value: 'DELETE' },
        { label: '全部', value: 'ALL' },
      ]
    }
  },
  methods: {
    onResourceTypeChange() {
      this.currentResource = ''
      this.resourceList = []
      this.privilegeType = ''
      if (this.currentResourceType === 'workspace') {
        queryWorkspaceList(1, -1, null, false).then(res=>{
          this.resourceList = res.data
        })
      } else if (this.currentResourceType === 'bucket') {
        queryBucketList(1, -1, null, null).then(res=>{
          this.resourceList = res.data
        })
      }
    },
    handleGrantPrivilege() {
      let resource = this.currentResource
      // 创建桶资源需要拼接工作区 e.g.  ws_default:bucket
      if (this.currentResourceType === 'bucket') {
        let currentBucket = this.resourceList.find(item=>{
          return item.name === this.currentResource
        })
        resource = currentBucket.workspace + ":" + resource
      }
      grantPrivilege(this.role.role_name, this.currentResourceType, resource, this.privilegeType).then(res => {
        this.$message.success('添加权限成功')
        this.currentResourceType = ''
        this.onResourceTypeChange()
        listPrivilegesByRole(this.role.role_name).then(res => {
          this.privilegeList = res.data
        })
      })
    },
    handleRevokePrivilege(row, privilege) {
      revokePrivilege(this.role.role_name, row.resource_type, row.resource_name, privilege).then(res => {
        this.$message.success('移除权限成功')
        listPrivilegesByRole(this.role.role_name).then(res => {
          this.privilegeList = res.data
        })
      })
    },
    show() {
      this.rolePrivilegeDialogVisible = true
    },
    close() {
      this.rolePrivilegeDialogVisible = false
    }
  }
}
</script>
