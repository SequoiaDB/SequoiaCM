import request from '@/utils/request'

const BASE_API = '/api/v1/monitor'

/**
 * 获取节点列表
 */
export function queryInstanceList() {
  return request({
    url: BASE_API+'/instances',
    method: 'get'
  })
}

/**
 * 
 * @returns 获取om版本号
 */
export function getVersion() {
  return request({
    url: BASE_API + '/version',
    method: 'get'
  })
}

/**
 * 获取指定节点信息
 */
 export function getInstanceInfo(instanceId) {
  return request({
    url: BASE_API + '/instances/' + encodeURIComponent(instanceId),
    method: 'get'
  })
}

/**
 * 移除指定节点
 */
 export function deleteInstance(instanceId) {
  return request({
    url: BASE_API + '/instances/' + encodeURIComponent(instanceId),
    method: 'delete'
  })
}

/**
 * 获取节点连接信息
 */
 export function getConnectionInfo(instanceId) {
  return request({
    url: BASE_API + '/instances/' + encodeURIComponent(instanceId) + '?action=getConnectionInfo',
    method: 'get'
  })
}

/**
 * 获取线程接信息
 */
 export function getThreadInfo(instanceId) {
  return request({
    url: BASE_API + '/instances/' + encodeURIComponent(instanceId) + '?action=getThreadInfo',
    method: 'get'
  })
}

/**
 * 获取堆内存信息
 */
 export function getHeapInfo(instanceId) {
  return request({
    url: BASE_API+'/instances/' + encodeURIComponent(instanceId) + '?action=getHeapInfo',
    method: 'get'
  })
}

/**
 * 获取进程信息
 */
 export function getProcessInfo(instanceId) {
  return request({
    url: BASE_API+'/instances/' + encodeURIComponent(instanceId) + '?action=getProcessInfo',
    method: 'get'
  })
}


/**
 * 获取配置信息
 */
 export function getConfigInfo(instanceId) {
  return request({
    url: BASE_API+'/instances/' + encodeURIComponent(instanceId) + '?action=getConfigInfo',
    method: 'get'
  })
}