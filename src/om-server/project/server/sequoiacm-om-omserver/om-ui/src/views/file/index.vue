<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="6">
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
            id="input_file_search_param"
            :placeholder="currentFileSearchType.tip"
            v-model="searchParam"
            class="input-with-select"
            size="small"
            @keyup.enter.native="doSearch">
            <i
              class="el-input__icon el-icon-question"
              slot="suffix"
              v-if="currentFileSearchTypeStr === 'search_by_json'"
              @click="handleQuestionIconClick">
            </i>
            <el-select
              id="search_type_select"
              v-model="currentFileSearchTypeStr"
              slot="prepend"
              size="small"
              @change="onSearchTypeChange()">
              <el-option
                v-for="item in fileSearchTypes"
                :key="item.value"
                :label="item.label"
                :value="item.value">
              </el-option>
            </el-select>
          </el-input>
        </el-col>
        <el-col :span="3">
          <tag-search-button ref="tagSearchButton" :workspace="currentWorkspace" @onTagConditionChange="saveTagCondition"></tag-search-button>
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
    <el-button id="btn_file_batch_download"  size="small" icon="el-icon-download" @click="handleDownloadBatchBtnClick" :disabled="fileIdList.length==0" style="margin-bottom:10px">批量下载</el-button>
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
              <el-button id="btn_file_showEditDialog" size="mini" @click="handleEditBtnClick(scope.row)">更新</el-button>
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
    <!-- 展示文件属性弹框 -->
    <file-properties-dialog ref="filePropertiesDialog"></file-properties-dialog>
  </div>
</template>
<script>
import {queryWorkspaceList, queryWorkspaceBasic} from '@/api/workspace'
import {queryFileDetail,getDownloadUrl} from '@/api/file'
import {queryFileList, deleteFiles} from '@/api/file'
import {FILE_SCOPE_VAL_CURRENT, FILE_SCOPE_VAL_ALL, X_RECORD_COUNT} from '@/utils/common-define'
import FileDetailDialog from './components/FileDetailDialog.vue'
import FileUploadDialog from './components/FileUploadDialog.vue'
import FileEditDialog from './components/FileEditDialog.vue'
import FileDownloadDialog from './components/FileDownloadDialog.vue'
import FilePropertiesDialog from './components/FilePropertiesDialog.vue'
import TagSearchButton from './components/TagSearchButton.vue'
import {Loading } from 'element-ui';
import {getToken} from '@/utils/auth'
export default {
  components: {
    FileDetailDialog,
    FileUploadDialog,
    FileEditDialog,
    FileDownloadDialog,
    FilePropertiesDialog,
    TagSearchButton
  },
  data(){
    return {
      pagination:{
        current: 1, //当前页
        size: 12, //每页大小
        total: 0, //总数据条数
      },
      filter: {},
      tagCondition: null,
      fileSearchTypes: [
        {
          value: 'search_by_id',
          label: '按 id',
          tip: '请输入文件 ID'
        },
        {
          value: 'search_by_name',
          label: '按文件名',
          tip: '请输入文件名'
        },
        {
          value: 'search_by_json',
          label: '按 json',
          tip: '请输入 json 串'
        }
      ],
      currentFileSearchType: {},
      currentFileSearchTypeStr: '',
      searchParam: '',
      tableLoading: false,
      tableData: [],
      workspaceList: [],
      selectedFiles:[],
      currentWorkspace: '',
      currentWorkspaceDetail: {},
      currentFileId: '',
      fileContentUpdate: {
        status: false,
        fileId: ''
      },
      fileIdList: [],
      multiVofCurFile: [],
      orderParam: { //记录排序参数
              prop: 'create_time',
              order: 'descending'
      }
    }
  },
  methods:{
    // 初始化
    init(){
      this.currentFileSearchType = this.fileSearchTypes[0]
      this.currentFileSearchTypeStr = this.currentFileSearchType.value
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
          if (this.$route.query.target  === 'checkJson') {
            this.currentFileSearchTypeStr = 'search_by_json'
            this.searchParam = this.$route.query.jsonStr
            this.doSearch()
          } else {
            this.onWorkspaceChange()
          }
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
      this.orderParam = column;
      this.queryTableData(prop, order)
    },
    // 文件列表选择项发生变化
    selectionChange(fileIdList) {
      this.selectedFiles = fileIdList
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
      queryFileList(this.currentWorkspace, FILE_SCOPE_VAL_ALL, null, queryCondition, null, 1, -1).then(res => {
        this.sortByVersion(res.data)
        this.multiVofCurFile = res.data;
        this.$refs['fileDetailDialog'].show()
      })
    },
    // 下载文件
    handleDownloadBtnClick(row) {
      var queryCondition = {}
      queryCondition['id'] = row.id
      queryFileList(this.currentWorkspace, FILE_SCOPE_VAL_ALL, null, queryCondition, null, 1, -1).then(res => {
       this.sortByVersion(res.data)
        this.multiVofCurFile = res.data;
        this.$refs['fileDownloadDialog'].show()
      })
    },
    // 批量下载文件
    handleDownloadBatchBtnClick() {
      if (this.selectedFiles.length <= 0) {
        return
      }
      this.$confirm(`即将下载${this.selectedFiles.length}个文件【${this.selectedFiles.map(item=>item.name).join(", ")}】，是否继续？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.selectedFiles.forEach(item => {
          queryFileDetail(this.currentWorkspace, item.id, item.major_version, item.minor_version).then(res => {
            let fileDetail = res.data
            let downloadURL = '/api/v1/files/id/' + fileDetail.id + '?action=download_file';
            downloadURL += '&workspace=' + this.currentWorkspace;
            downloadURL += '&site_name=' + fileDetail.sites[0].site_name;
            downloadURL += '&major_version=' + fileDetail.major_version;
            downloadURL += '&minor_version=' + fileDetail.minor_version;
            downloadURL += '&x-auth-token=' + getToken();
            const iframe = document.createElement("iframe")
            iframe.style.display = "none"
            iframe.src = downloadURL
            document.body.appendChild(iframe)
          })
        })
      }).catch(() => {
      });
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
        this.currentWorkspaceDetail = res.data
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
    // 查询文件列表（默认按create_time降序排序）
    queryTableData(prop = 'create_time', order = 'descending') {
      if (!this.currentWorkspace) {
        return
      }
      this.tableLoading = true
      // 添加排序参数
      let orderby = {}
      orderby[prop] = order === 'descending' ? -1 : 1
      queryFileList(this.currentWorkspace, FILE_SCOPE_VAL_CURRENT, this.tagCondition, this.filter, orderby, this.pagination.current, this.pagination.size).then(res => {
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
    // 切换搜索类型
    onSearchTypeChange() {
      this.currentFileSearchType = this.fileSearchTypes.find(
        item=>{
          return item.value === this.currentFileSearchTypeStr;
        }
      )
      this.resetSearch()
    },
    // 点击展示 json 查询示例
    handleQuestionIconClick() {
      this.$refs['filePropertiesDialog'].show()
    },
    // 
    saveTagCondition(tagCondition) {
      this.tagCondition = tagCondition
      this.queryTableData()
    },
    // 执行搜索
    doSearch() {
      let filter = {}
      if (this.searchParam) {
        if (this.currentFileSearchTypeStr === 'search_by_id') {
          if (this.searchParam.length >= 24) {
            filter['id'] = this.$util.escapeStr(this.searchParam)
          } else {
            filter['id'] = {
              $regex: this.$util.escapeStr(this.searchParam)
            }
          }
        }
        else if(this.currentFileSearchTypeStr === 'search_by_name') {
          filter['name'] = {
            $regex: this.$util.escapeStr(this.searchParam)
          }
        }
        else {
          if (!this.$util.isJsonStr(this.searchParam)) {
            this.$message.error(`请检查 json 格式是否正确`)
            return
          }
          filter = JSON.parse(this.searchParam)
        }
      }
      this.pagination.current = 1
      this.filter = {...filter}
      this.queryTableData()
    },
    // 重置搜索
    resetSearch() {
      // 重置标签检索按钮
      this.$refs['tagSearchButton'].reInit()
      this.tagCondition = null

      this.searchParam = ''
      this.filter = {}
      this.pagination.current = 1
      this.queryTableData()
      this.orderParam = {prop: 'create_time',order: 'descending'}
    },
    // 当前页变化时
    handleCurrentChange(currentPage) {
      this.pagination.current = currentPage
      this.queryTableData(this.orderParam.prop,this.orderParam.order)
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
.input-with-select >>> .el-select {
  width: 100px;
}
.input-with-select >>> .el-input-group__prepend {
  background-color: #fff;
}
</style>
