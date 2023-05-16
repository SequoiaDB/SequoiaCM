import request from '@/utils/request'
import { getToken } from '@/utils/auth';

const BASE_API = '/api/v1'

/**
 * 查看文件列表
 * @param {string} ws
 * @param {int} scope
 * @param {object} tag_condition
 * @param {object} filter
 * @param {object} orderby
 * @param {int} page
 * @param {int} size
 * @returns
 */
 export function queryFileList(ws, scope, tag_condition, filter, orderby, page, size) {
  return request({
    url: BASE_API+'/files',
    method: 'get',
    params: {
      workspace: ws,
      scope: scope,
      tag_condition,
      filter,
      orderby,
      skip: (page-1)*size,
      limit: size
    }
  })
}

/**
 * 查看文件详情
 * @param {string} ws
 * @param {string} fileId
 * @param {int} majorVersion
 * @param {int} minorVersion
 * @returns
 */
 export function queryFileDetail(ws, fileId, majorVersion, minorVersion) {
  return request({
    url: BASE_API+'/files/id/'+fileId,
    method: 'head',
    params: {
      workspace: ws,
      major_version: majorVersion,
      minor_version: minorVersion
    }
  })
}

/**
 * 上传文件
 * @param {string} ws
 * @param {string} site
 * @param {string} fileInfo
 * @param {object} uploadConf
 * @param {object} param
 * @returns
 */
export function uploadFile(ws, site, fileInfo, uploadConf, param, cancelToken) {
  return request({
    url: BASE_API+'/files',
    method: 'post',
    headers: {
      "Content-Type" : "binary/octet-stream",
      "description": fileInfo
    },
    params: {
      workspace: ws,
      site_name: site,
      upload_config: uploadConf
    },
    data: param.file,
    onUploadProgress: event=> {
      param.file.percent = event.loaded/event.total*100
      param.onProgress(param.file)
    },
    cancelToken: cancelToken,
    timeout: -1,
  })
}

/**
 * 更新文件内容
 * @param {string} ws
 * @param {string} fileId
 * @param {String} site
 * @param {object} updateOption
 * @param {object} param
 * @returns
 */
export function updateFileContent(ws, fileId, site, updateOption, param) {
  return request({
    url: BASE_API+'/files/id/'+fileId+'/content',
    method: 'put',
    headers: {
      "Content-Type" : "binary/octet-stream",
    },
    params: {
      workspace: ws,
      site_name: site,
      update_content_option: updateOption
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
 * 删除文件
 * @param {string} ws
 * @param {array} fileIdList
 * @returns
 */
export function deleteFiles(ws, fileIdList) {
  return request({
    url: BASE_API+'/files',
    method: 'delete',
    headers: {
      "Content-Type" : "application/json"
    },
    params: {
      workspace: ws,
    },
    data: fileIdList
  })
}
