<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="14">
          <el-input
            id="input_workspace_search_name"
            maxlength="50"
            size="small"
            placeholder="工作区名称"
            v-model="searchParams.workspaceName"
            @keyup.enter.native="doSearch"
            clearable>
          </el-input>
        </el-col>
        <el-col :span="5" >
            <el-button id="input_workspace_doSearche" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%" >搜索</el-button>
        </el-col>
        <el-col :span="5" >
            <el-button id="input_workspace_resetSearche" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%" >重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button type="danger" size="small" icon="el-icon-delete" style="margin-bottom:10px" @click="handleDeleteBtnClick(null)" :disabled="this.selectedWorkspaces.length === 0">批量删除</el-button>
    <el-button type="primary" size="small" icon="el-icon-plus" style="margin-bottom:10px" @click="$refs['createWorkspaceDialog'].show()">创建工作区</el-button>
    <el-table
        v-loading="tableLoading"
        :data="tableData"
        border
        row-key="oid"
        @selection-change="(list) => {this.selectedWorkspaces = list}"
        style="width: 100%">
        <el-table-column
          type="selection"
          width="50">
        </el-table-column>
        <el-table-column
          type="index"
          :index="getIndex"
          label="序号"
          width="55">
        </el-table-column>
        <el-table-column
          prop="name"
          width="160"
          show-overflow-tooltip
          label="工作区名称">
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          label="站点列表">
          <template slot-scope="scope">
            {{scope.row.site_list|arrayJoin}}
          </template>
        </el-table-column>
        <el-table-column
          prop="create_user"
          width="120"
          show-overflow-tooltip
          label="创建人">
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          label="创建时间">
          <template slot-scope="scope">
            {{scope.row.create_time|parseTime}}
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          show-overflow-tooltip
          label="描述信息"
          >
        </el-table-column>
        <el-table-column
          width="150"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_workspace_showDetail" type="primary"  @click="handleShowDetail(scope.row.name)" size="mini">查看</el-button>
              <el-button id="btn_workspace_delete" type="danger" @click="handleDeleteBtnClick(scope.row)" size="mini">删除</el-button>
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
      :total="pagination.total">
    </el-pagination>

    <!-- 删除工作区弹框 -->
    <el-dialog
      title="提示"
      :visible.sync="deleteWsDialogVisible"
      width="500px">
      <div class="tip-title"><i class="el-icon-warning warning-status"></i> {{confirmMsg}}</div>
      <el-row :gutter="10" style="margin-top:10px">
        <el-col :span="5">
          <span class="text-force-delete">强制删除</span>
          <el-tooltip effect="dark" placement="bottom" content="工作区内文件不为空时，也删除该工作区">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </el-col>
        <el-col :span="10">
          <el-radio-group v-model="isForceDelete" size="mini">
          <el-radio :label="true" border>是</el-radio>
          <el-radio :label="false" border>否</el-radio>
        </el-radio-group>
        </el-col>
      </el-row>
      <span slot="footer" class="dialog-footer">
        <el-button @click="deleteWsDialogVisible = false" size="mini">取 消</el-button>
        <el-button type="primary" @click="deleteWs" size="mini">确 定</el-button>
      </span>
    </el-dialog>

    <create-workspace-dialog ref="createWorkspaceDialog" @onWorkspaceCreated="queryTableData"></create-workspace-dialog>

  </div>
</template>
<script>
import {queryWorkspaceList, deleteWorkspaces} from '@/api/workspace'
import {X_RECORD_COUNT} from '@/utils/common-define'
import CreateWorkspaceDialog from './components/CreateWorkspaceDialog.vue'
export default {
  components:{
    CreateWorkspaceDialog
  },
  data(){
    return{
      input: "",
      expression: "",
      isForceDelete: false,
      showCron: false,
      deleteWsDialogVisible: false,
      pagination:{ 
        current: 1, //当前页
        size: 10, //每页大小
        total: 0, //总数据条数
      },
      filter: {},
      searchParams: {
        workspaceName: ''
      },
      selectedWorkspaces:[],
      tableLoading: false,
      tableData:[],
      confirmMsg: '',
      deletedWsNames: []
    }
  },
  computed:{
   
  },
  methods:{
    // 初始化
    init(){
      this.queryTableData()
    },
    // 查询表格数据
    queryTableData() {
      this.tableLoading = true
      queryWorkspaceList(this.pagination.current, this.pagination.size, this.filter).then(res => {
        this.pagination.total = Number(res.headers[X_RECORD_COUNT])
        this.tableData = res.data
      }).finally(()=>{
        this.tableLoading = false
      })
    },
    // 查看工作区详情
    handleShowDetail(name){
      this.$router.push("/workspace/detail/"+name)
    },
    // 点击搜索按钮
    doSearch() {
      this.pagination.current = 1
      let filter = {}
      if (this.searchParams.workspaceName) {
        filter['name'] = {
          $regex: this.$util.escapeStr(this.searchParams.workspaceName)
        }
      }
      this.filter = {...filter}
      this.queryTableData()
    },
    // 重置搜索条件
    resetSearch() {
      this.pagination.current = 1
      this.filter = {}
      this.searchParams = {
        workspaceName: ''
      }
      this.queryTableData()
    },
    //当前页变化时
    handleCurrentChange(currentPage){
      this.pagination.current = currentPage
      this.queryTableData()
    },
    getIndex(index) {
      return (this.pagination.current-1) * this.pagination.size + index + 1
    },
    // 点击了删除工作区按钮
    handleDeleteBtnClick(row) {
      this.deleteWsDialogVisible = true;
      if (row == null) {
        // 批量删除
        this.deletedWsNames = this.selectedWorkspaces.map(item => item.name)
        this.confirmMsg = `您确认删除工作区【${this.selectedWorkspaces.map(item => item.name).join(",")}】吗?`
      } else {
        // 删除单个工作区
        this.deletedWsNames = [row.name]
        this.confirmMsg = `您确认删除工作区【${row.name}】吗?`
      }
    },
    // 执行删除工作区操作
    deleteWs() {
      deleteWorkspaces(this.deletedWsNames, this.isForceDelete).then(res => {
        this.$util.showBatchOpMessage("删除工作区", res.data)
        this.deleteWsDialogVisible = false
        this.queryTableData()
      })
    }
  
  },
  created(){
  },
  activated() {
    this.init()
  }
  
}
</script>
<style  scoped>
.search-box {
  width: 40%;
  float: right;
  margin-bottom: 10px;
}
.text-force-delete {
  font-weight:700; 
  line-height:28px; 
  text-align:center;
}
.tip-title {
  line-height: 24px;
}
.warning-status {
  color: #E6A23C;
  font-size: 24px;
}
</style>