import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 查询工作区列表
 * @param {number} page 
 * @param {number} size 
 * @param {object} filter 
 * @param {boolean} isStrictMode
 * @returns 
 */
export function queryWorkspaceList(page, size, filter, isStrictMode) {
  return request({
    url: BASE_API+'/workspaces',
    method: 'get',
    params: {
      skip: (page-1)*size,
      limit: size,
      filter: filter,
      strict_mode: isStrictMode
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

/**
 * 更新工作区
 * @param {string} workspaceName 
 * @param {object} workspace 
 * @returns 
 */
 export function updateWorkspace(workspaceName, workspace) {
  return request({
    url: BASE_API+'/workspaces/'+workspaceName,
    method: 'put',
    data: workspace
  })
}

