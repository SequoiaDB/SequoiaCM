<template>
  <div>
    <!-- 文件上传对话框 -->
    <el-dialog
      class="upload-dialog"
      title="上传文件"
      :visible.sync="uploadDialogVisible"
      width="650px">
        <el-form ref="form" :rules="rules" :model="form" size="small" label-width="110px" :disabled="uploadForbidden">
          <el-form-item label="存储桶" prop="bucket">
            <el-select 
              id="select_upload_bucket"
              v-model="form.bucket" 
              size="small" 
              placeholder="请选择存储桶"  
              clearable filterable 
              style="width:100%"
              @change="onBucketChange" >
                <el-option
                  v-for="item in bucketList"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="存储站点" prop="site">
            <el-select 
              id="select_upload_object_site"
              v-model="form.site" 
              :no-data-text="form.bucket ? '无数据' : '请先选择存储桶'"
              size="small" 
              placeholder="请选择存储站点" 
              clearable filterable 
              style="width:100%">
                <el-option
                  v-for="item in workspaceSiteList"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="标题" prop="title">
            <el-input id="input_file_title" v-model="form.title" placeholder="请输入文件标题"></el-input>
          </el-form-item> 
          <el-form-item label="作者" prop="author">
            <el-input id="input_file_author" v-model="form.author" placeholder="请输入作者"></el-input>
          </el-form-item>
          <el-form-item label="标签" prop="tags">
            <el-tag
              id="tag_list"
              :key="tag"
              v-for="tag in form.tags"
              closable
              :disable-transitions="false"
              @close="handleCloseTag(tag)">
                <el-tooltip :content="tag" placement="top">
                  <span class="tag-text">{{tag}}</span>
                </el-tooltip>
            </el-tag>
            <el-input
              class="input-new-tag"
              v-if="tagInputVisible"
              v-model="tagInputValue"
              ref="saveTagInput"
              size="small"
              @keyup.enter.native="handleInputConfirm"
              @blur="handleInputConfirm"
            >
            </el-input>
            <el-button v-else class="button-new-tag" size="small" @click="showInput">+ 添加标签</el-button>
          </el-form-item>
          <el-form-item label="自定义元数据" v-if="workspaceClassList.length > 0" prop="classId">
            <el-select 
              id="select_metadatas_class"
              v-model="form.classId"
              size="small"
              placeholder="请选择元数据模型" 
              clearable filterable
              style="width:100%"
              @change="onClassChange" >
                <el-option
                  v-for="item in workspaceClassList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id">
                </el-option>
            </el-select>
            <div v-if="form.classId != ''" style="margin-top: 30px;">
              <el-form-item
                v-for="(attr) in form.selectAttributes"
                :label="attr.name"
                :key="attr.name"
                class="scm-attribute" >
                  <el-select 
                    id="select_attr_boolean_val"
                    v-if="attr.type==='BOOLEAN'"
                    v-model="attr.value" 
                    size="medium" 
                    style="width: 200px;">
                    <el-option
                      v-for="item in booleanType"
                      :key="item.value"
                      :label="item.value"
                      :value="item.value">
                    </el-option>
                  </el-select>
                  <el-date-picker
                    id="attr_date_val_picker"
                    v-else-if="attr.type==='DATE'"
                    v-model="attr.value"
                    type="datetime"
                    value-format="yyyy-MM-dd-HH:mm:ss.SSS"
                    placeholder="选择日期时间"
                    align="right"
                    :picker-options="pickerOptions"
                    style="width: 200px;">
                  </el-date-picker>
                  <el-input id="input_attr_val" v-else style="width: 200px;" v-model="attr.value" :type=getInputType(attr.type) :placeholder="attr.type"></el-input>        
                  <el-button id="btn_delete_attr_option" v-if="!attr.required" size="mini" type="text" icon="el-icon-delete" style="color: #F56C6C;margin-left: 5px" @click="removeAttributes(attr)">删除</el-button>
              </el-form-item>
              <template v-if="form.residualAttributes.length > 0">
                <el-select v-model="form.attributeName" placeholder="请选择属性" style="width: 330px;">
                  <el-option
                      v-for="item in form.residualAttributes"
                      :key="item.name"
                      :value="item.name" >
                </el-option>
                </el-select>
                <el-button :disabled="form.attributeName===''" style="margin-left: 12px;" @click="addAttributes">添加</el-button>
              </template>
            </div>
          </el-form-item>
          <el-form-item label="文件内容" prop="fileContentList">
            <el-upload 
              ref="elUpload"
              id="select_upload_file_content"
              :auto-upload="false"
              :file-list="form.fileContentList"
              :on-change="handleFileContentChange"
              :on-remove="handleFileRemove"
              :http-request="_uploadFile"
              action=""
              drag>
              <i class="el-icon-upload"></i>
              <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            </el-upload>
          </el-form-item>  
          <el-form-item label="文件名" prop="name">
            <el-input id="input_file_name" v-model="form.name" placeholder="请输入文件名"></el-input>
          </el-form-item>
          <el-form-item label="自由元数据" prop="customMetadata">
            <div v-if="this.form.customMetadata.length > 0">
              <div v-for="(item, index) in this.form.customMetadata" :key="index" style="margin-top: 5px">
                <el-input class="input-new-custom-metadata" v-model="item.key" placeholder="key"></el-input>
                <el-input class="input-new-custom-metadata" v-model="item.value" placeholder="value"></el-input>
                <el-button size="mini" type="text" icon="el-icon-delete" style="color: #F56C6C" @click="deleteCustomMeta">删除</el-button>
              </div>
            </div>
            <el-button size="mini" type="text" icon="el-icon-plus" style="margin-top: 5px" @click="addCustomMeta">添加自由元数据</el-button>
          </el-form-item>
          <el-form-item label="自由标签" prop="customTag">
            <div v-if="this.form.customTag.length > 0">
              <div v-for="(item, index) in this.form.customTag" :key="index" style="margin-top: 5px">
                <el-input class="input-new-custom-metadata" v-model="item.key" placeholder="key"></el-input>
                <el-input class="input-new-custom-metadata" v-model="item.value" placeholder="value"></el-input>
                <el-button size="mini" type="text" icon="el-icon-delete" style="color: #F56C6C" @click="deleteCustomTag">删除</el-button>
              </div>
            </div>
            <el-button size="mini" type="text" icon="el-icon-plus" style="margin-top: 5px" @click="addCustomTag">添加自由标签</el-button>
          </el-form-item>
        </el-form>
      <span slot="footer" class="dialog-footer" style="border:1px soild red">
        <el-button id="btn_upload_file" type="primary" @click="submitForm" size="mini" :disabled="uploadForbidden">保 存</el-button>
        <el-button id="btn_upload_close" @click="close" size="mini">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {queryWorkspaceBasic} from '@/api/workspace'
import {queryClassList, queryClassDetail} from '@/api/metadata'
import {queryBucketDetail, createFileInBucket} from '@/api/bucket'
export default {
  props: {
    bucketList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    let fileContentValidation = (rule, value, callback) => {
      if (this.form.fileContentList.length === 0) {
        callback(new Error('请选择文件内容'));
      } else {
        callback()
      }
    }
    return{
      pickerOptions: {
        shortcuts: [{
          text: '今天',
          onClick(picker) {
            picker.$emit('pick', new Date());
          }
        }, {
          text: '昨天',
          onClick(picker) {
            const date = new Date();
            date.setTime(date.getTime() - 3600 * 1000 * 24);
            picker.$emit('pick', date);
          }
        }, {
          text: '一周前',
          onClick(picker) {
            const date = new Date();
            date.setTime(date.getTime() - 3600 * 1000 * 24 * 7);
            picker.$emit('pick', date);
          }
        }]
      },
      booleanType: [ 
        { value: 'true' },
        { value: 'false' }
      ],
      uploadDialogVisible: false,
      uploadForbidden: false,
      currentBucket: '',
      currentBucketDetail: {},
      currentWorkspace: '',
      workspaceSiteList: [],
      workspaceClassList: [],
      classDetail: {},
      tagInputVisible: false,
      tagInputValue: '',
      uploadRequest: {},
      form: {
        bucket: '',
        site: '',
        title: '',
        author: '',
        classId: '',
        selectAttributes: [],
        residualAttributes: [],  
        attributeName: '',
        fileContentList: [],
        customMetadata: [],
        customTag: [],
        tags: [],
        name: '',
      },
      rules: {
        bucket: [
          { required: true, message: '请选择存储桶', trigger: 'none' },
        ],
        site: [
          { required: true, message: '请选择存储站点', trigger: 'change' },
        ],
        fileContentList: [
          { required: true, validator: fileContentValidation }
        ],
        name: [
          { required: true, message: '请输入文件名', trigger: 'change' },
        ],
      },
    }
  },
  methods: {
    show() {
      this.uploadDialogVisible = true
    },
    close() {
      this.uploadDialogVisible = false
    },
    clear() {
      setTimeout(()=>{
        // 重置表单
        this.clearForm()
        this.workspaceSiteList = []
        this.workspaceClassList = []
      }, 500)
    },
    handleFileContentChange(file, fileList) {
      // 清除校验 场景：提交表单校验未选择文件内容，重新添加时清除校验信息
      this.$refs['form'].clearValidate(['fileContentList'])
      // 限制文件上传个数、自动填充文件名
      if (file.status !== 'fail') {
        if (fileList.length > 1) {
          fileList.splice(0, 1)
        }
        this.form.fileContentList = fileList
        this.form.name = file.name
      }
    },
    handleFileRemove() {
      this.form.fileContentList = []
    },
    _uploadFile(data) {
      let bucketName = this.form.bucket
      let site = this.form.site
      let uploadConf = {
        // 桶内上传文件必须计算 MD5 
        is_overwrite: false,
        is_need_md5: true
      }
      
      let attributeValid = true
      let classProperties = new Map()
      this.form.selectAttributes.forEach((item) =>{
        if (!this.checkAttributeVal(item)) {
          this.$message.error("元数据未填写或填写类型不正确，属性 " + item.name + " 要求的类型为：" + item.type)
          attributeValid = false
          return
        }
        classProperties.set(item.name, this.convertAttributeVal(item))
      })
      let class_properties = this.$util.mapToObject(classProperties)
      if (!attributeValid) {
        return
      }

      let custom_metadata = {}
      for (let item of this.form.customMetadata) {
        custom_metadata[item.key] = item.value
      }
      let custom_tag = {}
      for (let item of this.form.customTag) {
        custom_tag[item.key] = item.value
      }
      let fileInfo = {
        name: this.form.name,
        title: this.form.title,
        author: this.form.author,
        tags: this.form.tags,
        class_id: this.form.classId,
        custom_metadata,
        custom_tag,
        class_properties
      }
      let desc=encodeURIComponent(this.$util.toPrettyJson(fileInfo))

      this.uploadForbidden = true
      this.uploadRequest = createFileInBucket(bucketName, site, desc, uploadConf, data).then(res=>{
        this.$message.success("文件【" + this.form.name + "】上传成功")
        this.clear()
        this.close()
        this.$emit('refreshTable')
      }).finally(() => {
        this.uploadForbidden = false
        if (this.form.fileContentList.length > 0) {
          this.form.fileContentList[0].status = 'ready'
        }
      })
    },
    // 提交表单（上传文件）
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.$refs['elUpload'].submit()
        }
      })
    },
    // 选择存储桶
    onBucketChange(bucket) {
      this.resetSite()
      this.resetClass()
      if (!bucket) {
        return
      }
      this.currentBucket = bucket
      queryBucketDetail(bucket).then(res => {
        this.currentBucketDetail = JSON.parse(res.headers['bucket'])
        this.currentWorkspace = this.currentBucketDetail.workspace
        queryWorkspaceBasic(this.currentWorkspace).then(res => {
          this.workspaceDetail = JSON.parse(res.headers['workspace'])
          let siteList = this.workspaceDetail['data_locations']
          if (siteList && siteList.length > 0) {
            for (let site of siteList) {
              this.workspaceSiteList.push(site['site_name'])
            }
          }
        })
        queryClassList(this.currentWorkspace, null, null, 1, -1).then(res=>{
          this.workspaceClassList = res.data;
        })
      })
    },
    resetSite() {
      this.workspaceSiteList = []
      this.form.site = ''
    },
    resetClass() {
      this.workspaceClassList = []
      this.form.classId = ''
      this.onClassChange()
    },
    // 选择元数据模型
    onClassChange(classId) {
      // 清空元数据、元数据属性
      this.classDetail = {}
      this.form.selectAttributes = []
      this.form.residualAttributes = []
      if (!classId) {
        return;
      }
      queryClassDetail(this.currentWorkspace, classId).then(res=>{
        this.classDetail = res.data
        let attrs = this.classDetail.attrs
        attrs.forEach((item) =>{
          if (item.required) {
            this.form.selectAttributes.push(item)
          } else {
            this.form.residualAttributes.push(item)
          }
        })
      })
    },
    // 根据元数据属性类型，生成输入框类型
    getInputType(type) {
      if (type === 'STRING') {
        return 'string'
      } else {
        return 'number'
      }
    },
    // 校验元数据属性类型是否正确
    checkAttributeVal(attr) {
      switch(attr.type) {
        case 'INTEGER':
          var regs = /^-?\d+$/;
          return regs.test(attr.value)
        case 'DOUBLE':
          var regs = /^(-?\d+)(\.\d+)?$/;
          return regs.test(attr.value)
        case 'BOOLEAN':
          return attr.value === 'false' || attr.value === 'true'
        case 'DATE':
          return attr.value != undefined && attr.value != null
        default:
          return true
      }
    },
    // 转换元数据属性类型
    convertAttributeVal(attr) {
      switch(attr.type) {
        case 'INTEGER':
          return parseInt(attr.value)
        case 'DOUBLE':
          return parseFloat(attr.value)
        case 'BOOLEAN':
          return attr.value === 'true' ? true : false
        default:
          return attr.value
      }
    },
    // 删除标签
    handleCloseTag(tag) {
      this.form.tags.splice(this.form.tags.indexOf(tag), 1);
    },
    showInput() {
      this.tagInputVisible = true;
      this.$nextTick(_ => {
        this.$refs.saveTagInput.$refs.input.focus();
      });
    },
    // 捕获标签添加事件
    handleInputConfirm() {
      let tagInputValue = this.tagInputValue;
      if (tagInputValue) {
        for (const existTag of this.form.tags) {
          if (existTag == tagInputValue){
            this.$message.error("已存在标签：" + tagInputValue);
            return;
          }
        }
        this.form.tags.push(tagInputValue);
      }
      this.tagInputVisible = false;
      this.tagInputValue = '';
    },
    // 删除自由元数据
    deleteCustomMeta(index) {
      this.form.customMetadata.splice(index, 1)
    },
    // 捕获自由元数据添加事件
    addCustomMeta() {
      this.form.customMetadata.push({ 'key': '', 'value': ''})
    },
    // 删除自由标签
    deleteCustomTag(index) {
      this.form.customTag.splice(index, 1)
    },
    // 捕获自由标签添加事件
    addCustomTag() {
      this.form.customTag.push({ 'key': '', 'value': ''})
    },
    // 动态添加元数据属性
    addAttributes() {
      let index = -1
      for (var i = 0; i < this.form.residualAttributes.length; i++) {
        if (this.form.residualAttributes[i].name === this.form.attributeName) {
          index = i
          break
        }
      }
      if (index > -1) {
        let attr = this.form.residualAttributes[i]
        this.form.residualAttributes.splice(index, 1)
        this.form.selectAttributes.push(attr)
        this.form.attributeName = ''
      }
    },
    // 去除已选属性
    removeAttributes(attr) {
      let index = this.form.selectAttributes.indexOf(attr)
      if (index !== -1) {
        this.form.selectAttributes.splice(index, 1)
        this.form.residualAttributes.push(attr)
      }
    },
    clearForm() {
      if (this.$refs['form']) {
        this.$refs['form'].resetFields()
      }
    }
  }
}
</script>

<style  scoped>
.upload-dialog >>> .el-dialog__body {
  max-height: 800px;
  overflow-y: auto;
  padding: 25px 25px;
}
.upload-container >>> .el-row {
  margin-top: 8px !important;
}
::v-deep .el-form-item__content {
  display: flex;
  flex-wrap: wrap;
}
.el-tag {
  margin-right: 10px;
  margin-bottom: 5px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.el-tag >>> .el-icon-close {
  top: 0;
}
.tag-text {
  display: inline-block;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.button-new-tag {
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}
.input-new-tag {
  width: 90px;
  margin-left: 10px;
  vertical-align: bottom;
}
.input-new-custom-metadata {
  width: 40%;
  margin-right: 5px;
  vertical-align: bottom;
}
.scm-attribute >>> .el-form-item__label {
  color: rgb(97, 161, 161);
  font-size: 10px;
  width: 130px !important;
}
</style>