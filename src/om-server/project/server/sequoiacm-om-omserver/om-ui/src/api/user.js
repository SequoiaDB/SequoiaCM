import request from '@/utils/request'
import { encrypt } from '@/utils/auth'

const BASE_API = '/api/v1'

/**
 * 用户登录
 * @param {object} params obj 
 * @returns Promise
 */
export function login(params) {
  return request({
    url: '/login',
    method: 'post',
    data: params
  })
}

/**
 * 获取用户信息
 * @param {string} username 
 * @returns 
 */
export function getInfo(username) {
  return request({
    url: BASE_API + '/users/' + username,
    method: 'get'
  })
}

/**
 * 获取用户列表
 * @param {object} user_filter 
 * @param {int} page 
 * @param {int} size 
 * @returns userList
 */
export function listUsers(user_filter, page, size) {
  return request({
    url: BASE_API + '/users/',
    method: 'get',
    params: {
      user_filter,
      skip: (page-1)*size,
      limit: size
    }
  })
}

/**
 * 创建用户
 * @param {string} username 
 * @param {string} password 
 * @returns 
 */
 export function createUser(username, password) {
  return request({
    url: BASE_API + '/users/' + encodeURIComponent(username),
    method: 'post',
    params: {
      password: encrypt(password)
    }
  })
}

/**
 * 修改用户密码
 * @param {string} username 
 * @param {string} password 
 * @returns 
 */
 export function updatePwd(username, oldPwd, newPwd) {
  return request({
    url: BASE_API + '/users/' + encodeURIComponent(username) + '?action=change_password',
    method: 'put',
    params: {
      old_password: encrypt(oldPwd),
      new_password: encrypt(newPwd)
    }
  })
}

/**
 * 添加用户角色
 * @param {string} username
 * @param {array} roles 
 * @returns 
 */
 export function grantRoles(username, roles) {
  return request({
    url: BASE_API + '/users/' + encodeURIComponent(username) + '?action=grant_role',
    headers: {
      "Content-Type" : "application/json"
    },
    method: 'put',
    data: roles
  })
}

/**
 * 移除用户角色
 * @param {string} username
 * @param {array} roleList 
 * @returns 
 */
 export function revokeRoles(username, roles) {
  return request({
    url: BASE_API + '/users/' + encodeURIComponent(username) + '?action=revoke_role',
    headers: {
      "Content-Type" : "application/json"
    },
    method: 'put',
    data: roles
  })
}

/**
 * 启用用户
 * @param {string} username
 * @returns 
 */
 export function enableUser(username) {
  return request({
    url: BASE_API + '/users/' + encodeURIComponent(username) + '?action=enable',
    method: 'put'
  })
}

/**
 * 禁用用户
 * @param {string} username
 * @returns 
 */
 export function disableUser(username, roleList) {
  return request({
    url: BASE_API + '/users/' + encodeURIComponent(username) + '?action=disable',
    method: 'put'
  })
}

/**
 * 删除用户
 * @param {string} username 
 * @returns 
 */
 export function deleteUser(username) {
  return request({
    url: BASE_API + '/users/' + encodeURIComponent(username),
    method: 'delete'
  })
}

/**
 * 登出
 * @returns 
 */
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}
