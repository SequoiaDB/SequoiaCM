<template>
  <div>
    <el-form :model="form" ref="form" :rules="rules" size="mini" label-width="140px" label-position="left">
      <el-row :gutter="10">
        <el-col :span="12">
          <el-form-item label="数据域" prop="domain">
            <el-input maxlength="30" v-model="form.domain" placeholder="请输入数据域"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分区规则" prop="shardingType">
            <template slot="label">
              分区规则
              <el-tooltip effect="dark" placement="bottom">
                <div slot="content">
                  当前分区规则下，文件元数据将存储在 "[wsName]_META.FILE{{this.$util.getShardingStr(form.shardingType, '_')}}" 表中
                </div>
                <i class="el-icon-question"></i>
              </el-tooltip>
            </template>
            <scm-sharding-select v-model="form.shardingType" :noneEnabled="false"/>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="集合空间创建参数" prop="csOptions">
        <el-input type="textarea" v-model="form.csOptions"  placeholder='指定集合空间的创建参数，例如：{"LobPageSize":262144}'></el-input>
      </el-form-item>
      <el-form-item label="集合创建参数" prop="clOptions">
        <el-input type="textarea" v-model="form.clOptions"   placeholder='指定集合的创建参数，例如：{"ReplSize":-1}'></el-input>
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
    let jsonValidator = (rule, value, callback) => {
      if (value && !this.$util.isJsonStr(value)) {
        callback(new Error('json格式不正确'));
      }else {
        callback()
      }
    }
    return {
      form: {
        domain: '',
        shardingType: 'year',
        csOptions: '',
        clOptions: ''
      },
      rules: {
        domain:　{required: true, message: "请输入domain", trigger: 'blur'},
        csOptions: {validator: jsonValidator},
        clOptions: {validator: jsonValidator}
      }
    }
  },

  methods: {
    toFormData(){
      return {
        site: this.siteName,
        domain: this.form.domain,
        meta_sharding_type: this.form.shardingType,
        meta_options: {
          collection_space: this.form.csOptions ? JSON.parse(this.form.csOptions) : {},
          collection: this.form.clOptions ? JSON.parse(this.form.clOptions) : {}
        }
      }
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
        domain: '',
        shardingType: 'year',
        csOptions: '',
        clOptions: ''
      }
    }
  }
  
}
</script>

<style scoped>
</style>