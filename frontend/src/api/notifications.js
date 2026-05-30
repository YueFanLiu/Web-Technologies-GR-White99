import request from '@/utils/request'

export function listNotifications(query) {
  return request({
    url: '/api/notifications',
    method: 'get',
    params: query || undefined,
    headers: {
      showErrorMessage: false
    }
  })
}

export function getUnreadNotificationCount() {
  return request({
    url: '/api/notifications/unread-count',
    method: 'get',
    headers: {
      showErrorMessage: false
    }
  })
}

export function markNotificationRead(id) {
  return request({
    url: `/api/notifications/${id}/read`,
    method: 'post',
    headers: {
      showErrorMessage: false
    }
  })
}
