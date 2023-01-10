<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="14">
          <el-input
            id="input_stage_tag_search_name"
            maxlength="50"
            size="small"
            placeholder="标签名称"
            v-model="searchParams.name"
            clearable
            @keyup.enter.native="doSearch">
          </el-input>
        </el-col>
        <el-col :span="5" >
          <el-button id="btn_stage_tag_doSearch" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%">搜索</el-button>
        </el-col>
        <el-col :span="5" >
          <el-button id="btn_stage_tag_resetSearch" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%">重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button id="btn_role_showCreateDialog" type="primary" size="small" icon="el-icon-plus"  @click="handleCreateBtnClick" style="margin-bottom:10px">新增</el-button>
    <el-table
        border
        :data="tableData"
        row-key="name"
        v-loading="tableLoading"
        style="width: 100%">
          <el-table-column
            type="index"
            :index="getIndex"
            label="序号"
            width="55">
          </el-table-column>
          <el-table-column
            prop="name"
            label="标签名称"
            show-overflow-tooltip
            width="230">
          </el-table-column>
          <el-table-column
            prop="site"
            show-overflow-tooltip
            label="应用站点"
            width="270">
            <template slot-scope="scope">
              <el-select
                id="select_transition_conf_site"
                v-model="scope.row.bindingSite" 
                placeholder="未关联站点" 
                size="small"
                width="220"
                :disabled="scope.row.bindingSite != null"
                @change="onBindingSite(scope.row)">
                <el-option
                  v-for="item in freeSiteList"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
              <el-tooltip content="点击解除阶段标签与该站点的关联" placement="top">
                <span><el-button v-if="scope.row.bindingSite" size="mini" icon="el-icon-remove-outline" @click="unbindindSite(scope.row)" circle style="margin-left:10px"></el-button></span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column
            prop="description"
            show-overflow-tooltip
            label="描述">
          </el-table-column>
          <el-table-column
            width="100"
            label="操作">
            <template slot-scope="scope">
              <el-button-group>
                <el-tooltip content="内置阶段标签，无法删除" :disabled="!isInnerStageTag(scope.row.name)" placement="top">
                  <span>
                    <el-button id="btn_stage_tag_delete" type="danger" :disabled="isInnerStageTag(scope.row.name)" @click="handleDeleteBtnClick(scope.row)" size="mini">删除</el-button>
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

    <!-- 新增阶段标签弹框 -->
    <stage-tag-create-dialog ref="stageTagCreateDialog" @onStageTagCreated="queryTableData"></stage-tag-create-dialog>
  </div>
</template>
<script>
import StageTagCreateDialog from './components/StageTagCreateDialog.vue'
import { Loading } from 'element-ui'
import { X_RECORD_COUNT, SYSTEM_STAGE_TAGS } from '@/utils/common-define'
import { listStageTag, deleteStageTag, addStageTag, removeStageTag } from "@/api/lifecycle"
import { querySiteList } from "@/api/site"
export default {
  components: {
    StageTagCreateDialog
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
        name: undefined
      },
      tableLoading: false,
      tableData: [],
      freeSiteList: []
    }
  },
  methods:{
    // 初始化
    init(){
      this.queryTableData()
      this.initFreeSiteList()
    },
    // 初始化空闲的（无阶段标签）的站点列表
    initFreeSiteList() {
      this.freeSiteList = []
      querySiteList().then(res => {
        let siteList = res.data
        if (siteList && siteList.length > 0) {
          for (let site of siteList) {
            if (!site.stage_tag || site.staget_tag === '') {
              this.freeSiteList.push(site.name)
            }
          }
        }
      })
    },
    // 新增阶段标签
    handleCreateBtnClick() {
      this.$refs['stageTagCreateDialog'].show()
    },
    // 站点设置阶段标签
    onBindingSite(row) {
      addStageTag(row.bindingSite, row.name).then(res => {
        this.$message.success(`阶段标签【${row.name}】关联站点成功`)
        this.freeSiteList.splice(this.freeSiteList.indexOf(row.bindingSite), 1);
        this.queryTableData()
      })
    },
    // 移除站点的阶段标签
    unbindindSite(row) {
      if (row.bindingSite && row.bindingSite !== '') {
        removeStageTag(row.bindingSite).then(res => {
          this.$message.success(`阶段标签【${row.name}】解除关联站点成功`)
          this.freeSiteList.push(row.bindingSite)
          this.queryTableData()
        })
      }
    },
    // 删除阶段标签
    handleDeleteBtnClick(row) {
      this.$confirm(`您确定删除阶段标签【${row.name}】吗`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        let loadingInstance = Loading.service({ fullscreen: true, text: "正在删除中..." })
        deleteStageTag(row.name).then(res => {
          this.$message.success(`删除成功`)
          this.queryTableData()
        }).finally(() => {
          loadingInstance.close()
        })  
      })
    }, 
    // 判断是否是内置阶段标签
    isInnerStageTag(stageTag)  {
      for (var i = 0; i < SYSTEM_STAGE_TAGS.length; i++) {
        if (SYSTEM_STAGE_TAGS[i] === stageTag) {
          return true
        }
      }
      return false
    },
    // 查询阶段标签列表
    queryTableData() { 
      this.tableLoading = true
      listStageTag(this.filter, this.pagination.current, this.pagination.size).then(res => {
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
      if (this.searchParams.name != undefined) {
        filter['nameMatcher'] = this.$util.escapeStr(this.searchParams.name)
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
