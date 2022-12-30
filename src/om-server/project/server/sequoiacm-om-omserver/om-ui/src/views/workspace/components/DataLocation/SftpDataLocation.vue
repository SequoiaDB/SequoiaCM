<template>
  <div>
    <el-form :model="form" ref="form" size="mini" label-width="140px" label-position="left">
      <el-form-item label="文件存储目录" prop="fileDir">
        <el-input v-model="form.fileDir" maxlength="60"  placeholder="指定文件数据存放的路径，默认：'/scmfile'"></el-input>
      </el-form-item>
      <el-form-item label="分目录规则" prop="dataShardingType">
        <template slot="label">
          分目录规则
          <el-tooltip effect="dark" placement="bottom">
            <div slot="content">
              当前分目录规则下，文件内容存储在 "{{form.fileDir?form.fileDir:'/scmfile'}}/[wsName]{{this.$util.getShardingStr(form.dataShardingType, '_')}}" 目录下
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </template>
        <scm-sharding-select v-model="form.dataShardingType" :dayEnabled="true"/>
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
        fileDir: '',
        dataShardingType: 'day'
      }
    }
  },

  methods: {
    toFormData(){
      let formData = {
        site: this.siteName,
        data_sharding_type: this.form.dataShardingType
      }
      if (this.form.fileDir) {
        formData['data_path'] = this.form.fileDir
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
        fileDir: '',
        dataShardingType: 'day'
      }
    }
  }
  
}
</script>

<style scoped>
</style>