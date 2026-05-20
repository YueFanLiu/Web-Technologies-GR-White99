import request from '@/utils/request'

export function getCurrentUserProfile() {
  return request({
    url: '/api/users/me',
    method: 'get'
  })
}

export function updateCurrentUserProfile(data) {
  return request({
    url: '/api/users/me',
    method: 'put',
    data
  })
}

export function uploadCurrentUserAvatar(file) {
  const data = new FormData()
  data.append('file', file)

  return request({
    url: '/api/users/me/avatar',
    method: 'post',
    data
  })
}
