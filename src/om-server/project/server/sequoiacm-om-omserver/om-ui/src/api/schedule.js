import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 查询任务调度列表
 * @param {number} page 
 * @param {number} size 
 * @param {string} filter 
 * @returns 
 */
export function queryScheduleList(page, size, filter) {
  return request({
    url: BASE_API+'/schedules',
    method: 'get',
    params: {
      skip: (page-1)*size,
      limit: size,
      filter
    }
  })
}

/**
 * 查看调度任务详情
 * @param {string} scheduleId 
 * @returns 
 */
export function queryScheduleDetail(scheduleId) {
  return request({
    url: BASE_API+'/schedules/'+scheduleId,
    method: 'get'
  })
}

/**
 * 创建任务调度
 * @param {object} schedule 
 * @returns 
 */
 export function createSchedule(schedule) {
  return request({
    url: BASE_API+'/schedules',
    method: 'put',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: schedule
  })
}


/**
 * 修改调度任务
 * @param {object} schedule 
 * @returns 
 */
 export function updateSchedule(scheduleId, schedule) {
  return request({
    url: BASE_API+'/schedules/'+scheduleId,
    method: 'post',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: schedule
  })
}

/**
 * 删除调度任务
 * @param {string} scheduleId 
 * @returns 
 */
 export function deleteSchedule(scheduleId) {
  return request({
    url: BASE_API+'/schedules/'+scheduleId,
    method: 'delete'
  })
}


/**
 * 查询调度任务运行记录
 * @param {object} filter 
 * @param {number} page 
 * @param {number} size 
 * @param {object} orderBy 
 * @returns 
 */
 export function queryTasks(scheduleId, filter, page, size, orderBy) {
  return request({
    url: BASE_API+'/schedules/tasks',
    method: 'get',
    params: {
      schedule_id: scheduleId,
      filter: filter,
      skip: size * (page-1),
      limit: size,
      orderby: orderBy
    }
  })
}
