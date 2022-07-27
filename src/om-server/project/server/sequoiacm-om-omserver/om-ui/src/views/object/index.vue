<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="8">
          <el-select 
            id="query_file_select_bucket"
            placeholder="请选择存储桶"
            v-model="currentBucket" 
            size="small"
            style="width:100%"
            filterable
            @change="onBucketChange()">
            <el-option
              v-for="item in bucketList"
              :key="item"
              :label="item"
              :value="item">
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="10">
          <el-input 
            id="input_object_search_param"
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
        <el-col :span="3" >
          <el-button id="btn_object_doSearch" @click="doSearch" type="primary" size="small" icon="el-icon-search" style="width:100%" :disabled="currentBucket===''">搜索</el-button>
        </el-col>
        <el-col :span="3" >
          <el-button id="btn_object_resetSearch" @click="resetSearch" size="small" icon="el-icon-circle-close" style="width:100%" :disabled="currentBucket===''">重置</el-button>
        </el-col>
      </el-row>
    </div>
    <!-- 表格部分 -->
    <el-button id="btn_object_batch_deletion" type="danger" size="small" icon="el-icon-delete" @click="handleDeleteBtnClick(null)" :disabled="fileIdList.length==0" style="margin-bottom:10px">批量删除</el-button>
    <el-button id="btn_object_showCreateDialog" type="primary" size="small" icon="el-icon-upload2"  @click="handleUploadBtnClick" style="margin-bottom:10px" :disabled="currentBucket===''">上传文件</el-button>
    <el-table
        border
        :data="tableData"
        @sort-change="sortChange"
        @selection-change="selectionChange"
        row-key="object_id"
        v-loading="tableLoading"      
        style="width: 100%">
        <el-table-column 
          type="selection"
          width="50">
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
          width="190"
          label="操作">
          <template slot-scope="scope">
            <el-button-group>
              <el-button id="btn_object_showDetailDialog" size="mini" @click="handleShowBtnClick(scope.row)">查看</el-button>
              <el-button id="btn_object_showDownloadDialog" size="mini" @click="handleDownloadBtnClick(scope.row)">下载</el-button>
              <el-button id="btn_object_delete" type="danger" @click="handleDeleteBtnClick(scope.row)" size="mini">删除</el-button>
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
    <file-upload-dialog ref="fileUploadDialog" :bucketList="bucketList" @refreshTable="queryTableData"></file-upload-dialog>
    <!-- 文件详情弹框 -->
    <file-detail-dialog ref="fileDetailDialog" :multiVofCurFile="multiVofCurFile" :workspace="currentBucketDetail.workspace" :isNameMaster=true></file-detail-dialog>
    <!-- 文件下载弹框 -->
    <file-download-dialog ref="fileDownloadDialog" :multiVofCurFile="multiVofCurFile" :workspace="currentBucketDetail.workspace"></file-download-dialog>
    <!-- 展示文件属性弹框 -->
    <file-properties-dialog ref="filePropertiesDialog" :isWorkspaceView="false"></file-properties-dialog>
  </div>
</template>
<script>
import {deleteFiles} from '@/api/file'
import {queryUserRelatedBucket, queryBucketDetail, queryFileInBucket} from '@/api/bucket'
import {X_RECORD_COUNT} from '@/utils/common-define'
import FileDetailDialog from '../file/components/FileDetailDialog.vue'
import FileUploadDialog from './components/FileUploadDialog.vue'
import FileDownloadDialog from '../file/components/FileDownloadDialog.vue'
import FilePropertiesDialog from '../file/components/FilePropertiesDialog.vue' 
import {Loading } from 'element-ui';
export default { 
  components: {
    FileDetailDialog,
    FileUploadDialog,
    FileDownloadDialog,
    FilePropertiesDialog
  },
  data(){
    return {
      pagination:{ 
        current: 1, //当前页
        size: 12, //每页大小
        total: 0, //总数据条数
      },
      filter: {},
      fileSearchTypes: [
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
      bucketList: [],
      currentBucket: '',
      currentBucketDetail: {},
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
      this.currentFileSearchType = this.fileSearchTypes[0]
      this.currentFileSearchTypeStr = this.currentFileSearchType.value
      // 加载用户关联的桶列表
      queryUserRelatedBucket().then(res => {
        let bucketList = res.data
        // 默认选中第一个桶，并加载该桶下的文件列表
        if (bucketList && bucketList.length > 0) {
          this.bucketList = bucketList
          this.currentBucket = this.bucketList[0]
          this.onBucketChange()
        }
      })
    },
    // 切换桶
    onBucketChange() {
      queryBucketDetail(this.currentBucket).then(res => {
        this.currentBucketDetail = JSON.parse(res.headers['bucket'])
        this.resetSearch()
      })
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
      this.multiVofCurFile = []
      this.multiVofCurFile.push(row)
      this.$refs['fileDetailDialog'].show()
    },
    // 下载文件
    handleDownloadBtnClick(row) {
      this.multiVofCurFile = []
      this.multiVofCurFile.push(row)
      this.$refs['fileDownloadDialog'].show()
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
        deleteFiles(this.currentBucketDetail.workspace, _fileIdList).then(res => {
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
      if (!this.currentBucket) {
        return
      }
      this.tableLoading = true
      // 添加排序参数
      let orderby = {}
      orderby[prop] = order === 'descending' ? -1 : 1 
      queryFileInBucket(this.currentBucket, this.filter, orderby, this.pagination.current, this.pagination.size).then(res => {
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
    // 执行搜索
    doSearch() {
      let filter = {}
      if (this.searchParam) {
        if (this.currentFileSearchTypeStr === 'search_by_id') {
          filter['id'] = {
            $regex: this.$util.escapeStr(this.searchParam)
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
      this.searchParam = ''
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
.input-with-select >>> .el-select {
  width: 100px;
}
.input-with-select >>> .el-input-group__prepend {
  background-color: #fff;
} 
</style>