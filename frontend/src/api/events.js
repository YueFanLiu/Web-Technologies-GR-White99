import request from '@/utils/request'

export function listEvents(params = {}) {
  return request({
    url: '/api/events',
    method: 'get',
    params,
    headers: {
      isToken: false
    }
  })
}
