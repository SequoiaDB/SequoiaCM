<template>
  <div class="app-container">
    <!-- 表格部分 -->
    <el-table
        :data="tableData"
        border
        row-key="oid"
        style="width: 100%">
        <el-table-column type="index"       label="序号"      > </el-table-column>
        <el-table-column prop="name"        label="工作区名称"  ></el-table-column>
        <el-table-column prop="site_list"   label="站点列表"></el-table-column>
        <el-table-column prop="create_user" label="创建人" > </el-table-column>
        <el-table-column prop="create_time" label="创建时间"> </el-table-column>
        <el-table-column prop="description" label="描述信息" ></el-table-column>
    </el-table>
  </div>
</template>

<script>
import {queryWorkspaceList} from '@/api/test'
export default {
  data(){
    return {
      tableData: []
    }
  },
  // 生命周期函数，让vue在创建时发送请求，获取后端数据
  created() {
    queryWorkspaceList(1,10).then(res => {
      this.tableData = res.data
    })
  }
}
</script>

<style scoped>

</style>