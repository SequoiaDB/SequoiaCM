<template>
  <div>
    <el-tree
      ref="dirTree"
      node-key="id"
      :props="dirTreeProps"
      v-if="isDirTreeShow"
      highlight-current
      lazy
      :load="loadDirectory"
      @node-click="handleDirNodeClick">
        <span slot-scope="{ node }">
          <template>
            <i :class="{
              'el-icon-folder': !node.expanded,
              'el-icon-folder-opened' : node.expanded,
              'el-icon-more' : node.data.name === '加载更多'
              }"
            />
            <span style="font-size:15px">{{ " " + node.label }}</span>
          </template>
        </span>
    </el-tree>
  </div>
</template>

<script>
import {getSubDirList} from '@/api/directory'
export default {
  name: 'DirectoryTree',
  props: {
    currentWorkspace: {
      type: String,
      default: ''
    },
    isDirTreeShow: {
      type: Boolean,
      default: false
    },
  },
  data() {
    return {
      dirTreeProps: {
        label: 'name',
        isLeaf: (data)=>{
          return data.sub_dir_count === 0;
        }
      },
      currentDir: {
        id: '',
        path: ''
      }
    }
  },
  methods: {
    setCurrentKey(key) {
      this.$refs['dirTree'].setCurrentKey(key)
    },
    // 目录点击
    handleDirNodeClick(data, node) {
      if (data.name === '加载更多') {
        // 点击加载更多时，当前选中的目录不改变
        if (this.currentDir.id !== '') {
          this.$refs['dirTree'].setCurrentKey(this.currentDir.id)
        } else {
          this.$refs['dirTree'].setCurrentKey(null)
        }
        // 首次点击时从第二页开始加载
        let page = node.parent.page
        node.parent.page = page ? page+1 : 2
        getSubDirList(this.currentWorkspace, node.parent.data.id, node.parent.page, 50).then(res => {
          let dirList = res.data
          dirList.forEach(dir => {
            this.$refs['dirTree'].append(dir, node.parent)
          });
          this.$message.success("成功加载了 " + dirList.length + " 条目录")
          if (dirList.length >= 50) {
            this.appendLoadMore(node.parent)
          }
          this.$refs['dirTree'].remove(data)
        })
      } else {
        if (this.currentDir.id == this.$refs['dirTree'].getCurrentKey()) {
          // 同一个目录被重复点击时，取消选中
          this.$refs['dirTree'].setCurrentKey(null);
          this.currentDir.id = ''
          this.currentDir.path = ''
        } else {
          this.setCurrentKey(data.id)
          this.currentDir.id = data.id
          this.currentDir.path = data.path
        }
        this.$emit('changeSelectDir', this.currentDir)
        this.$emit('closeDirSelectionBox')
      }
    },
    // 懒加载目录
    loadDirectory(node, resolve) {
      if (node.level === 0) {
        getSubDirList(this.currentWorkspace, '-1', 1, -1).then(res => {
          resolve(res.data)
          // 默认选中根目录
          if (res.data && res.data[0]) {
            this.handleDirNodeClick(res.data[0], node)
          }
        })
      } else {
        getSubDirList(this.currentWorkspace, node.data.id, 1, 50).then(res => {
          resolve(res.data)
          if (res.data.length === 50) {
            this.appendLoadMore(node)
          }
        })
      }
    },
    appendLoadMore(node) {
      let data = {
        id : '_' + this.$util.randomStr(23),
        name: "加载更多",
        sub_dir_count: 0
      }
      this.$refs['dirTree'].append(data, node)
    }
  }

}
</script>

<style scoped>
.el-input{
  border-radius:0;
  margin-top: 10px;
}
.el-tree{
  padding: 10px 0px 10px 0px;
  min-width: 100%;
  display: inline-block;
}
::v-deep .el-tree--highlight-current .el-tree-node.is-current>.el-tree-node__content {
  background-color: #409EFF !important;
  color: #fff
}
.tree {
  overflow: auto;
}
</style>
