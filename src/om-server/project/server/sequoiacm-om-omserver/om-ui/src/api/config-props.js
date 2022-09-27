import request from '@/utils/request'

const BASE_API = '/api/v1/config-props'

/**
 * 动态修改配置
 * @param {object} confParam 
 * @returns 
 */
export function updateProperties(confParam) {
  return request({
    url: BASE_API,
    method: 'put',
    data: confParam
  })
}
