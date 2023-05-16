<template>
  <el-popover
    v-model="isTagSearchPopoverShow"
    placement="bottom"
    trigger="click"
    @show="show">
    <div style="width: 680px;">
      <template>
        <el-tabs v-model="form.searchMode" style="margin-bottom: 20px;">
          <el-tab-pane label="标准模式" name="standard">
            <el-form ref="form" :model="form">
              <el-form-item v-if="form.conditionList && form.conditionList.length > 0">
                <el-radio-group v-model="form.conditionMode" size="small">
                  <el-radio-button label="$and">满足全部条件</el-radio-button>
                  <el-radio-button label="$or">满足单一条件</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-for="(condition, index) in form.conditionList" :key="index">
                <el-row>
                  <el-col :span="4">
                    <el-form-item>
                      <el-select v-model="condition.tagType" size="mini">
                        <el-option
                          v-for="item in tagTypes"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value">
                        </el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="3">
                    <el-form-item>
                      <el-select v-model="condition.conditionType" size="mini" style="margin-left: 4px;">
                        <el-option
                          v-for="item in conditionTypeList"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value">
                        </el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8" v-if="condition.tagType === 'tags'">
                    <el-form-item :prop="'conditionList.' + index + '.tagInput'" :rules="rules.input">
                      <el-autocomplete 
                        v-model="condition.tagInput" 
                        placeholder="请输入标签名" 
                        size="mini" 
                        :fetch-suggestions="queryTagList"
                        style="margin-left: 4px; width: 100%;">
                      </el-autocomplete>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4" v-if="condition.tagType === 'custom_tag'">
                    <el-form-item :prop="'conditionList.' + index + '.keyInput'" :rules="rules.input">
                      <el-autocomplete 
                        v-model="condition.keyInput" 
                        placeholder="请输入 key" 
                        size="mini" 
                        :fetch-suggestions="queryKeyList"
                        style="margin-left: 4px; width: 100%;">
                      </el-autocomplete>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4" v-if="condition.tagType === 'custom_tag'">
                    <el-form-item :prop="'conditionList.' + index + '.valueInput'" :rules="rules.input">
                      <el-autocomplete 
                        v-model="condition.valueInput" 
                        placeholder="请输入 value" 
                        size="mini" 
                        :fetch-suggestions="queryValList(condition)"
                        style="margin-left: 4px; width: 100%;">
                      </el-autocomplete>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4">
                    <el-form-item>
                      <el-checkbox v-model="condition.ignoreCase" size="mini" style="margin-left: 12px;">
                        <template>
                          <el-tooltip :disabled="condition.tagType!=='custom_tag'" content="仅支持 value 忽略大小写">
                            <span>忽略大小写</span>
                          </el-tooltip>
                        </template>
                      </el-checkbox>
                    </el-form-item>
                  </el-col>
                  <el-col :span="4" size="mini">
                    <el-form-item>
                      <el-checkbox v-model="condition.supportRegex" size="mini"  style="margin-left: 4px;">
                        <template>
                          <el-tooltip>
                            <div slot="content" class="tooltip">
                              例如： <br/> * 表示匹配 0-n 个字符<br/> ? 表示匹配一个字符
                            </div>
                            <span>支持通配符</span>
                          </el-tooltip>
                        </template>
                      </el-checkbox>
                    </el-form-item>
                  </el-col>
                  <el-col :span="1">
                    <el-form-item>
                      <el-button  icon="el-icon-delete" size="mini" circle @click="removeCondition(index)"></el-button>
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form-item>
              <el-button icon="el-icon-plus" size="mini" @click="addCondition">添加</el-button>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="高级模式" name="advanced">
            <el-input
              id="input_schedule_condition"
              v-model="form.customCondition"
              type="textarea"
              :rows="5"
              show-word-limit
              :placeholder="exampleCustomCondition">
            </el-input>
          </el-tab-pane>
        </el-tabs>
      </template>
  
      <el-divider></el-divider>

      <span slot="footer" class="filter-footer">
        <el-button id="btn_tag_search_reset" size="mini" icon="el-icon-refresh" @click="resetCondition">重置</el-button>
        <el-button id="btn_tag_search_cancel" size="mini" @click="cancel">取 消</el-button>
        <el-button id="btn_tag_search_sure"  type="primary" size="mini" @click="submitForm">确 定</el-button>
      </span>
    </div>

    <template slot="reference">
      <el-badge v-if="store.conditionCount===0">
        <el-button size="small" icon="el-icon-collection-tag" plain>标签检索</el-button>
      </el-badge>
      <el-badge v-else :value="store.conditionCount" type="primary">
        <el-button size="small" icon="el-icon-collection-tag" plain type="primary">标签检索</el-button>
      </el-badge>
    </template>
  </el-popover>
</template>

<script>
import {TAG_TYPES, TAGS, CUSTOM_TAG} from '@/utils/common-define'
import {listTag, listCustomTagKey} from '@/api/tag'
export default {
  props: {
    workspace: {
      type: String,
      default: ''
    }
  },
  data(){
    return {
      isTagSearchPopoverShow: false,
      form: {
        searchMode: 'standard',
        customCondition: '',
        conditionMode: '$and',
        conditionList:[]
      },
      store: {
        searchMode: 'standard',
        customCondition: '',
        conditionMode: '$and',
        conditionList: [],
        conditionCount: 0
      },
      rules: {
        input: [
          { required: true, message: '该项不能为空', trigger: 'change' }
        ]
      },
      tagTypes: TAG_TYPES,
      conditionTypeList: [
        { label: '包含', value: '$contains' },
        { label: '不包含', value: '$not_contains'}
      ],
      exampleCustomCondition: '自定义标签检索条件，例如:\n { "$and" : [ { "tags" : { "$contains" : "tag" , "$ignore_case" : false , "$enable_wildcard" : false } } ] }'
    }
  },
  methods:{
    showPopover() {
      this.isTagSearchPopoverShow = true
    },
    // 弹出框显示时触发（初始化条件列表）
    show() {
      // 使用持久化数据重置临时数据
      this.form.searchMode = this.store.searchMode
      this.form.customCondition = this.store.customCondition
      this.form.conditionMode = this.store.conditionMode
      this.form.conditionList = JSON.parse(JSON.stringify(this.store.conditionList))
    },
    // 关闭弹出框
    closePopover() {
      this.isTagSearchPopoverShow = false
    },
    // 点击确定按钮
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.saveCondition()
        }
      })
    },
    // 持久化检索条件
    saveCondition() {
      // 0. 参数检查
      if (this.form.searchMode === 'advanced') {
        if (!this.$util.isJsonStr(this.form.customCondition)) {
          this.$message.error(`请检查条件格式是否正确`)
          return
        }
      }
      // 1. 参数持久化
      this.store.searchMode = this.form.searchMode
      this.store.customCondition = this.form.customCondition
      this.store.conditionMode = this.form.conditionMode
      this.store.conditionList = JSON.parse(JSON.stringify(this.form.conditionList))
      this.closePopover()
      // 2. 生成查询参数
      let tagCondition = {}
      if (this.form.searchMode === 'advanced') {
        this.store.conditionCount = this.store.customCondition === '' ? 0 : 1
        tagCondition = this.store.customCondition
      }
      else {
        this.store.conditionCount = this.store.conditionList.length
        tagCondition = this.generateTagCondition()
      }
      this.$emit('onTagConditionChange', tagCondition)
    },
    // 生成 JSON 格式的标签检索条件
    generateTagCondition() {
      let conditionList = []
      this.form.conditionList.forEach(ele => {
        let curCondition = {}
        let item = {}
        item["$ignore_case"] = ele.ignoreCase
        item["$enable_wildcard"] = ele.supportRegex
        if (ele.tagType === TAGS) {
          item[ele.conditionType] = ele.tagInput
          curCondition[TAGS] = item
        }
        else {
          let obj = {}
          obj[ele.keyInput] = ele.valueInput
          item[ele.conditionType] = obj
          curCondition[CUSTOM_TAG] = item
        }
        conditionList.push(curCondition)
      })
      return { [this.form.conditionMode] : conditionList}
    },
    // 点击重置按钮
    resetCondition() {
      this.form = {
        searchMode: 'standard',
        customCondition: '',
        conditionMode: '$and',
        conditionList:[]
      }
    },
    // 点击取消按钮
    cancel() {
      this.closePopover()
    },
    // 新增条件
    addCondition() {
      this.form.conditionList.push({tagType: 'tags', conditionType: '$contains', tagInput: '', keyInput: '', valueInput: '', ignoreCase: false, supportRegex: false})
    },
    // 根据用户输入标签提示补全
    async queryTagList(queryString, callback) {
      let tagMatcher = queryString + '*'
      let filter = { "tag_matcher": tagMatcher }
      let res = await listTag(this.workspace, TAGS, filter, 1, 10)
      let filterTagList = []
      res.data.forEach(ele => {
        filterTagList.push({value: ele.tag})
      });
      setTimeout(() => {
        callback(filterTagList);
      }, 500);
    },
    // 根据用户输入自由标签 key 提示补全
    async queryKeyList(queryString, callback) {
      let keyMatcher = queryString + '*'
      let res = await listCustomTagKey(this.workspace, keyMatcher, 1, 10)
      let filterKeyList = []
      res.data.forEach(ele => {
        filterKeyList.push({value: ele})
      });
      setTimeout(() => {
        callback(filterKeyList);
      }, 500);
    },
    // 根据用户输入自由标签 key 提示补全 value
    queryValList(condition){
      let _this  = this
      return async function(queryString, callback){
        let filter = { }
        filter["key_matcher"] = condition.keyInput
        filter["value_matcher"] = condition.valueInput + "*"
        let res = await listTag(_this.workspace, CUSTOM_TAG, filter, 1, 10)
        let filterValueList = []
        res.data.forEach(ele => {
          filterValueList.push({value: ele.value})
        });
        setTimeout(() => {
          callback(filterValueList);
        }, 500);
      }
    },
    // 删除条件
    removeCondition(index) {
      this.form.conditionList.splice(index, 1)
    },
    // 重置标签检索条件
    reInit() {
      this.store = {
        searchMode: 'standard',
        customCondition: '',
        conditionMode: '$and',
        conditionList: [],
        conditionCount: 0
      }
    }
  }
}
</script>

<style scoped>
.filter-footer {
  float: right;
  margin: 12px 0px;
}
.el-form-item {
  height: 30px;
}
.el-tabs >>> .el-tabs__content {
  max-height: 400px;
  overflow-y: auto;
}
</style>