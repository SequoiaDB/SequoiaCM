<template>
  <div class="app-container">
      <div class="info-container">
        <div class="title">基本信息</div>
        <el-row>
          <el-col :span="24"><span class="key">工作区名称：</span> <span class="value">{{workspaceInfo.name}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><span class="key">工作区描述：</span> <span class="value">{{workspaceInfo.description}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">创建人：</span> <span class="value">{{workspaceInfo.create_user}}</span></el-col>
          <el-col :span="12"><span class="key">创建时间：</span> <span class="value">{{workspaceInfo.create_time|parseTime}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">更新人：</span> <span class="value">{{workspaceInfo.update_user}}</span></el-col>
          <el-col :span="12"><span class="key">更新时间：</span> <span class="value">{{workspaceInfo.update_time|parseTime}}</span></el-col>
        </el-row>
      </div>
      <el-divider></el-divider>
      <div class="info-container">
        <div class="title">站点信息</div>
        <el-table
          :data="siteList"
          border
          style="width: 100%">
          <el-table-column
            type="index"
            label="序号"
            width="55">
          </el-table-column>
          <el-table-column
            prop="site_name"
            label="站点名称"
            width="180">
          </el-table-column>
          <el-table-column
            prop="site_type"
            label="数据源类型"
            width="180">
          </el-table-column>
          <el-table-column
            label="站点配置">
            <template slot-scope="scope">
              {{scope.row.options}}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-divider></el-divider>
      <div class="info-container">
        <div class="title">元数据信息</div>
        <div class="code">
          <el-input
            type="textarea"
            readonly
            :rows="2"
            autosize
            :value="$util.toPrettyJson(workspaceInfo.meta_options)">
          </el-input>
        </div>
      </div>
      <el-divider></el-divider>
      <div class="info-container">
        <div class="title">统计信息</div>
        <el-row>
          <el-col :span="12"><span class="key">目录数：</span> <span class="value"><el-tag size="small" >{{workspaceInfo.directory_count}}</el-tag></span></el-col>
          <el-col :span="12"><span class="key">文件数：</span> <span class="value"><el-tag size="small">{{workspaceInfo.file_count}}</el-tag></span></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><span class="key">批次数：</span> <span class="value"><el-tag size="small">{{workspaceInfo.batch_count}}</el-tag></span></el-col>
        </el-row>
      </div>
  </div>
</template>
<script>
import {queryWorkspaceDetail} from '@/api/workspace'
export default {
  data(){
    return{
      workspaceInfo: {},
      siteList: [],
    }
  },
  computed:{
   
  },
  methods:{
    //初始化
    init(){
      this.queryWorkspaceDetail();
    },
    async queryWorkspaceDetail(){
      let res = await queryWorkspaceDetail(this.$route.params.name)
      this.workspaceInfo = res.data
      if(this.workspaceInfo && this.workspaceInfo.data_locations){
        this.siteList = this.workspaceInfo.data_locations
      }
    }
  },
  created(){
    this.init()
  }
  
}
</script>
<style  scoped>
.title {
  font-size: 16px;
  height: 20px;
  line-height: 20px;
  margin-bottom: 10px;
  font-weight: 600;
  text-indent: 3px;
  border-left: 5px solid #409EFF;
}
.info-container >>> .el-row {
  margin-top: 5px !important;
}
.key {
  font-size: 13px;
  font-weight: 600;
  opacity: 0.8;
}
.value {
  font-size: 14px;
  color: #606266;
}
.code {
  padding-left: 10px;
  font-size: 14px;
  color: #606266;

}
</style>