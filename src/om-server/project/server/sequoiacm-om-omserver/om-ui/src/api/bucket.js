import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
  * 查询桶列表
  * @param {number} page 
  * @param {number} size 
  * @param {object} filter 
  * @param {object} orderby 
  * @returns 
  */
 export function queryBucketList(page, size, filter, orderby, isStrictMode=false) {
  return request({
    url: BASE_API+'/buckets',
    method: 'get',
    params: {
      skip: (page-1)*size,
      limit: size,
      filter: filter,
      orderby: orderby,
      strict_mode: isStrictMode
    }
  })
}

/**
 * 查询用户拥有操作权限的桶
 * @returns 
 */
 export function queryUserRelatedBucket() {
  return request({
    url: BASE_API+'/buckets?action=get_related_buckets',
    method: 'get'
  })
}

/**
 * 查看桶详情
 * @param {string} bucketName 
 * @returns 
 */
 export function queryBucketDetail(bucketName) {
  return request({
    url: BASE_API+'/buckets/'+bucketName,
    method: 'head'
  })
}

/**
 * 查看桶下的文件列表
 * @param {string} bucketName 
 * @param {object} filter 
 * @param {object} orderby 
 * @param {int} page 
 * @param {int} size 
 * @returns 
 */
 export function queryFileInBucket(bucketName, filter, orderby, page, size) {
  return request({
    url: BASE_API+'/buckets/'+bucketName+'/files',
    method: 'get',
    params: {
      filter,
      orderby,
      skip: (page-1)*size,
      limit: size
    }
  })
}

/**
 * 桶内上传文件
 * @param {string} bucketName 
 * @param {string} site 
 * @param {string} fileInfo 
 * @param {object} uploadConf
 * @param {object} param 
 * @returns 
 */
 export function createFileInBucket(bucketName, site, fileInfo, uploadConf, param) {
  return request({
    url: BASE_API+'/buckets/'+bucketName+'/files',
    method: 'post',
    headers: {
      "Content-Type" : "binary/octet-stream",
      "description": fileInfo
    },
    params: {
      bucket: bucketName,
      site_name: site,
      upload_config: uploadConf
    },
    data: param.file,
    onUploadProgress: event=> {
      param.file.percent = event.loaded/event.total*100
      param.onProgress(param.file)
    },
    timeout: -1,
  })
}


/**
 * 创建桶
 * @param {object} data 
 * @returns 
 */
 export function createBucket(data) {
  return request({
    url: BASE_API + '/buckets',
    method: 'post',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: data
  })
}

/**
 * 更新桶
 * @param {object} data 
 * @returns 
 */
 export function updateBucket(bucketName, data) {
  return request({
    url: BASE_API + '/buckets/' + bucketName,
    method: 'put',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: data
  })
}

/**
 * 删除桶
 * @param {array} bucketNames 
 * @returns 
 */
 export function deleteBucket(bucketNames) {
  return request({
    url: BASE_API + '/buckets',
    method: 'delete',
    headers: {
      "Content-Type" : "application/json;charset=UTF-8",
    },
    data: bucketNames
  })
}