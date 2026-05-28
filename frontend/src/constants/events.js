export const EVENT_CATEGORY_OPTIONS = [
  { label: 'Art', value: 'ART' },
  { label: 'Outdoor', value: 'OUTDOOR' },
  { label: 'Tech', value: 'TECH' },
  { label: 'Sport', value: 'SPORT' },
  { label: 'Wellness', value: 'WELLNESS' },
  { label: 'Social', value: 'SOCIAL' }
]

export const ACCESSIBILITY_FILTER_OPTIONS = [
  { label: 'Wheelchair Accessible', value: 'wheelchairAccessible' },
  { label: 'Elevator', value: 'hasElevator' },
  { label: 'Accessible Restroom', value: 'accessibleToilet' },
  { label: 'Low Noise Level', value: 'quietEnvironment' },
  { label: 'Step-free Access', value: 'stepFreeAccess' }
]

export function normalizeQueryList(value) {
  if (Array.isArray(value)) {
    return value.filter(Boolean).join(',')
  }
  return value || ''
}

export function isAlreadyRegisteredError(error) {
  const status = Number(error?.response?.status || error?.status || 0)
  const message = String(
    error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.message ||
    ''
  ).toLowerCase()

  return [400, 403, 409, 500].includes(status) &&
    ['already', 'duplicate', 'registration', 'registered', 'exists', 'unique'].some((word) => message.includes(word))
}
