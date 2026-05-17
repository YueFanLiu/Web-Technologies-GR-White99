import request from '@/utils/request'

// 获取活动详情
export function getEventDetail(id) {
    return request({
        url: `/api/events/${id}`,
        method: 'get'
    })
}

// 获取活动图片
export function getEventImages(eventId) {
    return request({
        url: `/api/events/${eventId}/images`,
        method: 'get'
    })
}

// 获取活动评论
export function getEventReviews(eventId) {
    return request({
        url: `/api/events/${eventId}/reviews`,
        method: 'get'
    })
}

// 获取活动报名列表
export function getEventRegistrations(eventId) {
    return request({
        url: `/api/registrations/event/${eventId}`,
        method: 'get'
    })
}

export function createEventRegistration(data) {
    return request({
        url: '/api/registrations',
        method: 'post',
        data
    })
}

// 获取地点无障碍信息
export function getLocationAccessibility(locationId) {
    return request({
        url: `/api/locations/${locationId}/accessibility`,
        method: 'get'
    })
}
