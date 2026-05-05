import request from '@/utils/request'

// 登录方法
export function login(data) {
  return request({
    url: '/api/auth/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: {
      email:data.email,
      password: data.password
    }
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/api/auth/signup',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}

// 忘记密码：发送 Supabase password reset email
export function forgotPassword(data) {
  return request({
    url: '/api/auth/forgot-password',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: {
      email: data.email
    }
  })
}