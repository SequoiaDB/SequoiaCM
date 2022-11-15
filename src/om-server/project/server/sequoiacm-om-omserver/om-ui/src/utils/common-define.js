//  任务类型
export const TASK_TYPES = [
  {
    value: 'clean_file',
    label: '清理任务'
  },
  {
    value: 'copy_file',
    label: '迁移任务（拷贝）'
  },
  {
    value: 'move_file',
    label: '迁移任务（清理源站点）'
  },
  {
    value: 'recycle_space',
    label: '空间回收任务'
  }
]

export const FILE_SCOPE_VAL_CURRENT = 1
export const FILE_SCOPE_VAL_HISTORY = 2
export const FILE_SCOPE_VAL_ALL = 3
// 文件范围
export const FILE_SCOPE_TYPES = [
  {
    value: FILE_SCOPE_VAL_CURRENT,
    label: 'CURRENT'
  },
  {
    value: FILE_SCOPE_VAL_HISTORY,
    label: 'HISTORY'
  },
  {
    value: FILE_SCOPE_VAL_ALL,
    label: 'ALL'
  }
]

// 任务状态
export const TASK_STATUS = [
  {
      name: 'INIT',
      value: 1,
      text: '初始化中',
      color: '#606266'
  },
  {
      name: 'RUNNING',
      value: 2,
      text: '运行中',
      color: '#409EFF'
  },
  {
      name: 'FINISH',
      value: 3,
      text: '已完成',
      color: '#67C23A'
  },
  {
      name: 'CANCEL',
      value: 4,
      text: '已取消',
      color: '#909399'
  },
  {
      name: 'ABORT',
      value: 5,
      text: '异常中止',
      color: '#E6A23C'
  },
  {
      name: 'TIMEOUT',
      value: 6,
      text: '超时退出',
      color: '#F56C6C'
  }
]

export const INSTANCE_STATUS = {
  UP: 'UP',
  DOWN: 'DOWN',
  STOPPED: 'STOPPED'
}

export const JOB_CONFIG_PROPS = [
  {
    key: 'scm.jobManager.threadpool.coreSize',
    value: 8
  },
  {
    key: 'scm.jobManager.threadpool.maxSize',
    value: 10
  }
]

// 系统内置用户，无法删除
export const SYSTEM_ROLES = [
  'ROLE_AUTH_ADMIN',
  'ROLE_AUTH_MONITOR'
]

// 列表查询时在响应头里的数据总条数标识
export const X_RECORD_COUNT = "x-record-count"
