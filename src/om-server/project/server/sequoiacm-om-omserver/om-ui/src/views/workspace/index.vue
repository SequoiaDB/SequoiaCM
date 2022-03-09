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
    <el-table
        v-loading="tableLoading"
        :data="tableData"
        border
        row-key="oid"
        style="width: 100%">
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
          width="100"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_workspace_showDetail" type="primary"  @click="handleShowDetail(scope.row.name)" size="mini">查看详情</el-button>
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

  </div>
</template>
<script>
import {queryWorkspaceList} from '@/api/workspace'
import {X_RECORD_COUNT} from '@/utils/common-define'
export default {
  data(){
    return{
      input: "",
      expression: "",
      showCron: false,
      pagination:{ 
        current: 1, //当前页
        size: 10, //每页大小
        total: 0, //总数据条数
      },
      filter: {},
      searchParams: {
        workspaceName: ''
      },
      tableLoading: false,
      tableData:[],
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
</style>