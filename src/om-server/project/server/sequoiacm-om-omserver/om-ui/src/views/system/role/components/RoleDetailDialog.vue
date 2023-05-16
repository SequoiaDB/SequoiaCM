<template>
  <div>
    <!-- 查看角色详情对话框 -->
    <el-dialog
      title="角色详情"
      :visible.sync="detailDialogVisible"
      width="800px">
      <el-row>
        <el-col :span="3"><span class="key">角色 ID</span></el-col>
        <el-col :span="21"><span class="value">{{role.role_id}}</span></el-col>
      </el-row>
      <el-row>
        <el-col :span="3"><span class="key">角色名</span></el-col>
        <el-col :span="21"><span class="value">{{role.role_name}}</span></el-col>
      </el-row>
      <el-row v-if="role.description !== ''">
        <el-col :span="3"><span class="key">角色描述</span></el-col>
        <el-col :span="21"><span class="value">{{role.description}}</span></el-col>
      </el-row>
      <el-row>
        <el-col :span="3"><span class="key">权限列表</span></el-col>
        <el-col :span="21">
          <el-table 
          border
            :data="role.privileges"
            empty-text="该角色暂无权限" > 
            <el-table-column prop="resource_type" label="资源类型" width="120" /> 
            <el-table-column prop="resource_name" label="资源名称" width="180" />
            <el-table-column
                label="权限列表">
                <template slot-scope="scope">
                    <el-tag
                        size="mini"
                        v-for="item of scope.row.privileges"
                        :key="item"
                        style="margin-right:0.2rem">
                        <el-tooltip content="系统内置权限类型" :disabled="item!=='LOW_LEVEL_READ'" placement="top-start" >
                          <template>
                            <span> {{item}} </span>
                          </template>
                        </el-tooltip>
                    </el-tag>
                </template>
            </el-table-column>
          </el-table> 
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
export default {
  props: { 
    role: { 
      type: Object 
    } 
  },
  data() {
    return{
      detailDialogVisible: false,
    }
  },
  methods: {
    show() {
      this.detailDialogVisible = true
    },
    close() {
      this.detailDialogVisible = false
    }
  }
}
</script>
<style scoped>
.key {
  font-size: 14px;
  font-weight: 600;
  color: #888;
}
.value {
  font-size: 14px;
  overflow: hidden; 
  color: #606266;
}
.el-row {
  margin-top: 20px !important;
}
</style>