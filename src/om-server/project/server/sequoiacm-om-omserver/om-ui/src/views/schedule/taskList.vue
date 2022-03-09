<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="9">
          <el-input
            id="input_task_search_id"
            size="small"
            maxlength="50"
            placeholder="任务ID"
            v-model="searchParams.task_id"
            @keyup.enter.native="doSearch"
            clearable>
          </el-input>
        </el-col>
        <el-col :span="9">
           <el-select id="select_task_search_status" v-model="searchParams.status" size="small" placeholder="运行状态" clearable style="width:100%">
            <el-option
              v-for="item in taskStatus"
              :key="item.value"
              :label="item.text"
              :value="item.value">
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="3" >
            <el-button id="btn_task_doSearch" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%" >搜索</el-button>
        </el-col>
        <el-col :span="3" >
            <el-button id="btn_task_resetSearch" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%" >重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 调度任务基本信息 -->
    <div class="schedule-basic">
      <span class="key" >任务名称：</span><span class="value">{{scheduleInfo.name}}</span>
      <span class="key" style="margin-left:10px">任务类型：</span><span class="value">{{getTaskTypeName(scheduleInfo.type)}}</span>
      <a class="more" id="a_task_showMoreScheduleInfo" @click="openScheduleDetailDialog">更多</a>
    </div>
    <!-- 表格部分 -->
    <el-table
        v-loading="tableLoading"
        :data="tableData"
        border
        row-key="task_id"
        style="width: 100%">
        <el-table-column
          type="index"
          :index="getIndex"
          label="序号"
          width="55">
        </el-table-column>
        <el-table-column
          prop="task_id"
          show-overflow-tooltip
          label="任务ID">
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          prop="actual_count"
          width="100"
          label="预期文件数">
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          prop="success_count"
          width="100"
          label="成功文件数">
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          prop="fail_count"
          width="100"
          label="失败文件数">
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          label="开始时间">
          <template slot-scope="scope">
            {{scope.row.start_time|parseTime}}
          </template>
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          label="结束时间">
          <template slot-scope="scope">
            {{scope.row.stop_time|parseTime}}
          </template>
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          width="100"
          label="运行状态">
          <template slot-scope="scope">
            <el-tag :color="getStatusColor(scope.row.status)" style="color: white"><i v-if="scope.row.status == 2" class="el-icon-loading"></i> {{getStatusText(scope.row)}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          width="100"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_task_stop" type="danger" :disabled="scope.row.status != 2" size="mini" @click="handleStopBtnClick(scope.row)">停止</el-button>
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

    <!-- 调度任务详情弹框 -->
    <schedule-detail-dialog ref="detailDialog" :taskDetail="scheduleInfo"></schedule-detail-dialog>

  </div>
</template>
<script>
import ScheduleDetailDialog from '@/components/ScheduleDetailDialog/index.vue'
import {stopTask} from '@/api/task'
import {queryTasks, queryScheduleDetail} from '@/api/schedule'
import {X_RECORD_COUNT, TASK_STATUS, TASK_TYPES} from '@/utils/common-define'
export default {
  components: {
    ScheduleDetailDialog
  },
  data(){
    return{
      pagination: { 
        current: 1, //当前页
        size: 10, //每页大小
        total: 0, //总数据条数
      },
      tableLoading: false,
      scheduleId: this.$route.params.id,
      // 查询过滤参数
      filter: {},
      // 搜索参数
      searchParams: {
        task_id: '',
        status: ''
      },
      // 调度任务信息
      scheduleInfo: {},
      taskStatus: TASK_STATUS,
      taskTypes: TASK_TYPES,
      tableData: [],
      // 表格自动刷新时间间隔
      interval: 5000,
      refresh: ''
    }
  },
  computed:{
   
  },
  methods:{

    // 初始化
    init(){
      this.queryTableData()
      this.queryScheduleInfo()
      this.setRefresh()
    },
    // 查询表格数据
    queryTableData() {
      this.tableLoading = true
      queryTasks(this.scheduleId, this.filter, this.pagination.current, this.pagination.size).then(res => {
        this.tableData = res.data
        this.pagination.total = Number(res.headers[X_RECORD_COUNT])
      }).finally(() => {
        this.tableLoading = false
      })
    },
    // 查询调度任务信息
    queryScheduleInfo() {
      queryScheduleDetail(this.$route.params.id).then(res => {
        this.scheduleInfo = res.data
      })
    },
    // 点击停止任务按钮
    handleStopBtnClick(row) {
      this.$confirm(`您确认停止该任务吗`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_task_confirmStop',
        type: 'warning'
      }).then(() => {
        stopTask(row.task_id).then(res => {
          this.$message.success(`任务停止成功`)
          this.queryTableData()
        })
      }).catch(() => {
      });
    },
    // 查看调度任务详情
    openScheduleDetailDialog() {
      this.$refs['detailDialog'].show()
    },
    // 执行搜索
    doSearch() {
      this.pagination.current = 1
      let filter = {}
      if (this.searchParams.task_id) {
        filter['id'] = this.$util.escapeStr(this.searchParams.task_id)
      }
      if (this.searchParams.status) {
        filter['running_flag'] = parseInt(this.$util.escapeStr(this.searchParams.status))
      }
      this.filter = {...filter}
      this.queryTableData()
    },
    // 重置搜索
    resetSearch() {
      this.pagination.current = 1
      this.searchParams = {
        task_id: '',
        status: ''
      }
      this.filter = {}
      this.queryTableData()
    },
    // 表格数据自动刷新
    setRefresh() {
      this.refresh = setTimeout(() => {
        queryTasks(this.scheduleId , this.filter, this.pagination.current, this.pagination.size).then(res => {
        this.tableData = res.data
        this.pagination.total = Number(res.headers[X_RECORD_COUNT])
      }).finally(() => {
        this.setRefresh()
      })
      }, this.interval)
    },
    clearRefresh() {
      clearTimeout(this.refresh)
    },
    getStatusColor(status) {
      for (const task of this.taskStatus) {
        if (task.value == status) {
          return task.color
        }
      }
    },
    getStatusText(row) {
      if (row.status == 2) {
          return row.progress + "%"
      }
      for (const task of this.taskStatus) {
        if (task.value == row.status) {
          return task.text
        }
      }
    },
    // 获取任务类型名称
    getTaskTypeName(taskType) {
      for (const task of this.taskTypes) {
        if (task.value == taskType){
          return task.label
        }
      }
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
  created(){
    this.init()
  },
  beforeRouteLeave(to, from, next) {
    this.clearRefresh()
    next()
  }
  
  
}
</script>
<style  scoped>
.search-box {
  width: 50%;
  float: right;
  margin-bottom: 10px;
}
.schedule-basic {
  line-height: 32px;
}
.schedule-basic .more {
  color: #66b1ff;
  margin-left: 5px;
  font-size: 13px;
}
.schedule-basic .key{
  font-size: 13px;
  font-weight: 600;
  opacity: 0.8;
}
.schedule-basic .value {
  font-size: 14px;
  color: #606266;
}
</style>