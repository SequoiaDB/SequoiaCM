import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 获取标签列表
 * @param {string} ws 
 * @param {string} tag_type 
 * @param {object} tag_filter 
 * @param {int} page 
 * @param {int} size 
 * @returns tagList
 */
 export function listTag(ws, tag_type, tag_filter, page, size) {
  return request({
    url: BASE_API + '/tags/' + tag_type,
    method: 'get',
    params: {
      workspace: ws,
      tag_filter,
      skip: (page-1)*size,
      limit: size
    }
  })
}

/**
 * 获取自由标签的 key 列表
 * @param {string} ws 
 * @param {string} key_matcher 
 * @param {int} page 
 * @param {int} size 
 * @returns keyList
 */
 export function listCustomTagKey(ws, keyMatcher, page, size) {
  return request({
    url: BASE_API + '/tags/custom_tag/key',
    method: 'get',
    params: {
      workspace: ws,
      key_matcher: keyMatcher,
      skip: (page-1)*size,
      limit: size
    }
  })
}