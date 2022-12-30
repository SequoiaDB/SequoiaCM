<template>
  <div>
    <el-form :model="form" ref="form" size="mini" label-width="140px" label-position="left">
      <el-form-item label="文件存储目录" prop="rootPath">
        <el-input v-model="form.rootPath" maxlength="30"  placeholder="指定文件数据存放的路径，默认：'/scm'"></el-input>
      </el-form-item>
      <el-form-item label="数据分区规则" prop="dataShardingType">
        <template slot="label">
          数据分区规则
          <el-tooltip effect="dark" placement="bottom">
            <div slot="content">
              当前分区规则下，文件内容将存储在 "{{form.rootPath?form.rootPath:'/scm'}}/[wsName]{{this.$util.getShardingStr(form.dataShardingType, '_')}}" 路径下
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </template>
        <scm-sharding-select v-model="form.dataShardingType"/>
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
        rootPath: '',
        dataShardingType: 'month'
      }
    }
  },

  methods: {
    
    toFormData(){
      let formData = {
        site: this.siteName,
        data_sharding_type: this.form.dataShardingType
      }
      if (this.form.rootPath) {
        formData['hdfs_file_root_path'] = this.form.rootPath
      }
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
        rootPath: '',
        dataShardingType: 'month'
      }
    }
  }
  
}
</script>

<style scoped>
</style>