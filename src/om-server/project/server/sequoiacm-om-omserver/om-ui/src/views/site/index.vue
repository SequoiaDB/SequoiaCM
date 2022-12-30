<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="14">
          <el-input
            maxlength="50"
            size="small"
            placeholder="站点名称"
            v-model="searchParams.siteName"
            @keyup.enter.native="doSearch"
            clearable>
          </el-input>
        </el-col>
        <el-col :span="5" >
            <el-button @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%">搜索</el-button>
        </el-col>
        <el-col :span="5" >
            <el-button @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%">重置</el-button>
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
          prop="id"
          width="100"
          label="站点ID">
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          label="站点名称">
          <template slot-scope="scope">
            {{scope.row.name}}<el-tag v-if="scope.row.is_root_site" style="margin-left:5px" size="mini" type="danger">主站点</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="datasource_type"
          show-overflow-tooltip
          label="数据源类型">
        </el-table-column>
        <el-table-column
          prop="datasource_user"
          show-overflow-tooltip
          label="数据源用户">
          <template slot-scope="scope">
            {{scope.row.datasource_user ? scope.row.datasource_user : '无'}}
          </template>
        </el-table-column>
       <el-table-column
          prop="datasource_url"
          show-overflow-tooltip
          label="数据源地址">
           <template slot-scope="scope">
            {{scope.row.datasource_url ? scope.row.datasource_url.join(",") : '无'}}
          </template>
        </el-table-column>
        <el-table-column
          width="100"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_site_showDetail" type="primary"  @click="handleShowDetail(scope.$index)" size="mini">查看</el-button>
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
    
    <site-detail-dialog :curSiteDetail="curSiteDetail" ref="siteDetailDialog"></site-detail-dialog>
  </div>
</template>
<script>
import {querySiteList} from '@/api/site'
import {X_RECORD_COUNT} from '@/utils/common-define'
import SiteDetailDialog from './components/SiteDetailDialog.vue'
export default {
  components:{
   SiteDetailDialog
  },
  data(){
    return{
      input: "",
      pagination:{ 
        current: 1, //当前页
        size: 10, //每页大小
        total: 0, //总数据条数
      },
      filter: {},
      searchParams: {
        siteName: ''
      },
      tableLoading: false,
      tableData: [],
      curSiteDetail: {}
    }
  },
  computed:{
   
  },
  methods:{
    // 初始化
    init(){
     this.queryTableData()
    },
    //查询表格数据
    queryTableData() {
      this.tableLoading = true
      querySiteList(this.pagination.current, this.pagination.size, this.filter).then(res => {
        this.pagination.total = Number(res.headers[X_RECORD_COUNT])
        this.tableData = res.data
      }).finally(()=>{
        this.tableLoading = false
      }) 
    },
    
    // 查看站点详情
    handleShowDetail(index,row){
      this.curSiteDetail = this.tableData[index]
      this.$refs['siteDetailDialog'].show()
    },
    // 点击搜索按钮
    doSearch() {
      this.pagination.current = 1
      let filter = {}
      if (this.searchParams.siteName) {
        filter['name'] = {
          $regex: this.$util.escapeStr(this.searchParams.siteName)
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
        siteName: ''
      }
      this.queryTableData()
    },
    //当前页变化时
    handleCurrentChange(currentPage){
      this.pagination.current = currentPage
      this.queryTableData()
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
</style>