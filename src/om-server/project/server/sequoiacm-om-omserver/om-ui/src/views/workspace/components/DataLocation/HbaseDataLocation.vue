<template>
  <div>
    <el-form :model="form" ref="form" size="mini" label-width="140px" label-position="left">
      <el-form-item label="namespace" prop="namespace">
        <el-input v-model="form.namespace" maxlength="30" placeholder="默认使用 hbase 内置 namespace：'default'"></el-input>
      </el-form-item>
      <el-form-item label="数据分区规则" prop="shardingType">
        <template slot="label">
          数据分区规则
          <el-tooltip effect="dark" placement="bottom">
            <div slot="content">
              当前分区规则下，文件内容将存储在 "{{form.namespace?form.namespace:'default'}}:[wsName]_SCMFILE{{this.$util.getShardingStr(form.shardingType, '_')}}" 表中
            </div>
            <i class="el-icon-question"></i>
          </el-tooltip>
        </template>
        <scm-sharding-select v-model="form.shardingType"/>
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
        namespace: '',
        shardingType: 'month'
      }
    }
  },

  methods: {
    toFormData(){
      let fromData = {
        site: this.siteName,
        data_sharding_type: this.form.shardingType
      }
      if (this.form.namespace) {
        fromData['hbase_namespace'] = this.form.namespace
      }
      return fromData
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
        namespace: '',
        shardingType: 'month'
      }
    }
  }
  
}
</script>

<style scoped>
</style>