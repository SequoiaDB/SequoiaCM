import request from '@/utils/request'

const BASE_API = '/api/v1'



/**
 * 停止指定正在运行中的任务
 * @param {string} taskId 
 * @returns 
 */
 export function stopTask(taskId) {
  return request({
    url: BASE_API+'/tasks/' + taskId + '?action=stop_task',
    method: 'post'
  })
}


