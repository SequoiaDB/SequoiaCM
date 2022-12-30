<template>
  <div>
    <el-form :model="form" ref="form" :rules="rules" size="mini" label-width="140px" label-position="left">
      <el-form-item label="桶生成规则">
        <el-radio-group v-model="form.bucketGenerateRule">
          <el-radio label="auto">自动生成</el-radio>
          <el-radio label="custom">手动指定</el-radio>
        </el-radio-group>
      </el-form-item> 
      <el-form-item label="桶名" prop="bucketName" v-if="form.bucketGenerateRule === 'custom'">
        <el-input v-model="form.bucketName" maxlength="30" placeholder="请指定一个已经存在的桶作为桶名"></el-input>
      </el-form-item>
      <el-form-item label="桶名前缀" prop="bucketPrefix" v-if="form.bucketGenerateRule === 'auto'">
        <el-input v-model="form.bucketPrefix" maxlength="30" placeholder="留空使用默认前缀：[wsName]-scmfile"></el-input>
      </el-form-item>
      <el-form-item prop="bucketShardingType" v-if="form.bucketGenerateRule === 'auto'">
        <template slot="label">
          分桶规则
          <el-tooltip effect="dark" placement="bottom">
            <div slot="content">
              当前分桶规则下，文件内容将存储在 "{{form.bucketPrefix?form.bucketPrefix:'[wsName]-scmfile'}}{{this.$util.getShardingStr(form.bucketShardingType, '-')}}" 桶中
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </template>
        <scm-sharding-select v-model="form.bucketShardingType"/>
      </el-form-item>
      <el-form-item prop="objectShardingType">
        <template slot="label">
          桶内分目录规则
          <el-tooltip class="item" effect="dark" placement="bottom">
            <div slot="content">
              当前分目录规则下，生成的对象key示例为：{{this.$util.getShardingStr(form.objectShardingType)}}{{form.objectShardingType==='none'?'':'/'}}[objectId]
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </template>
        <scm-sharding-select v-model="form.objectShardingType" :dayEnabled="true"/>
      </el-form-item>
    </el-form>

  </div>
</template>

<script>
import ScmShardingSelect from '@/components/selector/ScmShardingSelect.vue'

export default {
  components: {
    ScmShardingSelect
  },
  props: {
    siteName: {
      type: String,
      required: true
    }
  },
  data(){
    return {
      form: {
        bucketPrefix: '',
        bucketName: '',
        bucketGenerateRule: 'auto',
        bucketShardingType: 'month',
        objectShardingType: 'none',
      },
      rules: {
        bucketName: {required: true, message: "请输入桶名", trigger: 'blur'}
      }
    
    }
  },

  methods: {
    toFormData(){
      let formData = {
        site: this.siteName
      }
      if (this.form.bucketGenerateRule == 'custom') {
        formData['bucket_name'] = this.form.bucketName
      } else {
        formData['data_sharding_type'] = this.form.bucketShardingType
        if (this.form.bucketPrefix) {
          formData['container_prefix'] = this.form.bucketPrefix
        }
      }
      formData['object_sharding_type'] = this.form.objectShardingType
      return formData
    },

    validate() {
      let result = false
      this.$refs['form'].validate(valid => {
        result = valid
      })
      return result
    },

    clear() {
      this.form = {
        bucketPrefix: '',
        bucketName: '',
        bucketGenerateRule: 'auto',
        bucketShardingType: 'month',
        objectShardingType: 'none',
      }
    }
  }
  
}
</script>

<style scoped>
</style>