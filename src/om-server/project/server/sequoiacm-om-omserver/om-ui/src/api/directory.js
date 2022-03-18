import request from '@/utils/request'

const BASE_API = '/api/v1'

/**
 * 获取目录的子目录列表
 * @param {string} ws 
 * @param {string} dirId 
 * @returns 
 */
 export function getSubDirList(ws, dirId, page, size) {
  return request({
    url: BASE_API+'/directory/id/'+dirId+'?action=list_sub_dir',
    method: 'get',
    params: {
      workspace: ws,
      dir_id: dirId,
      skip: (page-1)*size,
      limit: size
    }
  })
}