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
