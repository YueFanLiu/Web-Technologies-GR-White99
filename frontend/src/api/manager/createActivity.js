import request from '@/utils/request'

export function createLocation(data) {
  return request({
    url: '/api/locations',
    method: 'post',
    data
  })
}

export function createLocationAccessibility(locationId, data) {
  return request({
    url: `/api/locations/${locationId}/accessibility`,
    method: 'post',
    data
  })
}

export function createEvent(data) {
  return request({
    url: '/api/events',
    method: 'post',
    data
  })
}

export function uploadEventImage(eventId, file) {
  const data = new FormData()
  data.append('file', file)

  return request({
    url: `/api/events/${eventId}/images`,
    headers: {
      repeatSubmit: false
    },
    method: 'post',
    data
  })
}
