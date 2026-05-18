import axios from 'axios'
import { ElNotification , ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/ruoyi'
import cache from '@/plugins/cache'
import { saveAs } from 'file-saver'
import useUserStore from '@/store/modules/user'

let downloadLoadingInstance
// 是否已经弹出“重新登录”提示，防止多个 401 请求同时弹出多个弹窗
export let isRelogin = { show: false }

// 默认请求体类型：普通接口默认使用 JSON
axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const service = axios.create({
  // axios中请求配置有baseURL选项，表示请求URL公共部分
  baseURL: import.meta.env.VITE_APP_BASE_API,
  // 超时
  timeout: 10000
})

// 统一处理登录失效：
// 1. 弹出重新登录确认框
// 2. 用户确认后清空本地 token 和用户状态
// 3. 跳转回登录页，重新获取有效 token
function handleUnauthorized() {
  if (!isRelogin.show) {
    isRelogin.show = true
    ElMessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      isRelogin.show = false
      useUserStore().logOut().then(() => {
        location.href = '/login'
      })
    }).catch(() => {
      isRelogin.show = false
    })
  }
}

// request拦截器
// 作用：在请求真正发出去之前，统一处理 baseURL、token、GET 参数、重复提交和文件上传请求头
service.interceptors.request.use(config => {
  // 是否需要设置 token
  // 默认所有请求都会带 token；如果某个接口不需要 token，就在 api 文件里写 headers: { isToken: false }
  const isToken = (config.headers || {}).isToken === false
  // 是否需要防止数据重复提交
  // 默认 POST/PUT 会做重复提交检查；如果某个接口允许连续提交，就写 headers: { repeatSubmit: false }
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
  // 间隔时间(ms)，小于此时间视为重复提交
  const interval = (config.headers || {}).interval || 1000
  if (getToken() && !isToken) {
    // 后端使用 Bearer Token 校验登录状态，所以这里统一把登录后的 token 放到 Authorization 请求头
    config.headers['Authorization'] = 'Bearer ' + getToken()
  }
  // FormData 上传不能沿用全局 JSON Content-Type，需要让浏览器自动补 multipart boundary
  // 例如 post/event/location 图片上传，都应该走这里，避免后端收不到 multipart 文件
  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    if (typeof config.headers?.delete === 'function') {
      config.headers.delete('Content-Type')
    } else {
      delete config.headers['Content-Type']
      delete config.headers['content-type']
    }
  }
  // get请求映射params参数
  // RuoYi 的 tansParams 会把 params 对象拼到 URL 后面，避免 axios 默认序列化和后端格式不一致
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    // 记录本次 POST/PUT 请求，用于判断短时间内是否重复提交同一个接口和同一份数据
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length // 请求数据大小
    const limitSize = 5 * 1024 * 1024 // 限制存放数据5M
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
      return config
    }
    // sessionObj 保存上一条提交记录；如果 URL、数据相同并且间隔太短，就拦截掉
    const sessionObj = cache.session.getJSON('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url                // 请求地址
      const s_data = sessionObj.data              // 请求数据
      const s_time = sessionObj.time              // 请求时间
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
        const message = '数据正在处理，请勿重复提交'
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  return config
}, error => {
    console.log(error)
    Promise.reject(error)
})

// 响应拦截器
// 作用：统一处理后端返回的数据结构、业务错误码、HTTP 错误和提示信息
service.interceptors.response.use(res => {
    // 未设置状态码则默认成功状态
    // 有些后端直接返回业务数据，没有 code 字段；这种情况按 200 成功处理
    const code = res.data.code || 200
    // 获取错误信息
    const msg = errorCode[code] || res.data.msg || errorCode['default']
    // 二进制数据则直接返回
    if (res.request.responseType ===  'blob' || res.request.responseType ===  'arraybuffer') {
      return res.data
    }
    if (code === 401) {
      // 处理“HTTP 200，但业务 code 是 401”的登录失效情况
      handleUnauthorized()
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')
    } else if (code === 500) {
      ElMessage({ message: msg, type: 'error' })
      return Promise.reject(new Error(msg))
    } else if (code === 601) {
      ElMessage({ message: msg, type: 'warning' })
      return Promise.reject(new Error(msg))
    } else if (code !== 200) {
      ElNotification.error({ title: msg })
      return Promise.reject('error')
    } else {
      return  Promise.resolve(res.data)
    }
  },
  error => {
    console.log('err' + error)
    if (error?.response?.status === 401) {
      // 处理后端直接返回 HTTP 401 的情况，避免只显示“系统接口401异常”
      handleUnauthorized()
      return Promise.reject(error)
    }
    let message = error?.response?.data?.message || error?.response?.data?.msg || error?.message
    if (message == "Network Error") {
      message = "后端接口连接异常"
    } else if (message && message.includes("timeout")) {
      message = "系统接口请求超时"
    } else if (message && message.includes("Request failed with status code")) {
      message = "系统接口" + message.slice(-3) + "异常"
    }
    ElMessage({ message: message, type: 'error', duration: 5 * 1000 })
    return Promise.reject(error)
  }
)

// 通用下载方法
// 作用：发送文件下载请求，校验返回内容是否为 blob，成功则保存文件，失败则展示后端错误信息
export function download(url, params, filename, config) {
  downloadLoadingInstance = ElLoading.service({ text: "正在下载数据，请稍候", background: "rgba(0, 0, 0, 0.7)", })
  return service.post(url, params, {
    transformRequest: [(params) => { return tansParams(params) }],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    ...config
  }).then(async (data) => {
    const isBlob = blobValidate(data)
    if (isBlob) {
      const blob = new Blob([data])
      saveAs(blob, filename)
    } else {
      const resText = await data.text()
      const rspObj = JSON.parse(resText)
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      ElMessage.error(errMsg)
    }
    downloadLoadingInstance.close()
  }).catch((r) => {
    console.error(r)
    ElMessage.error('下载文件出现错误，请联系管理员！')
    downloadLoadingInstance.close()
  })
}

export default service
