<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="9">
          <el-select 
            id="query_file_select_workspace"
            placeholder="请选择工作区"
            v-model="currentWorkspace" 
            size="small"
            style="width:100%"
            filterable
            @change="onWorkspaceChange()">
            <el-option
              v-for="item in workspaceList"
              :key="item"
              :label="item"
              :value="item">
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="9">
          <el-input
            id="input_file_search_id_name"
            placeholder="请输入文件ID或文件名"
            v-model="searchParams.id_name"
            size="small"
            clearable
            @keyup.enter.native="doSearch">
          </el-input>
        </el-col>
        <el-col :span="3" >
          <el-button id="btn_file_doSearch" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%" :disabled="currentWorkspace===''">搜索</el-button>
        </el-col>
        <el-col :span="3" >
          <el-button id="btn_file_resetSearch" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%" :disabled="currentWorkspace===''">重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button id="btn_file_batch_deletion" type="danger" size="small" icon="el-icon-delete" @click="handleDeleteBtnClick(null)" :disabled="fileIdList.length==0" style="margin-bottom:10px">批量删除</el-button>
    <el-button id="btn_file_showCreateDialog" type="primary" size="small" icon="el-icon-upload2"  @click="handleUploadBtnClick" style="margin-bottom:10px" :disabled="currentWorkspace===''">上传文件</el-button>
    <el-table
        border
        :data="tableData"
        @sort-change="sortChange"
        @selection-change="selectionChange"
        row-key="file_id"
        v-loading="tableLoading"      
        style="width: 100%">
        <el-table-column 
          type="selection"
          width="50">
        </el-table-column>
        <el-table-column
          prop="id"
          label="文件ID"
          sortable="custom"
          width="230">
        </el-table-column>
        <el-table-column
          prop="name"
          label="文件名"
          show-overflow-tooltip
          sortable="custom"
          width="230">
        </el-table-column>
        <el-table-column
          prop="size"
          show-overflow-tooltip
          label="文件大小">
          <template slot-scope="scope">
            {{scope.row.size|convertFileSize}}
          </template>
        </el-table-column>
        <el-table-column
          prop="user"
          show-overflow-tooltip
          sortable="custom"
          label="创建人">
        </el-table-column>
        <el-table-column
          prop="create_time"
          show-overflow-tooltip
          sortable="custom"
          label="创建时间">
          <template slot-scope="scope">
            {{scope.row.create_time|parseTime}}
          </template>
        </el-table-column>
        <el-table-column
          prop="update_time"
          show-overflow-tooltip
          label="更新时间">
          <template slot-scope="scope">
            {{scope.row.update_time|parseTime}}
          </template>
        </el-table-column>
        <el-table-column
          width="245"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_file_showDetailDialog" size="mini" @click="handleShowBtnClick(scope.row)">查看</el-button>
              <el-button id="btn_file_showEditDialog" size="mini" @click="handleEditBtnClick(scope.row)">编辑</el-button>
              <el-button id="btn_file_showDownloadDialog" size="mini" @click="handleDownloadBtnClick(scope.row)">下载</el-button>
              <el-button id="btn_file_delete" type="danger" @click="handleDeleteBtnClick(scope.row)" size="mini">删除</el-button>
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

    <!-- 文件上传弹框 -->
    <file-upload-dialog ref="fileUploadDialog" :workspaceList="workspaceList" :workspace="currentWorkspace" @refreshTable="queryTableData"></file-upload-dialog>
    <!-- 文件详情弹框 -->
    <file-detail-dialog ref="fileDetailDialog" :multiVofCurFile="multiVofCurFile" :workspace="currentWorkspace"></file-detail-dialog>
    <!-- 文件下载弹框 -->
    <file-download-dialog ref="fileDownloadDialog" :multiVofCurFile="multiVofCurFile" :workspace="currentWorkspace"></file-download-dialog>
    <!-- 文件更新弹框 -->
    <file-edit-dialog ref="fileEditDialog" :workspaceDetail="currentWorkspaceDetail" :fileId="currentFileId" @refreshTable="queryTableData" @update="changeUpdateStatus"></file-edit-dialog>
  </div>
</template>
<script>
import {queryWorkspaceList, queryWorkspaceBasic} from '@/api/workspace'
import {queryFileList, deleteFiles} from '@/api/file'
import {FILE_SCOPE_VAL_CURRENT, FILE_SCOPE_VAL_ALL, X_RECORD_COUNT} from '@/utils/common-define'
import FileDetailDialog from './components/FileDetailDialog.vue'
import FileUploadDialog from './components/FileUploadDialog.vue'
import FileEditDialog from './components/FileEditDialog.vue'
import FileDownloadDialog from './components/FileDownloadDialog.vue'
import {Loading } from 'element-ui';
export default { 
  components: {
    FileDetailDialog,
    FileUploadDialog,
    FileEditDialog,
    FileDownloadDialog
  },
  data(){
    return {
      pagination:{ 
        current: 1, //当前页
        size: 12, //每页大小
        total: 0, //总数据条数
      },
      filter: {},
      searchParams: {},
      tableLoading: false,
      tableData: [],
      workspaceList: [],
      currentWorkspace: '',
      currentWorkspaceDetail: {},
      currentFileId: '',
      fileContentUpdate: {
        status: false,
        fileId: ''
      },
      fileIdList: [],
      multiVofCurFile: [],
    }
  },
  methods:{
    // 初始化
    init(){
      // 加载用户关联的工作区列表
      queryWorkspaceList(1, -1, null, true).then(res => {
        let workspaces = res.data
        this.workspaceList = [];
        // 默认选中第一个工作区，并加载该工作区的文件列表
        if (workspaces && workspaces.length > 0) {
          for (let ws of workspaces) {
            this.workspaceList.push(ws.name)
          }
          this.currentWorkspace = this.workspaceList[0]
          this.onWorkspaceChange()
        }
      })
    },
    // 切换工作区
    onWorkspaceChange() {
      this.resetSearch()
    },
    // 排序项发生变化 
    sortChange(column) {
      const { order = '', prop = ''} = column;
      this.queryTableData(prop, order)
    },
    // 文件列表选择项发生变化
    selectionChange(fileIdList) {
      this.fileIdList=[];
      fileIdList.forEach(item => {
        this.fileIdList.push(item.id);
      });
    },
    // 上传文件
    handleUploadBtnClick() {
      this.$refs['fileUploadDialog'].show()
    },
    // 查看文件详情
    handleShowBtnClick(row) {
      var queryCondition = {}
      queryCondition['id'] = row.id
      queryFileList(this.currentWorkspace, FILE_SCOPE_VAL_ALL, queryCondition, null, 1, -1).then(res => {
        this.sortByVersion(res.data)
        this.multiVofCurFile = res.data;
        this.$refs['fileDetailDialog'].show()
      })
    },
    // 下载文件
    handleDownloadBtnClick(row) {
      var queryCondition = {}
      queryCondition['id'] = row.id
      queryFileList(this.currentWorkspace, FILE_SCOPE_VAL_ALL, queryCondition, null, 1, -1).then(res => {
       this.sortByVersion(res.data)
        this.multiVofCurFile = res.data;
        this.$refs['fileDownloadDialog'].show()
      })
    },
    // 按版本号排序文件列表
    sortByVersion(data) {
      data.sort(function(a, b) {
        if (b.major_version === a.major_version) {
          return b.minor_version - a.minor_version;
        }
        return b.major_version - a.major_version;
      })
    },
    // 更新文件内容
    handleEditBtnClick(row) {
      if (this.fileContentUpdate.status && this.fileContentUpdate.fileId !== row.id) {
        this.$message.error('当前正在更新文件，ID：'+this.fileContentUpdate.fileId)
        return
      } 
      queryWorkspaceBasic(this.currentWorkspace).then(res => {
        this.currentWorkspaceDetail = JSON.parse(res.headers['workspace'])
      })
      this.currentFileId = row.id
      this.$refs['fileEditDialog'].show()
    },
    // 子组件回调：监听文件更新状态
    changeUpdateStatus(data) {
      this.fileContentUpdate = data
    },
    // 删除文件
    handleDeleteBtnClick(row) {
      var _fileIdList = []
      var confirmMsg = ''
      if (row == null) {
        _fileIdList = this.fileIdList
        confirmMsg = '您确定删除批量文件吗'
      } else {
        _fileIdList = [row.id]
        confirmMsg = `您确定删除文件【${row.name}】吗`
      }

      this.$confirm(confirmMsg, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        confirmButtonClass: 'btn_files_confirmDelete',
        type: 'warning'
      }).then(() => {
        let loadingInstance = Loading.service({ fullscreen: true, text: "正在删除中..." })
        deleteFiles(this.currentWorkspace, _fileIdList).then(res => {
          this.$message.success(`删除成功`)
          this.queryTableData()
        }).finally(() => {
          loadingInstance.close()
        })
      }).catch(() => {
      });
    },
    // 查询文件列表（默认按id降序排序）
    queryTableData(prop = 'id', order = 'descending') {
      this.tableLoading = true
      // 添加排序参数
      let orderby = {}
      orderby[prop] = order === 'descending' ? -1 : 1 
      queryFileList(this.currentWorkspace, FILE_SCOPE_VAL_CURRENT, this.filter, orderby, this.pagination.current, this.pagination.size).then(res => {
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
    // 执行搜索
    doSearch() {
      let filter = {}
      if (this.searchParams.id_name) {
        let idFilter = {}
        let nameFilter = {}
        idFilter['id'] = {
          $regex: this.$util.escapeStr(this.searchParams.id_name)
        }
        nameFilter['name'] = {
          $regex: this.$util.escapeStr(this.searchParams.id_name)
        }
        filter['$or'] = [ idFilter, nameFilter ]
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
    }
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
</style>