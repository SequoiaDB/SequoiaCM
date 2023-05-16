const trigger = (input, type, binding) => {
  let value = input.value
  // 保留数字和负号，其它字符都删除
  value = value.replace(/[^0-9-]/g, '')
  // 如果第一个字符是负号，则只保留一个负号
  let isNeg = false
  if (value && value.length > 0) {
    isNeg = value.substring(0, 1) === '-'
  }
  value = value.replace(/-/g, '')
  if (isNeg) {
    value = '-' + value
  }
  if (binding.value) {
    if (binding.value.minValue != undefined && Number(value) < binding.value.minValue) {
      value = binding.value.minValue
    }
    if (binding.value.maxValue && Number(value) > binding.value.maxValue) {
      value = binding.value.maxValue
    }
  }
  input.value = value
  const e = document.createEvent('HTMLEvents')
  e.initEvent(type, true, true)
  input.dispatchEvent(e)
}

const numberOnly = {
  inserted(el, binding, vNode) {
    const input = el.getElementsByTagName('input')[0]
    input.addEventListener('compositionstart', () => {
      vNode.locking = true
    })
      
    input.addEventListener('compositionend', () => {
      vNode.locking = false//解决中文输入双向绑定失效
      input.dispatchEvent(new Event('input'))
    })
      
    input.onkeyup = function (e) {
      if (vNode.locking) {
        return
      }
      trigger(input, 'input', binding)
    }
    input.onblur = function (e) {
      if (vNode.locking) {
        return
      }
      trigger(input, 'input', binding)
    }
  }
}
export default numberOnly