<template>
  <div>
    <el-form :model="form" ref="form" size="mini" label-width="140px" label-position="left">
      <el-form-item label="数据分区规则" prop="dataShardingType">
        <template slot="label">
          数据分区规则
          <el-tooltip effect="dark" placement="bottom">
            <div slot="content">
              当前分区规则下，文件内容将存储在 "[wsName]_scmfile{{this.$util.getShardingStr(form.dataShardingType, '_')}}" Container下
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
        dataShardingType: 'month'
      }
    }
  },

  methods: {
    
    toFormData(){
      return {
        site: this.siteName,
        data_sharding_type: this.form.dataShardingType
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
        dataShardingType: 'month'
      }
    }
  }
  
}
</script>

<style scoped>
</style>