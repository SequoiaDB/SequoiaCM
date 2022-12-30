<template>
  <div>
    <!-- 站点详情对话框 -->
    <el-dialog
      class="detail-dialog"
      title="站点信息"
      :visible.sync="detailDialogVisible"
      width="720px">
      <div class="detail-container">
        <el-row>
          <el-col :span="4"><span class="key">ID</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.id}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="4"><span class="key">站点名称</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.name}}</span></el-col>
        </el-row>
        <el-row>
          <el-col :span="4"><span class="key">是否为主站点</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.is_root_site ? "是" : "否"}}</span></el-col>
        </el-row>
        <el-row >
          <el-col :span="4"><span class="key">数据源类型</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.datasource_type}}</span></el-col>
        </el-row>
        <el-row v-if="curSiteDetail.datasource_url && curSiteDetail.datasource_url.length>0">
          <el-col :span="4"><span class="key">数据源地址</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.datasource_url.join(",")}}</span></el-col>
        </el-row>
        <el-row v-if="curSiteDetail.datasource_user">
          <el-col :span="4"><span class="key">数据源用户</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.datasource_user}}</span></el-col>
        </el-row>
        <el-row v-if="curSiteDetail.datasource_pwd">
          <el-col :span="4"><span class="key">数据源密码文件</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.datasource_pwd}}</span></el-col>
        </el-row>
        <el-row v-if="curSiteDetail.metasource_url && curSiteDetail.metasource_url.length > 0">
          <el-col :span="4"><span class="key">元数据地址</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.metasource_url.join(",")}}</span></el-col>
        </el-row>
        <el-row v-if="curSiteDetail.metasource_user">
          <el-col :span="4"><span class="key">元数据用户</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.metasource_user}}</span></el-col>
        </el-row>
        <el-row v-if="curSiteDetail.metasource_pwd">
          <el-col :span="4"><span class="key">元数据密码文件</span></el-col>
          <el-col :span="20"><span class="value">{{curSiteDetail.metasource_pwd}}</span></el-col>
        </el-row>
        <el-row v-if="curSiteDetail.datasource_conf">
          <el-col :span="4"><span class="key">数据源配置</span></el-col>
          <el-col :span="20">
            <el-input
              type="textarea"
              :value="$util.toPrettyJson(curSiteDetail.datasource_conf)"
              :autosize="{ minRows: 2, maxRows: 10}"
              readonly ></el-input>
          </el-col>
        </el-row>

      </div>

      <span slot="footer" class="dialog-footer">
        <el-button id="btn_file_detail_close" @click="close()" size="mini">关 闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  setup() {
  },
  props: {
    curSiteDetail: Object,
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

<style  scoped>
.detail-dialog >>> .el-row {
  margin-top: 12px !important;
}
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
</style>
