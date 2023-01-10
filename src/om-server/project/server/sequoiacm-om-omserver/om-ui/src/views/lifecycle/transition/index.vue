<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="9">
          <el-input
            id="input_transition_search_name"
            maxlength="50"
            size="small"
            placeholder="数据流名称"
            v-model="searchParams.name"
            clearable
            @keyup.enter.native="doSearch">
          </el-input>
        </el-col>
        <el-col :span="9">
          <el-select
            id="select_transition_search_stage_tag"
            v-model="searchParams.stageTag"
            size="small"
            placeholder="请选择阶段标签"
            clearable
            filterable
            style="width:100%">
            <el-option
              v-for="item in stageTagList"
              :key="item"
              :label="item"
              :value="item">
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="3" >
          <el-button id="btn_stage_tag_doSearch" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%">搜索</el-button>
        </el-col>
        <el-col :span="3" >
          <el-button id="btn_stage_tag_resetSearch" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%">重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button id="btn_role_showCreateDialog" type="primary" size="small" icon="el-icon-plus"  @click="handleCreateBtnClick" style="margin-bottom:10px">创建数据流</el-button>
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
            label="数据流名称"
            show-overflow-tooltip
            width="230">
          </el-table-column>
          <el-table-column
            prop="source"
            show-overflow-tooltip
            label="起始阶段"
            width="230">
          </el-table-column>
          <el-table-column
            prop="dest"
            show-overflow-tooltip
            label="目标阶段"
            width="230">
          </el-table-column>
          <el-table-column
            prop="wsList"
            show-overflow-tooltip
            label="生效范围">
            <template slot-scope="scope">
              <el-tag
                v-for="ws in scope.row.workspaces"
                :key="ws"
                style="margin-left: 3px">
                {{ws}}
              </el-tag>
              <el-tag
                v-for="ws in scope.row.workspaces_customized"
                :key="ws"
                style="margin-left: 3px">
                {{ws}}
              </el-tag>
              <el-button size="small" icon="el-icon-edit" circle style="margin-left: 5px" @click="handleSetWsBtnClick(scope.row)"></el-button>
            </template>
          </el-table-column>
          <el-table-column
            width="210"
            label="操作">
            <template slot-scope="scope">
              <el-button-group>
                <el-button id="btn_stage_tag_delete" @click="handleSearchBtnClick(scope.row)" size="mini">查看</el-button>
                <el-button id="btn_stage_tag_delete" @click="handleUpdateBtnClick(scope.row)" size="mini">编辑</el-button>
                <el-button id="btn_role_delete" type="danger" :disabled="scope.row.disabled" @click="handleDeleteBtnClick(scope.row)" size="mini">删除</el-button>
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

    <!-- 编辑数据流弹框 -->
    <transition-edit-dialog ref="transitionEditDialog" @onTransitionEdited="queryTableData"></transition-edit-dialog>
    <!-- 数据流详情弹框 -->
    <transition-detail-dialog ref="transitionDetailDialog"></transition-detail-dialog>
    <!-- 调整数据流生效范围弹框 -->
    <transition-ws-dialog ref="transitionWsDialog" @onWsChange="queryTableData"></transition-ws-dialog>
  </div>
</template>
<script>
import TransitionEditDialog from './components/TransitionEditDialog.vue'
import TransitionDetailDialog from './components/TransitionDetailDialog.vue'
import TransitionWsDialog from './components/TransitionWsDialog.vue'
import { Loading } from 'element-ui'
import { X_RECORD_COUNT, SYSTEM_ROLES } from '@/utils/common-define'
import { listStageTag, listTransition, deleteTransition } from "@/api/lifecycle"
export default {
  components: {
    TransitionEditDialog,
    TransitionDetailDialog,
    TransitionWsDialog
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
        name: undefined,
        stageTag: undefined
      },
      tableLoading: false,
      stageTagList: [],
      tableData: []
    }
  },
  methods:{
    // 初始化
    init(){
      this.queryTableData()
      this.queryStageTagList()
    },
    // 获取阶段标签列表
    queryStageTagList() {
      this.stageTagList = []
      listStageTag(null, 1, -1).then(res => {
        let stageTagList = res.data
        if (stageTagList && stageTagList.length > 0) {
          for (let stageTag of stageTagList) {
            this.stageTagList.push(stageTag.name)
          }
        }
      })
    },
    // 点击新增按钮
    handleCreateBtnClick() {
      this.$refs['transitionEditDialog'].show('create', null)
    },
    // 点击编辑数据流按钮
    handleUpdateBtnClick(row) {
      this.$refs['transitionEditDialog'].show('update', row, null, row.workspaces)
    },
    // 修改数据流的生效范围
    handleSetWsBtnClick(row) {
      let wsList = [...row.workspaces]
      let wsCustomList = [...row.workspaces_customized]
      this.$refs['transitionWsDialog'].show(row.name, wsList, wsCustomList)
    },
    // 查看数据流详情
    handleSearchBtnClick(row) {
      this.$refs['transitionDetailDialog'].show(row, null)
    },
    // 删除数据流
    handleDeleteBtnClick(row) {
      this.$confirm(`您确认删除数据流【${row.name}】吗`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_transition_confirmDelete',
        type: 'warning'
      }).then(() => {
        deleteTransition(row.name).then(res => {
          this.$message.success(`【${row.name}】删除成功`)
          this.queryTableData()
        })
      })
    },
    // 执行搜索
    doSearch() {
      let filter = {}
      if (this.searchParams.name != undefined) {
        filter['name_matcher'] = this.$util.escapeStr(this.searchParams.name)
      }
      if (this.searchParams.stageTag != undefined) {
        filter['stage_tag'] = this.$util.escapeStr(this.searchParams.stageTag)
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
    // 查询数据流列表
    queryTableData() {
      this.tableLoading = true
      listTransition(this.filter, this.pagination.current, this.pagination.size).then(res => {
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
  width: 60%;
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
