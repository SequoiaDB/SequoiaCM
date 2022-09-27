<template>
  <div>
    <!-- 动态刷新属性对话框 -->
    <el-dialog
      :title="name"
      :visible.sync="updatePropDialogVisible"
      width="550px">
      <table class="table is-fullwidth">
        <tr v-for="(item, index) in configProps" :key="index">
          <td>
            <span v-text="item.key" />
          </td>
          <td>
            <el-input v-model="item.value"></el-input>
          </td>
        </tr>
      </table>
      <span slot="footer" class="dialog-footer" style="border:1px soild red">
        <el-button id="btn_update_properties" type="primary" size="mini" @click=handleUpdateProperties>刷新配置</el-button>
        <el-button id="btn_update_prop_close" size="mini" @click=close>关 闭</el-button>
      </span>
      
    </el-table>
    </el-dialog>
  </div>
</template>

<script>
import {updateProperties} from '@/api/config-props'
export default {
  props:{
    type: String,
    name: String,
    configProps: Array
  },
  data() {
    return{
      updatePropDialogVisible: false
    }
  },
  methods: {
    show() {
      this.updatePropDialogVisible = true
    },
    close() {
      this.updatePropDialogVisible = false
    },
    handleUpdateProperties() {
      let confPropParam = {}
      confPropParam['targetType'] = this.type
      confPropParam['targets'] = [ this.name ]

      let properties = {}
      this.configProps.forEach((item) =>{
        properties[item.key] = item.value
      })
      confPropParam['updateProperties'] = properties
      
      updateProperties(confPropParam).then(res => {
        this.updatePropDialogVisible = false
        this.$emit('refreshConfig')
        let result = res.data
        if (result.failures.length > 0) {
          this.$message.error("成功刷新 " + result.successes.length + " 个节点配置, 刷新 " + result.failures.length + " 个节点失败")
        }
        else {
          this.$message.success("成功刷新 " + result.successes.length + " 个节点配置")
        }
      })
    }
  }
}
</script>

<style  scoped>
.table td, .table th {
    border: 1px solid #dbdbdb;
    border-width: 0 0 1px;
    padding: .5em .75em;
    vertical-align: top;
}
table td, table th {
    text-align: left;
    vertical-align: top;
}
td, th {
    padding: 0;
    text-align: left;
}
*, :after, :before {
    -webkit-box-sizing: inherit;
    box-sizing: inherit;
}
td {
    display: table-cell;
    vertical-align: inherit;
}
</style>

