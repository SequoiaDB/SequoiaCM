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
            label="应用站点"
            width="400">
            <template slot-scope="scope">
              <el-tag
                :key="site"
                v-for="site in scope.row.bindingSite"
                style="margin-left: 4px; margin-top: 4px"
                closable
                @close="handleRemoveSite(scope.row.name, site)">
                {{site}}
              </el-tag>
              <el-button size="mini" icon="el-icon-plus" style="margin-left: 4px; margin-top: 4px" @click="handlApplySite(scope.row)">添加</el-button>
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
    <!-- 阶段标签添加站点弹框 -->
    <stage-tag-apply-site-dialog ref="stageTagApplySiteDialog" @onStageTagChanged="queryTableData"></stage-tag-apply-site-dialog>
  </div>
</template>
<script>
import StageTagCreateDialog from './components/StageTagCreateDialog.vue'
import StageTagApplySiteDialog from './components/StageTagApplySiteDialog.vue'
import { Loading } from 'element-ui'
import { X_RECORD_COUNT, SYSTEM_STAGE_TAGS } from '@/utils/common-define'
import { listStageTag, deleteStageTag, removeStageTag } from "@/api/lifecycle"
export default {
  components: {
    StageTagCreateDialog,
    StageTagApplySiteDialog
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
      tableData: []
    }
  },
  methods:{
    // 初始化
    init(){
      this.queryTableData()
    },
    // 新增阶段标签
    handleCreateBtnClick() {
      this.$refs['stageTagCreateDialog'].show()
    },
    // 站点设置阶段标签
    handlApplySite(row) {
      this.$refs['stageTagApplySiteDialog'].show(row.name)
    },
    // 移除站点的阶段标签
    handleRemoveSite(stageTag, site) {
      let confirmMsg = `您确认解除阶段标签【${stageTag}】与站点【${site}】的关联吗吗`
      this.$confirm(confirmMsg, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_site_confirmRemoveStageTag',
        type: 'warning'
      }).then(() => {
        removeStageTag(site).then(res => {
          this.$message.success(`阶段标签【${stageTag}】解除关联站点【${site}】成功`)
          this.queryTableData()
        })
      })
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
