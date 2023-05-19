import request from '@/utils/request'

const BASE_API = '/api/v1/lifeCycle'

/**
 * 获取阶段标签列表
 * @param {object} filter 
 * @param {int} page 
 * @param {int} size 
 * @returns stageTagList
 */
export function listStageTag(filter, page, size) {
  return request({
    url: BASE_API + '/stageTag',
    method: 'get',
    params: {
      filter,
      skip: (page-1)*size,
      limit: size
    }
  })
}

/**
 * 创建阶段标签
 * @param {string} name 
 * @param {string} description 
 * @returns 
 */
 export function createStageTag(name, description) {
  return request({
    url: BASE_API + '/stageTag',
    method: 'post',
    params: {
      name: name,
      description: description
    }
  })
}

/**
 * 删除阶段标签
 * @param {string} name 
 * @returns 
 */
 export function deleteStageTag(name) {
  return request({
    url: BASE_API + '/stageTag/' + name,
    method: 'delete'
  })
}

/**
 * 设置站点的阶段标签
 * @param {string} site 
 * @param {string} stageTag 
 * @returns 
 */
 export function addStageTag(site, stageTag) {
  return request({
    url: BASE_API + '/site?action=addStageTag',
    method: 'put',
    params: {
      site_name: site,
      stage_tag: stageTag
    }
  })
}

/**
 * 移除站点的阶段标签
 * @param {string} site 
 * @returns 
 */
 export function removeStageTag(site) {
  return request({
    url: BASE_API + '/site?action=removeStageTag',
    method: 'put',
    params: {
      site_name: site,
    }
  })
}

/**
 * 获取数据流列表
 * @param {object} filter 
 * @param {int} page 
 * @param {int} size 
 * @returns stageTagList
 */
 export function listTransition(transition_filter, page, size) {
  return request({
    url: BASE_API + '/transition',
    method: 'get',
    params: {
      transition_filter,
      skip: (page-1)*size,
      limit: size
    }
  })
}

/**
 * 创建数据流
 * @param {object} transition  
 * @returns 
 */
 export function createTransition(transition) {
  return request({
    url: BASE_API + '/transition',
    method: 'post',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: transition
  })
}

/**
 * 更新数据流
 * @param {string} oldTransition  
 * @param {object} transition  
 * @returns 
 */
 export function updateTransition(oldTransition, transition) {
  return request({
    url: BASE_API + '/transition',
    method: 'put',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    params: {
      old_transition: oldTransition,
    },
    data: transition
  })
}

/**
 * 添加数据流应用的工作区
 * @param {object} transition  
 * @param {array} wsList  
 * @returns 
 */
export function addTransitionApply(transition, wsList) {
  return request({
    url: BASE_API + '/transition/' + transition + '?action=addApply',
    method: 'put',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: wsList,
    timeout: -1
  })
}

/**
 * 移除数据流应用的工作区
 * @param {object} transition  
 * @param {array} wsList  
 * @returns 
 */
 export function removeTransitionApply(transition, wsList) {
  return request({
    url: BASE_API + '/transition/' + transition + '?action=removeApply',
    method: 'put',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: wsList,
    timeout: -1
  })
}

/**
 * 删除数据流
 * @param {string} name 
 * @returns 
 */
 export function deleteTransition(name) {
  return request({
    url: BASE_API + '/transition/' + name,
    method: 'delete'
  })
}

/**
 * 获取工作区下的数据流列表
 * @param {string} workspace 
 */
export function listTransitionByWs(workspace) {
  return request({
    url: BASE_API + '/workspace/transition/' + workspace,
    method: 'get'
  })
}

/**
 * 工作区添加数据流
 */
export function addWsTransition(workspace, transition) {
  return request({
    url: BASE_API + '/workspace/transition/' + workspace,
    method: 'post',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: transition
  })
}

/**
 * 工作区更新数据流
 */
 export function updateWsTransition(workspace, oldTransition, transition) {
  return request({
    url: BASE_API + '/workspace/transition/' + workspace,
    method: 'put',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    params: {
      old_transition: oldTransition,
    },
    data: transition
  })
}

/**
 * 修改数据流的状态
 * @param {string} workspace 
 * @param {string} transition 
 * @param {boolean} isEnabled 
 */
 export function changeTransitionState(workspace, transition, isEnabled) {
  return request({
    url: BASE_API + '/workspace/transition/' + workspace + '?action=changeState',
    method: 'put',
    params: {
      transition: transition,
      is_enabled: isEnabled
    }
  })
}

/**
 * 移除工作区下的数据流
 * @param {string} workspace 
 * @param {string} transition 
 */
 export function removeTransition(workspace, transition) {
  return request({
    url: BASE_API + '/workspace/transition/' + workspace,
    method: 'delete',
    params: {
      transition: transition
    }
  })
}