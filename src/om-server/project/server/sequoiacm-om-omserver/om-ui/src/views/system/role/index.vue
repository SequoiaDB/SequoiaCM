<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="14">
          <el-input
            id="input_role_search_roleName"
            maxlength="50"
            size="small"
            placeholder="角色名称"
            v-model="searchParams.roleName"
            clearable
            @keyup.enter.native="doSearch">
          </el-input>
        </el-col>
        <el-col :span="5" >
          <el-button id="btn_role_doSearch" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%">搜索</el-button>
        </el-col>
        <el-col :span="5" >
          <el-button id="btn_role_resetSearch" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%">重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button id="btn_role_showCreateDialog" type="primary" size="small" icon="el-icon-plus"  @click="handleCreateBtnClick" style="margin-bottom:10px">新增</el-button>
    <el-table
        border
        :data="tableData"
        row-key="role_id"
        v-loading="tableLoading"
        style="width: 100%">
        <el-table-column
          type="index"
          :index="getIndex"
          label="序号"
          width="55">
        </el-table-column>
        <el-table-column
          prop="role_name"
          label="角色名称"
          show-overflow-tooltip
          width="230">
        </el-table-column>
        <el-table-column
          prop="description"
          show-overflow-tooltip
          label="描述">
        </el-table-column>
        <el-table-column
          width="225"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_role_showDetailDialog" size="mini" @click="handleShowBtnClick(scope.row)">查看</el-button>
              <el-button id="btn_role_showEditDialog" size="mini" @click="handleEditBtnClick(scope.row)">角色赋权</el-button>   
              <el-tooltip content="无法删除系统内置角色" :disabled="!isSystemRole(scope.row.role_name)" placement="top">
                <span>
                  <el-button id="btn_role_delete" type="danger" :disabled="isSystemRole(scope.row.role_name)" @click="handleDeleteBtnClick(scope.row)" size="mini">删除</el-button>
                </span>
              </el-tooltip>
            </el-button-group>
          </template>
        </el-table-column>
    </el-table>

    <!-- 分页部分 -->
    <el-pagination
      class="pagination"
      background
      :current-page="pagination.current"
      @current-change="handleCurrentChange"
      layout="total, prev, pager, next, jumper"
      :page-size="pagination.size"
      :total="pagination.total">
    </el-pagination>

    <!-- 新增用户弹框 -->
    <role-create-dialog ref="roleCreateDialog" @onRoleCreated="queryTableData"></role-create-dialog>
    <!-- 用户详情弹框 -->
    <role-detail-dialog ref="roleDetailDialog" :role="currentRole"></role-detail-dialog>
    <!-- 角色赋权弹框 -->
    <role-privilege-dialog ref="rolePrivilegeDialog" :role="currentRole" :privilegeList="privilegesOfCurrentRole"></role-privilege-dialog>
  </div>
</template>
<script>
import RoleCreateDialog from './components/RoleCreateDialog.vue'
import RoleDetailDialog from './components/RoleDetailDialog.vue'
import RolePrivilegeDialog from './components/RolePrivilegeDialog.vue'
import { Loading } from 'element-ui'
import { listRoles, deleteRole, listPrivilegesByRole } from "@/api/role"
import { X_RECORD_COUNT, SYSTEM_ROLES } from '@/utils/common-define'
export default {
  components: {
    RoleCreateDialog,
    RoleDetailDialog,
    RolePrivilegeDialog
  },
  data(){
    return {
      pagination:{
        current: 1, //当前页
        size: 12, //每页大小
        total: 0, //总数据条数
      },
      filter: {},
      searchParams: {
        roleName: undefined
      },
      tableLoading: false,
      tableData: [],
      currentRole: {},
      privilegesOfCurrentRole:[],
    }
  },
  methods:{
    // 初始化
    init(){
      this.queryTableData()
    },
    // 新增角色
    handleCreateBtnClick() {
      this.$refs['roleCreateDialog'].show()
    },
    // 角色赋权
    handleEditBtnClick(row) {
      this.currentRole = row
      listPrivilegesByRole(row.role_name).then(res => {
        this.privilegesOfCurrentRole = res.data
      })
      this.$refs['rolePrivilegeDialog'].show()
    },
    // 查看角色详情
    handleShowBtnClick(row) {
      this.currentRole = {}
      this.currentRole.role_id = row.role_id
      this.currentRole.role_name = row.role_name
      this.currentRole.description = row.description
      this.currentRole.privileges = []
      listPrivilegesByRole(row.role_name).then(res => {
        this.currentRole.privileges = res.data
        this.$refs['roleDetailDialog'].show()
      })
    },
    // 删除角色
    handleDeleteBtnClick(row) {
      this.$prompt('您正在进行角色删除操作，此操作无法撤销！<br/><br/> 请输入角色名 <b>' + row.role_name + '</b> 确认删除', "提示", {
          confirmButtonText: "确定", 
          cancelButtonText: "取消", 
          inputValidator(value) {
            if (value !== row.role_name) {
              return '输入的角色名有误'
            }
          },
          dangerouslyUseHTMLString: true,
          closeOnClickModal: false}).then(
            ({ value }) => { 
              deleteRole(row.role_name).then(response => { 
                this.$message.success("成功删除角色 " + row.role_name)
                this.queryTableData()
              })
          })
      },
    // 判断是否是系统内置角色
    isSystemRole(role) {
    for (var i = 0; i < SYSTEM_ROLES.length; i++) {
      if (SYSTEM_ROLES[i] === role) {
        return true
      }
    }
    return false
    },
    // 查询角色列表
    queryTableData() {
      this.tableLoading = true
      listRoles(this.filter, this.pagination.current, this.pagination.size).then(res => {
        let total = Number(res.headers[X_RECORD_COUNT])
        if (res.data.length == 0 && total > 0) {
          this.pagination.current--
          if (this.pagination.current > 0){
            this.queryTableData()
          }
        }else {
          this.tableData = res.data
          this.pagination.total = total
        }
      }).finally(() => {
        this.tableLoading = false
      })
      this.tableLoading = false
    },
    // 执行搜索
    doSearch() {
      let filter = {}
      if (this.searchParams.roleName != undefined) {
        filter['roleName'] = {
          $regex: this.$util.escapeStr(this.searchParams.roleName)
        } 
      }
      this.pagination.current = 1
      this.filter = {...filter}
      this.queryTableData()
    },
    // 重置搜索
    resetSearch() {
      this.searchParams = {}
      this.filter = {}
      this.pagination.current = 1
      this.queryTableData()
    },
    // 当前页变化时
    handleCurrentChange(currentPage) {
      this.pagination.current = currentPage
      this.queryTableData()
    },
    getIndex(index) {
      return (this.pagination.current-1) * this.pagination.size + index + 1
    }
  },
  activated() {
    this.init()
  }
}
</script>
<style scoped>
.search-box {
  width: 40%;
  float: right;
  margin-bottom: 10px;
}
.input-with-select >>> .el-select {
  width: 100px;
}
.input-with-select >>> .el-input-group__prepend {
  background-color: #fff;
}
</style>
