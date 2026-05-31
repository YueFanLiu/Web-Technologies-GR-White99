import request from '@/utils/request'

export function saveEvent(eventId) {
  return request({
    url: `/api/events/${eventId}/save`,
    method: 'post'
  })
}

export function unsaveEvent(eventId) {
  return request({
    url: `/api/events/${eventId}/save`,
    method: 'delete'
  })
}

export function getEventSaveStatus(eventId) {
  return request({
    url: `/api/events/${eventId}/save`,
    method: 'get'
  })
}

export function getMySavedEvents() {
  return request({
    url: '/api/users/me/saved-events',
    method: 'get'
  })
}
