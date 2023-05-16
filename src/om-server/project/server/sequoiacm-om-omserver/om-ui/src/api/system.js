import request from '@/utils/request'

const BASE_API = '/api/v1/system'

/**
 * 获取全局配置
 * @param {string} configName 
 * @returns 
 */
export function getGlobalConfig(configName) {
  return request({
    url: BASE_API+'/globalConfig',
    method: 'get',
    params: {
      config_name: configName
    }
  })
}

/**
 * 设置全局配置
 * @param {string} configName 
 * @param {string} configValue
 * @returns 
 */
 export function setGlobalConfig(configName, configValue) {
  return request({
    url: BASE_API+'/globalConfig',
    method: 'put',
    params: {
      config_name: configName,
      config_value: configValue
    }
  })
}
