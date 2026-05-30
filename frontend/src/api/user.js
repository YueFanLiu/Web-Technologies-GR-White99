import request from '@/utils/request'

export function getPublicUserProfile(userId) {
  return request({
    url: `/api/users/${userId}`,
    method: 'get'
  })
}

export function searchUsers(keyword, limit = 20) {
  return request({
    url: '/api/users/search',
    method: 'get',
    params: {
      keyword,
      limit
    }
  })
}
