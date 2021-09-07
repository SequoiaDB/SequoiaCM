import { login, logout, getInfo } from '@/api/user'
import { getToken,  getUsername, setToken, removeToken, setUsername, removeUser, encrypt } from '@/utils/auth'
import { resetRouter } from '@/router'

const getDefaultState = () => {
  return {
    // 
    token: getToken(),
    // username for fetch userInfo
    user_name: getUsername(),
    user_id: '',
    user_type: '',
    enable: '',
    roles: []
  }
}

const state = getDefaultState()

const mutations = {
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_USER_NAME: (state, username) => {
    state.user_name = username
  },
  SET_USER_INFO: (state, userInfo) => {
    state.user_id = userInfo.user_id
    state.user_type = userInfo.user_type
    state.user_name  = state.user_name
    state.roles = userInfo.roles
    state.enable = userInfo.enable
  }
  
  
}

const actions = {
  // user login
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password: encrypt(password) }).then(response => {
        const { headers } = response
        commit('SET_TOKEN', headers["x-auth-token"])
        commit('SET_USER_NAME', username)
        setToken(headers["x-auth-token"])
        setUsername(username)
        resolve(username)
      }).catch(error => {
        reject(error)
      })
    }).then(username =>{
      return new Promise((resolve, reject) => {
        getInfo(username).then(res => {
          commit('SET_USER_INFO', res.data)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    })
  },

  // get user info
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getInfo(state.user_name).then(response => {
        const { data } = response
        if (!data) {
          return reject('Verification failed, please Login again.')
        }
        commit('SET_USER_INFO', data)
        resolve(data)
      }).catch(error => {
        reject(error)
      })
    })
  },

  // user logout
  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      logout(state.token).then(() => {
        removeToken() // must remove  token  first
        removeUser()
        resetRouter()
        commit('RESET_STATE')
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // remove token
  resetToken({ commit }) {
    return new Promise(resolve => {
      removeToken() // must remove  token  first
      removeUser()
      commit('RESET_STATE')
      resolve()
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

