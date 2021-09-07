import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 查询站点列表
 * @returns 
 */
export function querySiteList() {
  return request({
    url: BASE_API+'/sites',
    method: 'get'
  })
}

/**
 * 查询站点网络模型
 * @returns 
 */
 export function querySiteStrategy() {
  return request({
    url: BASE_API+'/sites/strategy',
    method: 'get'
  })
}


