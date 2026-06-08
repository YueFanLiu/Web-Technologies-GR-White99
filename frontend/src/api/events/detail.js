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

export function createEventReview(eventId, data) {
    return request({
        url: `/api/events/${eventId}/reviews`,
        method: 'post',
        data,
        headers: {
            permissionMessage: 'You can review only confirmed events you attended after they have ended.'
        }
    })
}

export function updateEventReview(eventId, reviewId, data) {
    return request({
        url: `/api/events/${eventId}/reviews/${reviewId}`,
        method: 'put',
        data,
        headers: {
            permissionMessage: 'You do not have permission to perform this action.'
        }
    })
}

export function deleteEventReview(eventId, reviewId) {
    return request({
        url: `/api/events/${eventId}/reviews/${reviewId}`,
        method: 'delete',
        headers: {
            permissionMessage: 'You do not have permission to perform this action.'
        }
    })
}

// 获取活动报名列表
export function getEventRegistrations(eventId) {
    return request({
        url: `/api/registrations/event/${eventId}`,
        method: 'get',
        headers: {
            permissionMessage: 'You do not have permission to perform this action.'
        }
    })
}

export function getEventTicketTiers(eventId) {
    return request({
        url: `/api/events/${eventId}/ticket-tiers`,
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

export function downloadRegistrationCalendar(registrationId) {
    return request({
        url: `/api/registrations/${registrationId}/calendar.ics`,
        method: 'get',
        responseType: 'blob'
    })
}

export function getRegistrationsByUser(userId) {
    return request({
        url: `/api/registrations/user/${userId}`,
        method: 'get'
    })
}

export function getCurrentUserProfile() {
    return request({
        url: '/api/users/me',
        method: 'get'
    })
}

// 获取地点无障碍信息
export function getLocationAccessibility(locationId) {
    return request({
        url: `/api/locations/${locationId}/accessibility`,
        method: 'get',
        headers: {
            showErrorMessage: false
        }
    })
}
