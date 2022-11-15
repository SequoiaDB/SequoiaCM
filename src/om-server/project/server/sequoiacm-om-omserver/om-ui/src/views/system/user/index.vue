<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="8">
          <el-input
            id="input_user_search_username"
            maxlength="50"
            size="small"
            placeholder="请输入用户名称"
            v-model="searchName"
            clearable
            @keyup.enter.native="doSearch">
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select
            id="query_user_select_role"
            placeholder="请选择角色名称"
            v-model="searchParams.has_role"
            size="small"
            style="width:100%"
            filterable>
            <el-option
              v-for="item in roleList"
              :key="item.id"
              :label="item.role_name"
              :value="item.role_name">
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select
            id="query_user_select_status"
            placeholder="用户状态"
            v-model="searchParams.enabled"
            size="small"
            style="width:100%">
            <el-option
              v-for="item in statusList"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="3" >
          <el-button id="btn_user_doSearch" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%">搜索</el-button>
        </el-col>
        <el-col :span="3" >
          <el-button id="btn_user_resetSearch" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%">重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button id="btn_user_showCreateDialog" type="primary" size="small" icon="el-icon-plus"  @click="handleCreateBtnClick" style="margin-bottom:10px">新增</el-button>
    <el-table
        border
        :data="tableData"
        row-key="user_id"
        v-loading="tableLoading"
        style="width: 100%">
        <el-table-column
          type="index"
          :index="getIndex"
          label="序号"
          width="55">
        </el-table-column>
        <el-table-column
          prop="user_name"
          label="用户名称"
          show-overflow-tooltip
          width="230">
        </el-table-column>
        <el-table-column
          prop="roles"
          show-overflow-tooltip
          label="用户角色">
          <template slot-scope="scope">
            <el-tag
              v-for="role in scope.row.roles"
              :key="role.role_name">
              {{role.role_name}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="enable"
          width="120"
          label="状态">
          <template slot-scope="scope">
            <el-switch
              :id="'input-user-switch-'+scope.row.user_id"
              v-model="scope.row.enable"
              @click.native="handleChangeUserState($event,scope.row)"
              :active-value="true"
              :inactive-value="false"
              disabled
              active-color="#13ce66">
            </el-switch>
          </template>
        </el-table-column>
        <el-table-column
          width="300"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_user_showDetailDialog" size="mini" @click="handleSearchBtnClick(scope.row)">查看</el-button>
              <el-button id="btn_user_roleDialog" size="mini" @click="handleRoleBtnClick(scope.row)">角色分配</el-button>
              <el-button id="btn_user_resetPassword" @click="handleResetPwdBtnClick(scope.row)" size="mini">重置密码</el-button>
              <el-tooltip content="无法删除当前用户" :disabled="scope.row.user_name !== user_name" placement="top">
                <span>
                  <el-button id="btn_user_delete" type="danger" :disabled="scope.row.user_name === user_name" @click="handleDeleteBtnClick(scope.row)" size="mini">删除</el-button>
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
    <user-create-dialog ref="userCreateDialog" @onUserCreated="queryTableData"></user-create-dialog>
    <!-- 用户详情弹框 -->
    <user-detail-dialog ref="userDetailDialog" :user="currentUserDetail"></user-detail-dialog>
    <!-- 角色分配弹框 -->
    <user-role-dialog ref="userRoleDialog" :user="currentUser" :allRoles="allRoles" :hasRoles="roleOfCurrentUser" @refreshTable="queryTableData"></user-role-dialog>
    <!-- 重置用户密码弹框 -->
    <user-reset-pwd-dialog :username="currentUser" :roles="roleOfCurrentUser" ref="userResetPwdDialog"></user-reset-pwd-dialog>
  </div>
</template>
<script>
import UserCreateDialog from './components/UserCreateDialog.vue'
import UserDetailDialog from './components/UserDetailDialog.vue'
import UserRoleDialog from './components/UserRoleDialog.vue'
import UserResetPwdDialog from './components/UserResetPwdDialog.vue'
import { Loading } from 'element-ui'
import { listUsers, deleteUser, enableUser, disableUser } from "@/api/user"
import { listRoles } from "@/api/role"
import { X_RECORD_COUNT} from '@/utils/common-define'
import { mapGetters } from 'vuex'
export default {
  components: {
    UserCreateDialog,
    UserDetailDialog,
    UserRoleDialog,
    UserResetPwdDialog
  },
  computed: {
    ...mapGetters([
    'user_name'
    ])
  },
  data(){
    return {
      pagination:{
        current: 1, //当前页
        size: 12, //每页大小
        total: 0, //总数据条数
      },
      userFilter: {},
      searchName: undefined,
      searchParams: {
        has_role: undefined,
        enabled: undefined
      },  
      tableLoading: false,
      tableData: [],
      currentUser: '',
      currentUserDetail: {},
      roleOfCurrentUser: [],
      allRoles: [],
      roleList: [],
      statusList: [
        {
          "label" : "正常",
          "value" : true
        },
        {   
          "label" : "停用",
          "value" : false
        } 
      ]
    }
  },
  methods:{
    // 初始化
    async init(){
      await this.queryTableData()
      await this.initRoleList()
    },
    // 获取角色列表
    initRoleList() {
      listRoles(null, 1, -1).then(res => {
        this.roleList = res.data
      })
    },
    // 文件列表选择项发生变化
    selectionChange(userIdList) {
      this.userIdList=[];
      userIdList.forEach(item => {
        this.userIdList.push(item.id);
      });
    },
    // 新增用户
    handleCreateBtnClick() {
      this.$refs['userCreateDialog'].show()
    },
    // 更改用户状态
    handleChangeUserState(e, row) {
      let action = row.enable ? '禁用' : '启用'
      let confirmMsg = `您确认${action}用户 ${row.user_name} 吗`
      if (row.enable) {
        confirmMsg += '， 禁用后该用户将无法登录'
      } 
      this.$confirm(confirmMsg, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_user_confirmChangeState',
        type: 'warning'
      }).then(() => {
        if (row.enable) {
          disableUser(row.user_name).then(res => {
            this.$message.success(`用户${action}成功`)
            row.enable = !row.enable
          })
        }
        else {
          enableUser(row.user_name).then(res => {
            this.$message.success(`用户${action}成功`)
            row.enable = !row.enable
          })
        }
      })
    },
    // 角色分配
    handleRoleBtnClick(row) {
      this.currentUser = row.user_name
      this.roleOfCurrentUser = []
      for (let i in row.roles) {
        let role = row.roles[i]
        this.roleOfCurrentUser.push(role.role_name)
      }
      this.allRoles = []
      for (let i in this.roleList) {
        let role = this.roleList[i]
        this.allRoles.push({
          'key' : role.role_name,
          'label' : role.role_name
        })
      }
      this.$refs['userRoleDialog'].show()
    },
    // 查看用户详情
    handleSearchBtnClick(row) {
      this.currentUserDetail = row
      this.$refs['userDetailDialog'].show()
    },
    // 重置密码操作
    handleResetPwdBtnClick(row) {
      this.currentUser = row.user_name
      this.roleOfCurrentUser = []
      for (let i in row.roles) {
        let role = row.roles[i]
        this.roleOfCurrentUser.push(role.role_name)
      }
      this.$refs['userResetPwdDialog'].show()
    },
    // 删除用户
    handleDeleteBtnClick(row) {
      this.$prompt('您正在进行用户删除操作，此操作无法撤销！<br/><br/> 请输入用户名 <b>' + row.user_name + '</b> 确认删除', "提示", {
          confirmButtonText: "确定", 
          cancelButtonText: "取消", 
          inputValidator(value) {
            if (value !== row.user_name) {
              return '输入的用户名有误'
            }
          },
          dangerouslyUseHTMLString: true,
          closeOnClickModal: false}).then(
            ({ value }) => { 
              deleteUser(row.user_name).then(response => { 
                this.$message.success("成功删除用户 " + row.user_name)
                this.queryTableData()
              })
          })
      },
    // 查询用户列表
    queryTableData() {
      this.tableLoading = true
      this.userFilter['name_matcher'] = this.searchName
      listUsers(this.userFilter, this.pagination.current, this.pagination.size).then(res => {
        let total = Number(res.headers[X_RECORD_COUNT])
        if (res.data.length == 0 && total > 0) {
          this.pagination.current--
          if (this.pagination.current > 0){
            this.queryTableData()
          }
        } else {
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
      if (this.searchParams.has_role != undefined) {
        filter['has_role'] = this.searchParams.has_role
      }
      if (this.searchParams.enabled != undefined) {
        filter['enabled'] = this.searchParams.enabled
      }
      this.pagination.current = 1
      this.userFilter = {...filter}
      this.queryTableData()
    },
    // 重置搜索
    resetSearch() {
      this.searchName = undefined
      this.searchParams = {}
      this.userFilter = {}
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
    },
  },
  activated() {
    this.init()
  }
}
</script>
<style scoped>
.search-box {
  width: 55%;
  float: right;
  margin-bottom: 10px;
}
.input-with-select >>> .el-select {
  width: 100px;
}
.input-with-select >>> .el-input-group__prepend {
  background-color: #fff;
}
.app-container >>> .el-switch.is-disabled{
  opacity: 1;
}
.app-container .el-switch.is-disabled >>>  .el-switch__core{
  cursor: pointer;
}
</style>
