<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="9">
          <el-input
            id="input_schedule_search_scheduleName"
            maxlength="50"
            size="small"
            placeholder="调度任务名称"
            v-model="searchParams.name"
            clearable
            @keyup.enter.native="doSearch">
          </el-input>
        </el-col>
        <el-col :span="9">
          <el-select
            id="select_schedule_search_workspace"
            v-model="searchParams.workspace"
            size="small"
            placeholder="请选择工作区"
            clearable
            filterable
            style="width:100%">
            <el-option
              v-for="item in workspaceList"
              :key="item"
              :label="item"
              :value="item">
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="3" >
            <el-button id="btn_schedule_doSearch" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%" >搜索</el-button>
        </el-col>
        <el-col :span="3" >
            <el-button id="btn_schedule_resetSearch" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%" >重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button id="btn_schedule_showCreateDialog" @click="hanldeAddBtnClick" type="primary" size="small" icon="el-icon-plus"  style="margin-bottom:10px">创建调度任务</el-button>
    <el-table
        v-loading="tableLoading"
        :data="tableData"
        border
        row-key="schedule_id"
        style="width: 100%">
        <el-table-column
          type="index"
          :index="getIndex"
          label="序号"
          width="55">
        </el-table-column>
        <el-table-column
          prop="name"
          show-overflow-tooltip
          label="任务名称">
        </el-table-column>
        <el-table-column
          show-overflow-tooltip
          label="任务类型">
          <template slot-scope="scope">
            {{getTaskTypeName(scope.row.type)}}
          </template>
        </el-table-column>
        <el-table-column
          prop="workspace"
          show-overflow-tooltip
          label="所属工作区">
        </el-table-column>
        <el-table-column
          prop="description"
          show-overflow-tooltip
          label="任务描述">
        </el-table-column>
        <el-table-column
        prop="enable"
        width="120"
        label="是否启用">
        <template slot-scope="scope">
            <el-switch
              :id="'input-schedule-switch-'+scope.row.schedule_id"
              v-model="scope.row.enable"
              disabled
              @click.native="handleChangeState($event,scope.row)"
              :active-value="true"
              :inactive-value="false"
              active-color="#13ce66">
            </el-switch>
        </template>
        </el-table-column>
        <el-table-column
          width="300"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_schedule_showDetailDialog" size="mini" @click="handleShowBtnClick(scope.row)">查看</el-button>
              <el-button id="btn_schedule_showEditdeleteDialog" size="mini" @click="handleEditBtnClick(scope.row)">编辑</el-button>
              <el-button id="btn_schedule_showTasks" size="mini" @click="handleShowTasksBtnClick(scope.row)">运行记录</el-button>
              <el-button id="btn_schedule_showDeleteDialog" size="mini" type="danger" @click="handleDeleteBtnClick(scope.row)">删除任务</el-button>
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

    <!-- 新增/编辑调度任务对话框 -->
    <el-dialog
      :title="(operation == 'create' ? '创建':'编辑')+'调度任务'"
      top="10px"
      :visible.sync="scheduleDialogVisible"
      class="edit_dialog"
      width="40%"
      @close="onDialogClose">
       <el-form ref="form" v-loading="scheduleDialogLoading" :rules="rules" :model="form" size="small" label-width="110px">
        <el-form-item label="任务名称"  prop="name">
          <el-input id="input_schedule_name" v-model="form.name" maxlength="30"  placeholder="请输入任务名称"></el-input>
        </el-form-item>
        <el-form-item label="任务类型"  prop="type">
          <el-select
            id="select_schedule_type"
            v-model="form.type"
            size="small"
            placeholder="请选择任务类型"
            clearable
            style="width:100%"
            :disabled=selectTypeForbidden
            @change="onTaskTypeChange" >
            <el-option
              v-for="item in taskTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="工作区"  prop="workspace">
          <el-select
            id="select_schedule_workspace"
            v-model="form.workspace"
            size="small"
            placeholder="请选择工作区"
            clearable filterable
            style="width:100%"
            @change="onWorkspaceChange" >
            <el-option
              v-for="item in workspaceList"
              :key="item"
              :label="item"
              :value="item">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type" :label="form.type == 'clean_file' ? '清理站点' : form.type == 'recycle_space' ? '回收站点' : '源站点'"  prop="sourceSite">
          <el-select
            id="select_schedule_sourceSite"
            v-model="form.sourceSite"
            :no-data-text="form.workspace ? '无数据' : '请先选择工作区'"
            size="small"
            placeholder="请选择站点"
            clearable filterable
            style="width:100%"
            @change="onSourceSiteChange">
            <el-option
              v-for="item in sourceSiteList"
              :key="item"
              :label="item"
              :value="item">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目标站点" v-if="form.type == 'copy_file' || form.type == 'move_file'"  prop="targetSite">
          <el-select
            id="select_schedule_targetSite"
            v-model="form.targetSite"
            size="small"
            placeholder="请选择目标站点"
            :no-data-text="form.workspace ? '无数据' : '请先选择工作区'"
            clearable filterable
            style="width:100%"
            @change="onTargetSiteChange" >
            <el-option
              v-for="item in targetSiteList"
              :key="item"
              :label="item"
              :value="item">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="cron"  prop="cron">
          <el-input id="input_schedule_cron" v-model="form.cron" maxlength="100"  placeholder="请输入Cron表达式">
            <template #suffix>
              <el-tooltip class="item" effect="dark" :content="'点击' + (showCronPicker ? '隐藏' : '显示') + 'cron快捷生成工具'" placement="top">
                <i class="el-icon-question pointer-cursor no-select" @click="showCronPicker = !showCronPicker"></i>
              </el-tooltip>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="cron快捷生成" v-if="showCronPicker">
          <cron-picker  ref="cronPicker" :cron="form.cron" @change="onCronChange" />
        </el-form-item>
        <el-form-item label="文件范围"  prop="scope" v-if="form.type && form.type !== 'recycle_space'">
          <el-select id="select_schedule_scope" v-model="form.scope" size="small" placeholder="请选择文件范围" clearable style="width:100%">
            <el-option
              v-for="item in fileScopeTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="文件停留时间" prop="maxStayTime"  v-if="form.type && form.type !== 'recycle_space'">
              <el-input id="input_schedule_maxStayTime" v-model.number="form.maxStayTime" maxlength="9"   placeholder="单位：天">
                <template #suffix>
                  <el-tooltip class="item" effect="dark" content="调度站点的文件存在时间超过指定时间才会被调度" placement="top">
                  <i class="el-icon-question pointer-cursor"></i>
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="空间回收范围" prop="recycleScope" v-if="form.type == 'recycle_space'">
              <el-input id="input_schedule_recycle_scope" v-model.number="form.recycleScope"  maxlength="12" type="number" placeholder="N 个月之前"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务超时时间" prop="timeout">
              <el-input id="input_schedule_timeout" v-model.number="form.timeout"  maxlength="12"   placeholder="单位：ms"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="快速启动" prop="quickStart" v-if="form.type && form.type !== 'recycle_space'">
              <el-select
                id="select_schedule_is_quick_start"
                v-model="form.quickStart"
                size="small"
                style="width:100%">
                <el-option
                  v-for="item in booleanType"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据校验级别" prop="dataCheckLevel" v-if="form.type && form.type !== 'recycle_space'">
              <el-select
                id="select_schedule_data_check_level"
                v-model="form.dataCheckLevel"
                size="small"
                style="width:100%">
                <el-option
                  v-for="item in checkLevels"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="是否回收空间" prop="isRecycleSpace" v-if="form.type && form.type !== 'recycle_space'  && form.type !== 'copy_file'">
              <el-select
                id="select_schedule_is_recycle_space"
                v-model="form.isRecycleSpace"
                size="small"
                style="width:100%">
                <el-option
                  v-for="item in booleanType"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="优先region" prop="preferredRegion">
              <el-select
                id="select_schedule_region"
                v-model="form.preferredRegion"
                size="small"
                placeholder="请选择region"
                clearable filterable
                style="width:100%"
                @change="onRegionChange">
                <el-option
                  v-for="item in regionList"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先zone" prop="preferredZone">
              <el-select
                id="select_schedule_zone"
                v-model="form.preferredZone"
                size="small"
                :no-data-text="form.preferredRegion ? '无数据' : '请先选择region'"
                placeholder="请选择zone"
                clearable filterable
                style="width:100%">
                <el-option
                  v-for="item in zoneList"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="查询条件" prop="condition" v-if="form.type && form.type !== 'recycle_space'">
          <el-input
            id="input_schedule_condition"
            v-model="form.condition"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
            :placeholder="conditionPlaceholder"
            >
          </el-input>
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input
            id="input_schedule_description"
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="请输入任务描述"
            >
          </el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer" style="border:1px soild red">
        <el-button id="btn_schedule_cancel" @click="scheduleDialogVisible = false" size="mini">取 消</el-button>
        <el-button id="btn_schedule_save" type="primary" :loading="saveButtonLoading" @click="submitForm" size="mini">保 存</el-button>
      </span>
    </el-dialog>

    <!-- 调度任务详情弹框 -->
    <schedule-detail-dialog ref="detailDialog" :taskDetail="taskDetail"></schedule-detail-dialog>
  </div>
</template>
<script>
import CronPicker from '@/components/common/CronPicker/index.vue'
import ScheduleDetailDialog from '@/components/ScheduleDetailDialog/index.vue'
import {queryScheduleList, createSchedule, updateSchedule, deleteSchedule, queryScheduleDetail} from '@/api/schedule'
import {querySiteList, querySiteStrategy} from '@/api/site'
import {queryZoneList, queryRegionList} from '@/api/service'
import {queryWorkspaceList, queryWorkspaceBasic} from '@/api/workspace'
import {TASK_TYPES, FILE_SCOPE_TYPES, X_RECORD_COUNT} from '@/utils/common-define'
export default {
  components: {
    CronPicker,
    ScheduleDetailDialog
  },
  data(){
    let validateCondition = (rule, value, callback) => {
      if (value != '') {
        if (!this.$util.isJsonStr(value)) {
          callback(new Error('查询条件格式不正确'));
        } else {
          callback()
        }
      } else {
        callback()
      }
    }
    return{
      interval: 'minute', // 调度周期
      showCronPicker: true,
      // 操作类型 create or update
      operation: 'create',
      pagination: {
        current: 1, //当前页
        size: 10, //每页大小
        total: 0, //总数据条数
      },
      oldSchedule: {},
      selectTypeForbidden: false,
      scheduleDialogVisible: false,
      scheduleDialogLoading: false,
      saveButtonLoading: false,
      tableLoading: false,
      filter: {},
      searchParams: {
        name: '',
        workspace: ''
      },
      form: {
        name: '',
        condition: '',
        enable: true,
        cron: '',
        sourceSite: '',
        targetSite: '',
        maxStayTime: '',
        quickStart: false,
        dataCheckLevel: 'week',
        isRecycleSpace: false,
        recycle_scope: '',
        timeout: '',
        scope: '',
        preferredRegion: '',
        preferredZone: '',
        description: ''
      },
      taskDetail: {
        condition: ''
      },
      booleanType: [
        { value: true, label: '是' },
        { value: false, label: '否' },
      ],
      checkLevels: [
        { value: 'strict', label: '强校验'},
        { value: 'week', label: '弱校验'}
      ],
      rules: {
        name: [
          { required: true, message: '请输入任务名称', trigger: 'change' },
          { min: 2, max: 30, message: '长度在 2 到 30 个字符', trigger: 'change' }
        ],
        type: [
          { required: true, message: '请选择任务类型', trigger: 'change' },
        ],
        workspace: [
          { required: true, message: '请选择工作区', trigger: 'change' },
        ],
        cron: [
          { required: true, message: '请输入cron表达式', trigger: 'change' },
        ],
        sourceSite: [
          { required: true, message: '请选择源站点', trigger: 'none' },
        ],
        targetSite: [
          { required: true, message: '请选择目标站点', trigger: 'none' },
        ],
        recycleScope: [
          { required: true, message: '请填写空间回收范围', trigger: 'none' },
        ],
        maxStayTime: [
          { required: true, message: '请输入文件最大停留时间', trigger: 'change' },
        ],
        timeout: [
          { required: true, message: '请输入任务超时时间', trigger: 'change' },
        ],
        scope: [
          { required: true, message: '请选择文件范围', trigger: 'change' },
        ],
        condition: [
          {validator: validateCondition, trigger: 'blur'}
        ]
      },
      selectedSchedule: '',
      siteStrategy: '',
      allSite: [],
      workspaceList: [],
      workspaceSiteList: [],
      sourceSiteList: [],
      targetSiteList: [],
      regionList: [],
      zoneList: [],
      fileScopeTypes: FILE_SCOPE_TYPES,
      taskTypes: TASK_TYPES,
      tableData: [],
      conditionPlaceholder: '请输入文件查询条件（json格式）,为空表示匹配所有文件\n如{"id":"test"}'
    }
  },
  computed:{

  },
  methods:{
    // 初始化
    init(){
      this.queryTableData()
      this.queryWorkspaces()
    },
    // 初始化任务添加、编辑对话框数据
    initScheduleDialogData() {
      if( !this.allSite || this.allSite.length <= 0) {
        this.queryAllSite()
      }
      if (!this.siteStrategy) {
        this.qeurySiteStrategy()
      }
      if (!this.regionList || this.regionList.length <= 0) {
        this.queryRegions()
      }
    },
    // 查询表格数据
    queryTableData() {
      this.tableLoading = true
      queryScheduleList(this.pagination.current, this.pagination.size, this.filter).then(res => {
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
    },
    // 查询工作区列表
    queryWorkspaces() {
      queryWorkspaceList(1, -1).then(res => {
        this.workspaceList = []
        let workspaces = res.data
        if (workspaces && workspaces.length > 0) {
          for (let workspace of workspaces) {
            this.workspaceList.push(workspace.name)
          }
        }
      })
    },
    // 查询站点列表
    queryAllSite() {
      querySiteList().then(res => {
        this.allSite = res.data
      })
    },
    // 查询站点网络模型
    qeurySiteStrategy() {
      querySiteStrategy().then(res => {
        this.siteStrategy = res.data
      })
    },
    // 查询region列表
    queryRegions() {
      queryRegionList().then(res => {
        this.regionList = res.data
      })
    },
    // 点击新增按钮
    hanldeAddBtnClick() {
      this.initScheduleDialogData()
      this.operation = 'create'
      this.saveButtonLoading = false
      this.selectTypeForbidden = false
      this.clearForm()
      this.resetSiteList()
      this.showCronPicker = true
      if (this.$refs['cronPicker']) {
        this.$refs['cronPicker'].reset()
      }
      setTimeout(()=>{this.scheduleDialogVisible = true})
    },
    // 点击编辑按钮
    handleEditBtnClick(row) {
      this.initScheduleDialogData()
      this.operation = 'update'
      this.saveButtonLoading = false
      this.selectTypeForbidden = true
      this.clearForm()
      this.selectedSchedule = {...row}
      this.showCronPicker = false
      if (this.$refs['cronPicker']) {
        this.$refs['cronPicker'].reset()
      }
      this.scheduleDialogVisible = true
      this.scheduleDialogLoading = true
      queryScheduleDetail(row.schedule_id).then( res => {
        let detail = res.data
        this.form = {
          name: detail.name,
          condition: '',
          enable: detail.enable,
          cron: detail.cron,
          workspace: detail.workspace,
          preferredRegion: detail.preferred_region,
          preferredZone: detail.preferred_zone,
          timeout: Number(detail.content.max_exec_time),
          type: detail.type,
          description: detail.description,
          sourceSite: detail.content.source_site,
          targetSite: detail.content.target_site,
          quickStart: false,
          dataCheckLevel: 'week',
          isRecycleSpace: false,
          recycleScope: ''
        }
        if (this.form.type !== 'recycle_space') {
          this.form['condition'] = this.$util.toPrettyJson(detail.content.extra_condition),
          this.form['scope'] = detail.content.scope,
          this.form['maxStayTime'] = Number(detail.content.max_stay_time.slice(0, detail.content.max_stay_time.length - 1))
          this.form['quickStart'] = detail.content.quick_start
          this.form['dataCheckLevel'] = detail.content.data_check_level
          if (this.form.type !== 'copy_file') {
            this.form['isRecycleSpace'] = detail.content.is_recycle_space
          }
          if(this.form.type === 'clean_file') {
            this.form['sourceSite'] = detail.content.site
          }
        }
        else {
          this.form['recycleScope'] = Number(detail.content.recycle_scope.slice(0, detail.content.recycle_scope.length - 13))
          this.form['sourceSite'] = detail.content.target_site
        }
        this.oldSchedule = {...this.form}
        this.onWorkspaceChange(detail.workspace, false)
        this.onRegionChange(detail.preferred_region, false)

      }).finally(() => {
        this.scheduleDialogLoading = false
      })
    },
    // 点击查看按钮
    handleShowBtnClick(row) {
      this.taskDetail = {}
      queryScheduleDetail(row.schedule_id).then(res => {
        this.taskDetail = res.data
      })
      this.$refs['detailDialog'].show()
    },
    // 点击查看运行记录按钮
    handleShowTasksBtnClick(row) {
      this.$router.push("/schedule/tasks/"+row.schedule_id)
    },
    // 点击删除按钮
    handleDeleteBtnClick(row) {
      this.$confirm(`您确认删除任务【${row.name}】吗`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_schedule_confirmDelete',
        type: 'warning'
      }).then(() => {
        deleteSchedule(row.schedule_id).then(res => {
          this.$message.success(`【${row.name}】删除成功`)
          this.queryTableData()
        })

      }).catch(() => {
      });
    },
    // 校验单个表单项
    validateField(field, changeValue) {
      if (this.$refs['form'] && changeValue) {
        this.$refs['form'].validateField(field)
      }
    },
    // 选择工作区
    onWorkspaceChange(workspace, isReset=true) {
      if (isReset) {
        this.resetSiteList()
      }
      if (!workspace) {
        return
      }
      queryWorkspaceBasic(workspace).then(res => {
        let basicInfo = JSON.parse(res.headers['workspace'])
        let siteList = basicInfo['data_locations']
        if (siteList && siteList.length > 0) {
          this.workspaceSiteList = []
          for (let site of siteList) {
            this.workspaceSiteList.push(site['site_name'])
          }
          this.sourceSiteList = [...this.workspaceSiteList]
          this.targetSiteList = [...this.workspaceSiteList]
          this.onSourceSiteChange(this.form.sourceSite)
          if (this.form.targetSite) {
            this.onTargetSiteChange(this.form.targetSite)
          }
        }
      })
    },
    // 任务类型改变
    onTaskTypeChange(value) {
      this.onWorkspaceChange(this.form.workspace, true)
    },
    // region改变
    onRegionChange(region, resetZone=true) {
      if (resetZone) {
        this.zoneList = []
        this.form.preferredZone = ''
      }
      if (region) {
        queryZoneList(region).then(res => {
          this.zoneList = res.data
        })
      }
    },
    // 执行搜索
    doSearch() {
      let filter = {}
      if (this.searchParams.name) {
        filter['name'] = {
          $regex: this.$util.escapeStr(this.searchParams.name)
        }
      }
      if (this.searchParams.workspace) {
        filter['workspace'] = this.$util.escapeStr(this.searchParams.workspace)
      }
      this.filter = {...filter}
      this.pagination.current = 1
      this.queryTableData()
    },
    // 重置搜索
    resetSearch() {
      this.searchParams = {}
      this.filter = {}
      this.pagination.current = 1
      this.queryTableData()
    },

    // cron改变
    onCronChange(res) {
      this.interval = res.interval
      this.form.cron = res.cron
      this.$refs['form'].validateField('cron')
    },
    // 改变任务启用状态
    handleChangeState(e, row) {
      let action = row.enable ? '禁用' : '启用'
      this.$confirm(`您确认${action}该任务吗`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_schedule_confirmChangeState',
        type: 'warning'
      }).then(() => {
        let formData = {
          enable: !row.enable
        }
        updateSchedule(row.schedule_id, formData).then(res => {
          row.enable = !row.enable
          this.$message.success(`任务${action}成功`)
        })
      }).catch(() => {

      });
    },
    // 提交表单
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          let form = this.form
          let content = {
            max_exec_time: form.timeout,
            source_site: form.sourceSite,
            target_site: form.targetSite,
          }
          let type = form.type
          if (type === 'recycle_space') {
            content['recycle_scope'] = form.recycleScope + ' month before'
            content['target_site'] = form.sourceSite
          }
          else {
            content['data_check_level'] = form.dataCheckLevel
            content['max_stay_time'] = form.maxStayTime + 'd'
            content['quick_start'] = form.quickStart
            if (form.condition === '') {
              form.condition = '{}'
            }
            content['extra_condition'] = JSON.parse(form.condition)
            if (type === 'clean_file') {
              content['site'] = form.sourceSite
            }
            if (type !== 'copy_file') {
              content['is_recycle_space'] = form.isRecycleSpace
            }
          }
          let formData = {
            name: form.name,
            type: form.type,
            workspace: form.workspace,
            scopeType: form.scope,
            cron: form.cron,
            preferred_region: form.preferredRegion,
            preferred_zone: form.preferredZone,
            content: content,
            description: form.description
          }
          if (this.operation === 'create') {
            this.saveButtonLoading = true
            createSchedule(formData).then(res => {
              this.scheduleDialogVisible = false
              this.$message.success("任务创建成功")
              this.queryTableData()
            }).catch( () => {
              this.saveButtonLoading = false
            })
          }else if (this.operation === 'update') {
            let isChange = !this.$util.equalsObj( this.oldSchedule, form)
            if (!isChange) {
              this.scheduleDialogVisible = false
              return
            }
            this.saveButtonLoading = true
            updateSchedule(this.selectedSchedule.schedule_id, formData).then(res => {
              this.scheduleDialogVisible = false
              this.$message.success("任务修改成功")
              this.queryTableData()
            }).catch( () => {
              this.saveButtonLoading = false
            })
          }
        }
      })
    },
    // 重置表单
    clearForm() {
      if (this.$refs['form']) {
        this.$refs['form'].clearValidate()
      }
      this.form = {
        name: '',
        condition: '',
        enable: true,
        cron: '',
        sourceSite: '',
        targetSite: '',
        quickStart: false,
        dataCheckLevel: 'week',
        isRecycleSpace: false,
        recycleScope: '',
        maxStayTime: '',
        timeout: '',
        preferredRegion: '',
        preferredZone: '',
        scope: '',
        description: ''
      }
      this.zoneList = []
    },
    // 对话框关闭
    onDialogClose() {
      setTimeout(()=>{
        this.clearForm()
      }, 500)
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
    // 处理多选
    handleSelectionChange(val) {
      this.multipleSelection = val;
    },

    // 源站点发生变化
    onSourceSiteChange(selectedSite) {
      this.updateSiteSelector(selectedSite, this.targetSiteList, this.form.targetSite, 'taretSite')
      this.validateField("sourceSite", selectedSite)
    },
    // 目的站点发生变化
    onTargetSiteChange(selectedSite) {
      this.updateSiteSelector(selectedSite, this.sourceSiteList, this.form.sourceSite, 'sourceSite')
      this.validateField("targetSite", selectedSite)
    },
    getIndex(index) {
      return (this.pagination.current-1) * this.pagination.size + index + 1
    },
    // 重置站点列表
    resetSiteList() {
      this.workspaceSiteList = []
      this.sourceSiteList = []
      this.targetSiteList = []
      this.form.sourceSite = ''
      this.form.targetSite = ''
    },
    updateSiteSelector(selectedSite, otherSiteList, otherSite, otherSiteType) {
      if (!this.form.type || this.form.type == 'clean_file') {
        return
      }
      this.recoverySiteList(otherSiteList)
      if (!selectedSite) {
        return
      }
      if (this.siteStrategy == 'star') {
        if (this.isRootSite(selectedSite)) {
          this.removeRootSite(otherSiteList)
          if (this.isRootSite(otherSite)) {
            this.clearSite(otherSiteType)
          }
        }else {
          this.setRootSite(otherSiteList)
          if (!this.isRootSite(otherSite)) {
            this.clearSite(otherSiteType)
          }
        }
      }else {
        this.removeSite(otherSiteList, selectedSite)
      }
    },
    isRootSite(siteName) {
      for (let siteInfo of this.allSite) {
        if (siteInfo.name == siteName) {
          return siteInfo['is_root_site']
        }
      }
    },
    getRootSite() {
      for (let siteInfo of this.allSite) {
        if (siteInfo.rootSite) {
          return siteInfo.name
        }
      }
    },
    setRootSite(siteList) {
      siteList.splice(0)
      siteList.push(this.getRootSite())
    },
    removeRootSite(siteList) {
      siteList.forEach((item, index, arr) =>{
        if (this.isRootSite(item)) {
          arr.splice(index, 1)
        }
      })
    },
    removeSite(siteList, siteName) {
      siteList.forEach((item, index, arr) =>{
        if (item == siteName) {
          arr.splice(index, 1)
        }
      })
    },
    recoverySiteList(siteList) {
      siteList.splice(0)
      siteList.push(...this.workspaceSiteList)
    },
    clearSite(siteType) {
      if (siteType === 'sourceSite') {
        this.form.sourceSite = ''
      }else {
        this.form.targetSite = ''
      }
    },
  },
  created(){
  },
  activated() {
    this.init()
  },
  watch: {
    form: {
      handler(val, old) {
        if (val.maxStayTime) {
          this.form.maxStayTime = Number((val.maxStayTime+'').replace(/[^\d.]/g,''))
        }
        if (val.timeout) {
          this.form.timeout = Number((val.timeout+'').replace(/[^\d.]/g,''))
        }
      },
      deep: true
    }
  },
  computed: {

  }

}
</script>
<style  scoped>
.search-box {
  width: 60%;
  float: right;
  margin-bottom: 10px;
}
.no-select {
  user-select: none;
}
.app-container >>> .el-switch.is-disabled{
  opacity: 1;
}
.app-container .el-switch.is-disabled >>>  .el-switch__core{
  cursor: pointer;
}
.edit_dialog >>> .el-dialog__body {
  padding: 0px 15px 0px 5px;
}
.pointer-cursor {
  cursor: pointer;
}
</style>
