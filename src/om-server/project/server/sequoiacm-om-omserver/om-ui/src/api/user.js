import request from '@/utils/request'


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
    url: '/api/v1/users/'+username,
    method: 'get'
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
