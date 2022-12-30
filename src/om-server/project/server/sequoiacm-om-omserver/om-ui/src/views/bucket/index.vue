<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="14">
          <el-input
            maxlength="50"
            size="small"
            placeholder="桶名称"
            v-model="searchParams.bucketName"
            @keyup.enter.native="doSearch"
            clearable>
          </el-input>
        </el-col>
        <el-col :span="5" >
            <el-button @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%" >搜索</el-button>
        </el-col>
        <el-col :span="5" >
            <el-button @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%" >重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button type="danger" size="small" icon="el-icon-delete"  style="margin-bottom:10px" :disabled="selectedBuckets.length <= 0" @click="handleDeleteBtnClick(null)">批量删除</el-button>
    <el-button type="primary" size="small" icon="el-icon-plus"  style="margin-bottom:10px" @click="$refs['createBucketDialog'].show()">创建桶</el-button>
    <el-table
        v-loading="tableLoading"
        :data="tableData"
        border
        row-key="oid"
        @selection-change="(list) => {this.selectedBuckets = list}"
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
          show-overflow-tooltip
          label="桶名">
        </el-table-column>
        <el-table-column
          prop="workspace"
          show-overflow-tooltip
          label="所属region">
        </el-table-column>
        <el-table-column
          width="300px"
          show-overflow-tooltip
          label="版本控制状态">
          <template slot-scope="scope">
            <el-radio-group :value="scope.row.version_status" size="mini" >
              <el-radio-button label="Disabled"  v-if="scope.row.version_status==='Disabled'"></el-radio-button>
              <el-radio-button label="Enabled" @click.native.prevent="handleChangeState($event, scope.row, 'Enabled')"></el-radio-button>
              <el-radio-button label="Suspended" @click.native.prevent="handleChangeState($event, scope.row, 'Suspended')"></el-radio-button>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column
          prop="create_user"
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
          width="180"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button type="primary" @click="handleShowDetail(scope.row.name)" size="mini">查看</el-button>
              <el-button type="danger" @click="handleDeleteBtnClick(scope.row)" size="mini">删除</el-button>
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
    <create-bucket-dialog ref="createBucketDialog" @onBucketCreated="queryTableData"></create-bucket-dialog>
    <bucket-detail-dialog ref="bucketDetailDialog"></bucket-detail-dialog>

  </div>
</template>
<script>
import CreateBucketDialog from './components/CreateBucketDialog.vue'
import {queryBucketList, deleteBucket, updateBucket, queryBucketDetail} from '@/api/bucket'
import {X_RECORD_COUNT} from '@/utils/common-define'
import BucketDetailDialog from './components/BucketDetailDialog.vue'
export default {
  components:{
    CreateBucketDialog,
    BucketDetailDialog
  },
  data(){
    return{
      selectedBuckets: [],
      pagination:{ 
        current: 1, //当前页
        size: 10, //每页大小
        total: 0, //总数据条数
      },
      orderParam: { //记录排序参数
        prop: 'create_time',
        order: -1
      },
      filter: {},
      searchParams: {
        bucketName: ''
      },
      tableLoading: false,
      tableData:[],
    }
  },
  methods:{
    // 初始化
    init(){
      this.queryTableData()
    },

    // 查询表格数据
    queryTableData() {
      this.tableLoading = true
      let orderBy = {}
      orderBy[this.orderParam.prop] = this.orderParam.order
      queryBucketList(this.pagination.current, this.pagination.size, this.filter, orderBy, true).then(res => {
        this.pagination.total = Number(res.headers[X_RECORD_COUNT])
        this.tableData = res.data
      }).finally(()=>{
        this.tableLoading = false
      })
    },

    // 查看桶详情
    handleShowDetail(name){
      queryBucketDetail(name).then(res => {
        let currentBucketDetail = JSON.parse(res.headers['bucket'])
        this.$refs['bucketDetailDialog'].show(currentBucketDetail)
      })
    },

    // 点击搜索按钮
    doSearch() {
      this.pagination.current = 1
      let filter = {}
      if (this.searchParams.bucketName) {
        filter['name'] = {
          $regex: this.$util.escapeStr(this.searchParams.bucketName)
        }
      }
      this.filter = {...filter}
      this.queryTableData()
    },

    handleChangeState(e, row, status) {
       this.$confirm(`您确认修改桶：${row.name} 的版本控制状态为 ${status} 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        
        updateBucket(row.name, {version_status:status}).then(res => {
          this.$message.success("修改成功")
          this.queryTableData()
        })
      }).catch(() => {

      })
    },

    handleDeleteBtnClick(row) {
      let confirmMsg = ''
      let deleteBucketNames = []
      if (row == null) {
        // 批量删除
        deleteBucketNames = this.selectedBuckets.map(item => item.name)
        confirmMsg = `您确认删除桶【${this.selectedBuckets.map(item => item.name).join(", ")}】吗？`
      } else {
        // 删除单个工作区
        deleteBucketNames.push(row.name)
        confirmMsg = `您确认删除桶【${row.name}】吗？`
      }
      this.$confirm(confirmMsg, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteBucket(deleteBucketNames).then(res => {
          this.$util.showBatchOpMessage("删除桶", res.data)
          this.queryTableData()
        })
      }).catch(() => {

      });
    },

    // 重置搜索条件
    resetSearch() {
      this.pagination.current = 1
      this.filter = {}
      this.searchParams = {
        bucketName: ''
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