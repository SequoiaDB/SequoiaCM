import axios from 'axios'
import qs from 'qs'
import { message as Message } from '@/utils/scm-message'
import store from '@/store'
import { getToken } from '@/utils/auth'

// create an axios instance
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
  // withCredentials: true, // send cookies when cross-domain requests
  timeout: 30000, // request timeout
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  }
})

// request interceptor
service.interceptors.request.use(
  config => {
    // do something before request is sent
    if (store.getters.token) {
      // let each request carry token
      config.headers['x-auth-token'] = getToken()
    }
    if (config.headers['Content-Type'] == 'application/x-www-form-urlencoded') {
      config.data = qs.stringify(config.data)
    }
    return config
  },
  error => {
    // do something with request error
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(

  response => {
    return response
  },
  error => {
    let code = ''
    let path = ''
    if(error && error.response) {
      let data = error.response.data
      error.message = data.message
      path = data.path
      if( !data.message ){
        if( error.response.headers ){
          let scmError = error.response.headers['x-scm-error']
          if( scmError ){
            let errorDetail = JSON.parse(scmError)
            error.message = errorDetail.message
            path = errorDetail.path
          }
        }
      }
      error.message = error.message || 'unknown error'
      code = error.response.status
    }else {
      error.message = "Cannot connect to server"
    }
    console.log('error', error) // for debug
    // 请求被取消产生的异常，不需要弹出提示框
    if ( !error.__CANCEL__) {
      Message({
        message: error.message,
        type: 'error',
        showClose: true,
        duration: 0
      })
    }

    if(code == 401 && path != '/login') {
      store.dispatch('user/resetToken').then(() => {
        location.reload()
      })
    }
    return Promise.reject(error)
  }
)

export default service
