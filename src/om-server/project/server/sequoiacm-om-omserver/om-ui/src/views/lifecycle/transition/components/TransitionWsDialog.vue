<template>
  <div>
    <!-- 修改数据流生效范围对话框 -->
    <el-dialog
      title="修改生效范围"
      :visible.sync="detailDialogVisible"
      width="750px">
        <el-transfer
          filterable
          filter-placeholder="请输入工作区名称"
          v-model="hasWs"
          :titles="['可选工作区', '已应用工作区']"
          :button-texts="['移除', '添加']"
          :data="allWs"
          @change="handleChange"
          style="margin-left: 50px;">
          <div slot-scope="{option}">
            <el-tooltip content="该工作区使用数据流的副本，无法直接移除！需要在工作区管理中，对应工作区的详情页移除数据流！" :disabled="!option.disabled" placement="top">
              <span>{{option.value}}</span>
            </el-tooltip>
          </div>
        </el-transfer>
        <span slot="footer" class="dialog-footer" style="border:1px soild red">
          <el-button id="btn_create_close" @click="close" size="mini">关 闭</el-button>
        </span>
    </el-dialog>
  </div>
</template>

<script>
import { addTransitionApply, removeTransitionApply } from "@/api/lifecycle"
import { queryWorkspaceList } from '@/api/workspace'
export default {
  data() {
    return{
      transition: '',
      detailDialogVisible: false,
      hasWs: [],
      allWs: [],
    }
  },
  methods: {
    // 加载所有工作区
    initWorkspaceList(wsCustomList) {
      this.allWs = []
      queryWorkspaceList(1, -1, null).then(res => {
        let wsList = res.data
        wsList.forEach(ele => {
          let isWorkspaceCustomTransition = wsCustomList.some(item => item === ele.name)
          this.allWs.push({ key: ele.name, label: ele.name, value: ele.name, disabled: isWorkspaceCustomTransition });
        });
      })
    },
    handleChange(value, direction, movedKeys) {
      if (direction === 'right') {
        addTransitionApply(this.transition, movedKeys).then(res=>{
          let resList = res.data
          this.$util.showBatchOpMessage("应用工作区", resList)
          // 如果存在添加失败的工作区，穿梭框需要做回滚
          resList.forEach(ele => {
            if (!ele.success) {
              this.hasWs.splice(this.hasWs.indexOf(ele.name), 1)
            }
          })
        })
      } else {
        removeTransitionApply(this.transition, movedKeys).then(res=>{
          let resList = res.data
          this.$util.showBatchOpMessage("移除工作区", resList)
          // 如果存在移除失败的工作区，穿梭框需要做回滚
          resList.forEach(ele => {
            if (!ele.success) {
              this.hasWs.push(ele.name)
            }
          })
        })
      }
      this.$emit('onWsChange')
    },
    show(transition, wsList, wsCustomList) {
      this.transition = transition
      this.hasWs = []
      this.hasWs.push(...wsList)
      this.hasWs.push(...wsCustomList)
      // 自定义数据流的工作区，不允许在此处移除
      this.initWorkspaceList(wsCustomList)
      setTimeout(()=>{this.detailDialogVisible = true})
    },
    close() {
      this.detailDialogVisible = false
    }
  }
}
</script>