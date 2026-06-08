import request from '@/utils/request'

export function listAdminUsers(params) {
  return request({
    url: '/api/admin/users',
    method: 'get',
    params
  })
}

export function updateAdminUser(id, data) {
  return request({
    url: `/api/admin/users/${id}`,
    method: 'put',
    data
  })
}

export function updateAdminUserStatus(id, status) {
  return request({
    url: `/api/admin/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}

export function getPlatformUsageReport(params) {
  return request({
    url: '/api/admin/reports/platform-usage',
    method: 'get',
    params
  })
}
