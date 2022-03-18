
/**
 * Parse the time to string
 * @param {(Object|string|number)} time
 * @param {string} cFormat
 * @returns {string | null}
 */
export function parseTime(time, cFormat) {
  if (arguments.length === 0 || !time) {
    return null
  }
  const format = cFormat || '{y}-{m}-{d} {h}:{i}:{s}'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if ((typeof time === 'string')) {
      if ((/^[0-9]+$/.test(time))) {
        // support "1548221490638"
        time = parseInt(time)
      } else {
        // support safari
        time = time.replace(new RegExp(/-/gm), '/')
      }
    }

    if ((typeof time === 'number') && (time.toString().length === 10)) {
      time = time * 1000
    }
    date = new Date(time)
  }
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  }
  const time_str = format.replace(/{([ymdhisa])+}/g, (result, key) => {
    const value = formatObj[key]
    // Note: getDay() returns 0 on Sunday
    if (key === 'a') { return ['日', '一', '二', '三', '四', '五', '六'][value ] }
    return value.toString().padStart(2, '0')
  })
  return time_str
}

/**
 * @param {number} time
 * @param {string} option
 * @returns {string}
 */
export function formatTime(time, option) {
  if (('' + time).length === 10) {
    time = parseInt(time) * 1000
  } else {
    time = +time
  }
  const d = new Date(time)
  const now = Date.now()

  const diff = (now - d) / 1000

  if (diff < 30) {
    return '刚刚'
  } else if (diff < 3600) {
    // less 1 hour
    return Math.ceil(diff / 60) + '分钟前'
  } else if (diff < 3600 * 24) {
    return Math.ceil(diff / 3600) + '小时前'
  } else if (diff < 3600 * 24 * 2) {
    return '1天前'
  }
  if (option) {
    return parseTime(time, option)
  } else {
    return (
      d.getMonth() +
      1 +
      '月' +
      d.getDate() +
      '日' +
      d.getHours() +
      '时' +
      d.getMinutes() +
      '分'
    )
  }
}

/**
 * @param {string} url
 * @returns {Object}
 */
export function param2Obj(url) {
  const search = decodeURIComponent(url.split('?')[1]).replace(/\+/g, ' ')
  if (!search) {
    return {}
  }
  const obj = {}
  const searchArr = search.split('&')
  searchArr.forEach(v => { 
    const index = v.indexOf('=')
    if (index !== -1) { 
      const name = v.substring(0, index)
      const val = v.substring(index + 1, v.length)
      obj[name] = val
    }
  })
  return obj
}

/**
 * @param {number} len
 * @returns {string}
 */
export function randomStr(len) {
  let chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
  let res = ''
  let maxPos = chars.length
  for(let i=0; i<len; i++){
    res = res + chars.charAt(Math.floor(Math.random() * maxPos))
  }
  return res
}

/**
 * @param {Map} map 
 * @returns {Object} 
 */
export function mapToObject(map) {
  let obj = Object.create(null)
  for (let[k,v] of map) {
    obj[k] = v
  }
  return obj
}

/**
 * @param {string|object} data
 * @returns {string}
 */
 export function toPrettyJson(data) {
  if (data instanceof Object) {
    return JSON.stringify(data, null, 2)
  }
  let obj
  try {
    obj = JSON.parse(data)
  }catch(err){
    return data
  }
  return JSON.stringify(obj, null, 2)
}

/**
 * @param {string} str
 * @returns {boolean}
 */
export function isJsonStr (str) {
  if (typeof str == 'string') {
    try {
      let obj = JSON.parse(str)
      if (typeof obj == 'object' && obj) {
        return true
      } else {
        return false
      }
    } catch (e) {
      return false
    }
  }
}

/**
 * @param {object} obj
 */
 export function isObject(obj){
  return Object.prototype.toString.call(obj)==='[object Object]';
};
/**
* @param {Array} arr
*/
export function isArray(arr){
  return Object.prototype.toString.call(arr)==='[object Array]';
};
/**
* @param {object} oldData
* @param {object} newData
*/
 export function equalsObj(oldData, newData){
   if(oldData === newData){
     return true
   }
   if(isObject(oldData) && isObject(newData) && Object.keys(oldData).length === Object.keys(newData).length){
      for (const key in oldData) {
        if (oldData.hasOwnProperty(key)) {
          if(!equalsObj(oldData[key],newData[key])) {
            return false
          }
        }
      }
   }else if(isArray(oldData) && isArray(oldData) && oldData.length===newData.length){
      for (let i = 0,length=oldData.length; i <length; i++) {
        if(!equalsObj(oldData[i],newData[i]))
        return false
      }
   }else{
      return false
   }
   return true
}

/**
 * Convert file size
 * @param {long} fileSize 
 * @returns 
 */
export function convertFileSize(val) {
  if (val == 0) return "0 B";
  var k = 1024;
  var sizes = ["B", "KB", "MB", "GB", "TB", "PB"],
  i = Math.floor(Math.log(val) / Math.log(k));
  return (val / Math.pow(k, i)).toPrecision(3) + " " + sizes[i];
}
/**
 *
 * @param {string} str
 */
export function escapeStr(str) {
  const specialChars = '$^*()+|\\{}[].?'
  str = str + ''
  let newStr = ''
  for (let i=0; i < str.length; i++) {
    let c = str.charAt(i)
    if (specialChars.indexOf(c) != -1) {
      newStr += '\\' + c
    } else {
      newStr += c
    }
  }
  return newStr
}
