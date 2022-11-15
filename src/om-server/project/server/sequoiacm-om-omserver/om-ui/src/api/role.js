import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 获取角色列表
 * @param {object} filter 
 * @param {int} page 
 * @param {int} size 
 * @returns roleList
 */
export function listRoles(filter, page, size) {
  return request({
    url: BASE_API + '/roles',
    method: 'get',
    params: {
      filter,
      skip: (page-1)*size,
      limit: size
    }
  })
}

/**
 * 创建角色
 * @param {string} rolename 
 * @param {string} description 
 * @returns 
 */
 export function createRole(rolename, description) {
  return request({
    url: BASE_API + '/roles/' + rolename,
    method: 'post',
    params: {
      description: description
    }
  })
}

/**
 * 获取角色权限列表
 * @param {string} rolename 
 * @returns 
 */
 export function listPrivilegesByRole(rolename) {
  return request({
    url: BASE_API + '/roles/' + rolename + '?action=list_privilege',
    method: 'get'
  })
}

/**
 * 添加角色权限
 * @param {string} rolename 
 * @param {string} resourceType
 * @param {string} resourceName
 * @param {string} privilegeType
 * @returns 
 */
 export function grantPrivilege(rolename, resourceType, resourceName, privilegeType) {
  return request({
    url: BASE_API + '/roles/' + rolename + '?action=grant',
    method: 'put',
    params: {
      resource_type: resourceType,
      resource: resourceName,
      privilege: privilegeType
    }
  })
}

/**
 * 移除角色权限
 * @param {string} rolename 
 * @param {string} resourceType
 * @param {string} resourceName
 * @param {string} privilegeType
 * @returns 
 */
 export function revokePrivilege(rolename, resourceType, resourceName, privilegeType) {
  return request({
    url: BASE_API + '/roles/' + rolename + '?action=revoke',
    method: 'put',
    params: {
      resource_type: resourceType,
      resource: resourceName,
      privilege: privilegeType
    }
  })
}

/**
 * 删除角色
 * @param {string} rolename 
 * @returns 
 */
 export function deleteRole(rolename) {
  return request({
    url: BASE_API + '/roles/' + rolename,
    method: 'delete'
  })
}