import request from '@/utils/request'

function toNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function formatAddress(location = {}) {
  return [location.address, location.city, location.country].filter(Boolean).join(', ')
}

export function normalizeEventMapMarker(event) {
  const location = event?.location || {}
  const latitude = toNumber(location.latitude)
  const longitude = toNumber(location.longitude)

  if (latitude === null || longitude === null) {
    return null
  }

  return {
    eventId: event.id,
    activityId: event.id,
    title: event.title || 'Untitled event',
    address: formatAddress(location) || location.name || 'Address TBA',
    venue: location.name || '',
    latitude,
    longitude,
    activityType: event.category || '',
    status: event.status || '',
    locationId: location.id || null
  }
}

export function normalizeEventMapMarkers(events = []) {
  return events
    .map(normalizeEventMapMarker)
    .filter(Boolean)
}

function getEventsForMap(url, query) {
  return request({
    url,
    headers: {
      isToken: false,
      showErrorMessage: false
    },
    method: 'get',
    params: query
  }).then((events) => {
    const list = Array.isArray(events) ? events : []
    return {
      events: list,
      markers: normalizeEventMapMarkers(list)
    }
  })
}

export function listEventMapPoints(query) {
  return getEventsForMap('/api/events', query)
}

export function searchEventMapPoints(query) {
  return getEventsForMap('/api/events/search', query)
}
