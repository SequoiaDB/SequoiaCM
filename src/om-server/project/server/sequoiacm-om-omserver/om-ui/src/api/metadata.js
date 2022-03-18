import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 查看元数据模型列表
 * @param {string} ws
 * @param {object} filter
 * @param {object} orderby
 * @param {int} page
 * @param {int} size
 * @returns
 */
export function queryClassList(ws, filter, orderby, page, size) {
  return request({
    url: BASE_API+'/metadatas/classes',
    method: 'get',
    params: {
      workspace: ws,
      filter,
      orderby,
      skip: (page-1)*size,
      limit: size
    }
  })
}

/**
 * 查看元数据模型详情
 * @param {string} ws 
 * @param {string} classId 
 * @returns 
 */
export function queryClassDetail(ws, classId) {
  return request({
    url: BASE_API+'/metadatas/classes/' + classId,
      method: 'get',
      params: {
        workspace: ws
      }
    })
  }