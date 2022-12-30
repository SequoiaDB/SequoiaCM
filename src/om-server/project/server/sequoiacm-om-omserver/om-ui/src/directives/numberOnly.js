const numberOnly = {
  inserted(el, binding) {
    el.addEventListener('input', function (event) {
      let value = event.target.value;
      // 保留数字和负号，其它字符都删除
      value = value.replace(/[^0-9-]/g, '');
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
      event.target.value = value;
    })
  }
}
export default numberOnly

