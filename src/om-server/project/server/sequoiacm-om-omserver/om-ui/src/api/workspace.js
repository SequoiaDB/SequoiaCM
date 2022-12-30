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

/**
 * 获取工作区上传下载请求流量数据
 * @param {string} workspaceName 
 * @param {number} beginTime 
 * @param {number} endTime 
 * @returns 
 */
 export function queryWorkspaceTraffic(workspaceName, beginTime, endTime) {
  return request({
    url: BASE_API + '/workspaces/' + workspaceName + "?action=getTraffic",
    method: 'get',
    params: {
      workspace: workspaceName,
      begin_time: beginTime,
      end_time: endTime
    }
  })
}

/**
 * 获取工作区文件增量数据
 * @param {string} workspaceName 
 * @param {number} beginTime 
 * @param {number} endTime 
 * @returns 
 */
 export function queryWorkspaceFileDelta(workspaceName, beginTime, endTime) {
  return request({
    url: BASE_API + '/workspaces/' + workspaceName + "?action=getFileDelta",
    method: 'get',
    params: {
      workspace: workspaceName,
      begin_time: beginTime,
      end_time: endTime
    }
  })
}

/**
 * 获取用户拥有CREATE权限的工作区列表
 * @returns 
 */
 export function queryCreatePrivilegeWsList() {
  return request({
    url: BASE_API + '/workspaces'+ "?action=getCreatePrivilegeWs",
    method: 'get'
  })
}


/**
 * 删除工作区
 * @param {array} workspaceNames 
 * @returns 
 */
 export function deleteWorkspaces(workspaceNames, isForce) {
  return request({
    url: BASE_API + '/workspaces',
    method: 'delete',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    params: {
      is_force: isForce
    },
    data: workspaceNames
  })
}

/**
 * 创建工作区
 * @param {object} data 
 * @returns 
 */
 export function createWorkspace(data) {
  return request({
    url: BASE_API + '/workspaces',
    method: 'post',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: data
  })
}


