import request from '@/utils/request'

export function getEvent(id) {
  return request({
    url: `/api/events/${id}`,
    method: 'get'
  })
}

export function getCurrentUserProfile() {
  return request({
    url: '/api/users/me',
    method: 'get'
  })
}

export function getPublicUser(id) {
  return request({
    url: `/api/users/${id}`,
    method: 'get'
  })
}

export function searchUsers(keyword) {
  return request({
    url: '/api/users/search',
    method: 'get',
    params: { keyword }
  })
}

export function listRegistrations() {
  return request({
    url: '/api/registrations',
    method: 'get'
  })
}

export function getRegistration(id) {
  return request({
    url: `/api/registrations/${id}`,
    method: 'get'
  })
}

export function getEventRegistrations(eventId) {
  return request({
    url: `/api/registrations/event/${eventId}`,
    method: 'get'
  })
}

export function createRegistration(data) {
  return request({
    url: '/api/registrations',
    method: 'post',
    data
  })
}

export function updateRegistration(id, data) {
  return request({
    url: `/api/registrations/${id}`,
    method: 'put',
    data
  })
}

export function deleteRegistration(id) {
  return request({
    url: `/api/registrations/${id}`,
    method: 'delete'
  })
}
