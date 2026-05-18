import request from '@/utils/request'

// 获取活动详情，用来展示写评价页面顶部的活动信息
export function getEventDetail(id) {
  return request({
    url: `/api/events/${id}`,
    method: 'get'
  })
}

// 获取活动图片，用来展示写评价页面顶部的活动图片
export function getEventImages(eventId) {
  return request({
    url: `/api/events/${eventId}/images`,
    method: 'get'
  })
}

// 获取当前登录用户，用来判断是谁要提交评价
export function getCurrentUserProfile() {
  return request({
    url: '/api/users/me',
    method: 'get'
  })
}

// 获取当前用户的报名记录，用来限制只有报名过活动的人才能评价
export function getRegistrationsByUser(userId) {
  return request({
    url: `/api/registrations/user/${userId}`,
    method: 'get'
  })
}

// 提交活动评价，后端 ReviewRequest 需要 rating 和 comment
export function createEventReview(eventId, data) {
  return request({
    url: `/api/events/${eventId}/reviews`,
    method: 'post',
    data
  })
}
