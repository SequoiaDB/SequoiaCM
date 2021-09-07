import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 查询region列表
 * @returns 
 */
export function queryRegionList() {
  return request({
    url: BASE_API+'/services/regions',
    method: 'get'
  })
}

/**
 * 查询zones列表，如不指定region，查询全部
 * @param {string} region 
 * @returns 
 */
 export function queryZoneList(region) {
  return request({
    url: BASE_API+'/services/zones',
    method: 'get',
    params: {
      region
    }
  })
}


