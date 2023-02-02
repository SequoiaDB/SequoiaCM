<template>
  <div>
    <el-dialog
      :title="this.stageTag + ' 添加应用站点'"
      :visible.sync="stageTagApplySiteDialog"
      width="600px">
        <el-form ref="form" :model="form" size="small" label-width="150px">
          <el-form-item label="选择站点" prop="site">
            <el-select
                v-model="form.site" 
                placeholder="请选择需要应用的站点" 
                size="medium"
                width="250">
                <el-option
                  v-for="item in freeSiteList"
                  :key="item"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
          </el-form-item>
        </el-form>
        <span slot="footer" class="dialog-footer">
          <el-button @click="close" size="mini">关 闭</el-button>
          <el-button @click="submit" type="primary" :disabled="form.site===''" size="mini">添 加</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { querySiteList } from "@/api/site"
import { addStageTag } from "@/api/lifecycle"
export default {
  data() {
    return{
      stageTagApplySiteDialog: false,
      freeSiteList: [],
      stageTag: '',
      form: {
        site: ''
      },
    }
  },
  methods: {
    submit() { 
      addStageTag(this.form.site, this.stageTag).then(res => {
        this.$message.success(`阶段标签【${this.stageTag}】关联站点【${this.form.site}】成功`)
        this.close()
        this.$emit('onStageTagChanged')
      })
    },
    show(stagetTag) {
      this.stageTag = stagetTag
      // 初始化空闲的（无阶段标签）的站点列表
      this.freeSiteList = []
      querySiteList().then(res => {
        let siteList = res.data
        if (siteList && siteList.length > 0) {
          for (let site of siteList) {
            if (!site.stage_tag || site.staget_tag === '') {
              this.freeSiteList.push(site.name)
            }
          }
        }
      }),
      this.stageTagApplySiteDialog = true
    },
    close() {
      this.clear()
      this.stageTagApplySiteDialog = false
    },
    clear() {
      setTimeout(()=>{
        // 重置表单
        this.clearForm()
      }, 500)
    },
    clearForm() {
      if (this.$refs['form']) {
        this.$refs['form'].resetFields()
      }
    }
  }
}
</script>