import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 查询工作区列表
 * @param {number} page 
 * @param {number} size 
 * @param {object} filter 
 * @returns 
 */
export function queryWorkspaceList(page, size, filter) {
  return request({
    url: BASE_API+'/workspaces',
    method: 'get',
    params: {
      skip: (page-1)*size,
      limit: size,
      filter: filter
    }
  })
}

/**
 * 查询工作区详情
 * @param {string} workspaceName 
 * @returns 
 */
export function queryWorkspaceDetail(workspaceName) {
  return request({
    url: BASE_API+'/workspaces/'+workspaceName,
    method: 'get'
  })
}

/**
 * 查询工作区基本信息
 * @param {string} workspaceName 
 * @returns 
 */
 export function queryWorkspaceBasic(workspaceName) {
  return request({
    url: BASE_API+'/workspaces/'+workspaceName,
    method: 'head'
  })
}

