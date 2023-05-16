<template>
  <div class="app-container">
    <!-- 搜索部分 -->
    <div class="search-box">
      <el-row :gutter="2">
        <el-col :span="7">
          <el-select
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
        <el-col :span="3">
          <el-select
            v-model="currentType"
            size="small"
            style="width:100%"
            filterable
            @change="onTypeChange()">
            <el-option
              v-for="item in tagTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-input
            placeholder="请输入标签名"
            v-model="tagMatcher"
            class="input-with-select"
            size="small"
            v-if="currentType==='tags'"
            @keyup.enter.native="doSearch">
            <template #suffix>
              <el-tooltip>
                <div slot="content" class="tooltip">
                  默认为精准匹配，支持填写通配符<br/><br/> * 表示匹配 0-n 个字符<br/> ? 表示匹配一个字符
                </div>
                <i class="el-input__icon el-icon-question"></i>
              </el-tooltip>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-input
            placeholder="请输入 key"
            v-model="keyMatcher"
            class="input-with-select"
            size="small"
            v-if="currentType==='custom_tag'"
            @keyup.enter.native="doSearch">
            <template #suffix>
              <el-tooltip>
                <div slot="content" class="tooltip">
                  默认为精准匹配，支持填写通配符<br/><br/> * 表示匹配 0-n 个字符<br/> ? 表示匹配一个字符
                </div>
                <i class="el-input__icon el-icon-question"></i>
              </el-tooltip>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-input
            id="input_file_search_param"
            placeholder="请输入 value"
            v-model="valueMatcher"
            class="input-with-select"
            size="small"
            v-if="currentType==='custom_tag'"
            @keyup.enter.native="doSearch">
            <template #suffix>
              <el-tooltip>
                <div slot="content" class="tooltip">
                  默认为精准匹配，支持填写通配符<br/><br/> * 表示匹配 0-n 个字符<br/> ? 表示匹配一个字符
                </div>
                <i class="el-input__icon el-icon-question"></i>
              </el-tooltip>
            </template>
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
    <el-table
        border
        :data="tableData"
        row-key="id"
        v-loading="tableLoading"
        style="width: 100%">
        <el-table-column
          prop="id"
          label="标签ID"
          width="230">
        </el-table-column>
        <el-table-column
          prop="type"
          label="标签类型"
          width="230">
        </el-table-column>
        <el-table-column
          prop="tagContent"
          show-overflow-tooltip
          label="标签内容">
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
  </div>
</template>
<script>
import {queryWorkspaceList} from '@/api/workspace'
import {listTag} from '@/api/tag'
import {X_RECORD_COUNT, TAG_TYPES, TAGS, CUSTOM_TAG} from '@/utils/common-define'
import {Loading } from 'element-ui';
export default {
  data(){
    return {
      pagination:{
        current: 1, //当前页
        size: 12, //每页大小
        total: 0, //总数据条数
      },
      tagTypes: TAG_TYPES,
      currentType: 'tags',
      tagMatcher: '',
      keyMatcher: '',
      valueMatcher: '',
      filter: {},
      workspaceList: [],
      currentWorkspace: '',
      tableLoading: false,
      tableData: []
    }
  },
  methods:{
    // 初始化
    init() {
      // 加载用户关联的工作区列表
      queryWorkspaceList(1, -1, null, true).then(res => {
        let workspaces = res.data
        this.workspaceList = [];
        // 默认选中第一个工作区，并加载该工作区的标签列表
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
    onTypeChange() {
      this.resetSearch()
    },
    // 查询标签列表
    queryTableData() {
      if (!this.currentWorkspace) {
        return
      }
      this.tableLoading = true
      listTag(this.currentWorkspace, this.currentType, this.filter, this.pagination.current, this.pagination.size).then(res => {
        let total = Number(res.headers[X_RECORD_COUNT])
        if (res.data.length == 0 && total > 0) {
          this.pagination.current--
          if (this.pagination.current > 0){
            this.queryTableData()
          }
        }else {
          this.tableData = []
          res.data.forEach(ele => {
            if (ele.type === TAGS) {
              ele.type = '标签'
            } else {
              ele.type = '自由标签'
            }
            this.tableData.push(ele)
          });
          this.pagination.total = total
        }
      }).finally(() => {
        this.tableLoading = false
      })
    },
    // 执行搜索
    doSearch() {
      let filter = {}
      if (this.currentType === 'tags') {
        if (this.tagMatcher !== '') {
          filter['tag_matcher'] = this.tagMatcher
        }
      }
      else {
        if (this.keyMatcher !== '') {
          filter['key_matcher'] = this.keyMatcher
        }
        if (this.valueMatcher !== '') {
          filter['value_matcher'] = this.valueMatcher
        }
      }
      this.pagination.current = 1
      this.filter = {...filter}
      this.queryTableData()
    },
    // 重置搜索
    resetSearch() {
      this.tagMatcher = ''
      this.keyMatcher = ''
      this.valueMatcher = ''
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
