import request from '@/utils/request'
import { supabase } from '@/utils/supabase'

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

export function verifyEmail(data) {
  return request({
    url: '/api/auth/verify-email',
    headers: {
      isToken: false
    },
    method: 'post',
    data
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
      email: data.email,
      redirectTo: data.redirectTo
    }
  })
}

// 确认重置密码：Swagger 没有对应后端接口，使用 Supabase reset session 更新当前用户密码
export async function confirmPassword(data) {
  const { error } = await supabase.auth.updateUser({
    password: data.password
  })

  if (error) {
    throw error
  }

  return { success: true }
}
