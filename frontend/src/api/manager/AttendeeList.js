import request from '@/utils/request'

export function getEvent(id) {
  return request({
    url: `/api/events/${id}`,
    method: 'get'
  })
}

export function getEventRegistrations(eventId) {
  return request({
    url: `/api/registrations/event/${eventId}`,
    method: 'get'
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
