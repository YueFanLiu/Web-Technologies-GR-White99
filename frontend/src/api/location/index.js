import request from '@/utils/request'


export function listLocations() {
  return request({
    url: '/api/locations',
    headers: {
      isToken: false
    },
    method: 'get',
  })
}