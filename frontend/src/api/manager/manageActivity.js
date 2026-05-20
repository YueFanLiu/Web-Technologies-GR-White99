import request from '@/utils/request'

export function getEvent(id) {
  return request({
    url: `/api/events/${id}`,
    method: 'get'
  })
}

export function updateEvent(id, data) {
  return request({
    url: `/api/events/${id}`,
    method: 'put',
    data
  })
}

export function updateLocation(id, data) {
  return request({
    url: `/api/locations/${id}`,
    method: 'put',
    data
  })
}

export function getLocationAccessibility(locationId) {
  return request({
    url: `/api/locations/${locationId}/accessibility`,
    method: 'get'
  })
}

export function createLocationAccessibility(locationId, data) {
  return request({
    url: `/api/locations/${locationId}/accessibility`,
    method: 'post',
    data
  })
}

export function updateLocationAccessibility(locationId, data) {
  return request({
    url: `/api/locations/${locationId}/accessibility`,
    method: 'put',
    data
  })
}

export function deleteLocationAccessibility(locationId) {
  return request({
    url: `/api/locations/${locationId}/accessibility`,
    method: 'delete'
  })
}

export function getEventRegistrations(eventId) {
  return request({
    url: `/api/registrations/event/${eventId}`,
    method: 'get'
  })
}
