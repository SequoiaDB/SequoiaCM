import Cookies from 'js-cookie'
import CryptoJS from 'crypto-js'
import store from '../store'
import router from '../router'
import { randomStr } from './index'
const TokenKey = 'scm_token'
const UsernameKey = 'username'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function getUsername() {
  return Cookies.get(UsernameKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function setUsername(username) {
  return Cookies.set(UsernameKey, username)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}

export function removeUser() {
  return Cookies.remove(UsernameKey)
}

export function encrypt(password) {
  let baseKeyLen = 2
  let baseKey = randomStr(baseKeyLen)
  let key = ''
  // fill key to 8 byte
  for(let i=0; i<(8/baseKeyLen); i++){
    key = key + baseKey
  }
  let iv = key
  let head = ''
  for(let i=0; i<baseKeyLen; i++){
    head = head + baseKey.charCodeAt(i).toString(16)
  }
  return head + encryptByDES(password,key,iv)
}

function encryptByDES(message, key, iv) {
  const keyHex = CryptoJS.enc.Utf8.parse(key)
  const ivHex = CryptoJS.enc.Utf8.parse(iv)
  const encrypted = CryptoJS.DES.encrypt(message, keyHex, {
    iv: ivHex,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return encrypted.ciphertext.toString() // 加密出来为 hex格式密文
}

/**
 * logout
 */
export async function logout(redirectPath) {
  await store.dispatch('user/logout')
  let path = '/login'
  if (redirectPath != null) {
    path = path + '?redirect=' + redirectPath
  }
  router.push(path)
}
