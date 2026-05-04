import request from '@/utils/request'

// 1. 获取活动列表
export function listEvent(query) {
  return request({
    url: '/api/events',
    headers: {
      isToken: false
    },
    method: 'get',
    params: query
  })
}

// 2. 获取活动详情
export function getEvent(id) {
  return request({
    url: `/api/events/${id}`,
    headers: {
      isToken: false
    },
    method: 'get'
  })
}

// 3. 创建活动
export function createEvent(data) {
  return request({
    url: '/api/events',
    method: 'post',
    data: data,
  })
}

// 4. 修改活动
export function updateEvent(id, data) {
  return request({
    url: `/api/events/${id}`,
    method: 'put',
    data: data
  })
}

// 5. 删除活动
export function delEvent(id) {
  return request({
    url: `/api/events/${id}`,
    method: 'delete'
  })
}