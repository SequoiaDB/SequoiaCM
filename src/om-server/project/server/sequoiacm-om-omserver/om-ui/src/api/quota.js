import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 开启桶限额
 * @param {string} bucket 桶名
 * @param {number} maxObjects 最大对象数
 * @param {string} maxSize 最大容量
 * @returns
 */
 export function enableBucketQuota(bucket, maxObjects, maxSize) {
  return request({
    url: BASE_API + '/quotas/bucket/' + bucket + '?action=enable_quota',
    method: 'PUT',
    params: {
      max_objects: maxObjects,
      max_size: maxSize
    }
  })
}


/**
 * 更新桶限额
 * @param {string} bucket 桶名
 * @param {number} maxObjects 最大对象数
 * @param {string} maxSize 最大容量
 * @returns
 */
 export function updateBucketQuota(bucket, maxObjects, maxSize) {
  return request({
    url: BASE_API + '/quotas/bucket/' + bucket + '?action=update_quota',
    method: 'PUT',
    params: {
      max_objects: maxObjects,
      max_size: maxSize
    }
  })
}

/**
 * 获取桶限额
 * @param {string} bucket 桶名
 * @returns
 */
 export function getBucketQuota(bucket) {
  return request({
    url: BASE_API + '/quotas/bucket/' + bucket,
    method: 'GET'
  })
}


/**
 * 禁用桶限额
 * @param {string} bucket 桶名
 * @returns
 */
 export function disableBucketQuota(bucket) {
  return request({
    url: BASE_API + '/quotas/bucket/' + bucket + '?action=disable_quota',
    method: 'PUT'
  })
}

/**
 * 同步桶限额
 * @param {string} bucket 桶名
 * @returns
 */
 export function syncBucketQuota(bucket) {
  return request({
    url: BASE_API + '/quotas/bucket/' + bucket + '?action=sync',
    method: 'POST'
  })
}


/**
 * 取消同步桶限额
 * @param {string} bucket 桶名
 * @returns
 */
 export function cancelSyncBucketQuota(bucket) {
  return request({
    url: BASE_API + '/quotas/bucket/' + bucket + '?action=cancel_sync',
    method: 'POST'
  })
}
