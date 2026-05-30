export const ROLES = {
  PARENT: 'PARENT',
  ORGANIZER: 'ORGANIZER',
  ADMIN: 'ADMIN'
}

export function normalizeRole(role) {
  return String(role || '').replace(/^role_/i, '').toUpperCase()
}

export function roleDisplayLabel(role) {
  const normalized = normalizeRole(role)
  return normalized === ROLES.PARENT ? 'ATTENDEE' : normalized
}

export function getUserId(userInfo) {
  return userInfo?.id || userInfo?.userId || userInfo?.user?.id || userInfo?.profile?.id || ''
}

export function isParent(userInfo) {
  return normalizeRole(userInfo?.role) === ROLES.PARENT
}

export function isOrganizer(userInfo) {
  return normalizeRole(userInfo?.role) === ROLES.ORGANIZER
}

export function isAdmin(userInfo) {
  return normalizeRole(userInfo?.role) === ROLES.ADMIN
}

export function isAdminRole(role) {
  return normalizeRole(role) === ROLES.ADMIN
}

export function isOrganizerRole(role) {
  return normalizeRole(role) === ROLES.ORGANIZER
}

export function canCreateActivityForUser(userInfo) {
  return isOrganizer(userInfo) || isAdmin(userInfo)
}

export function getPostOwnerId(post) {
  return post?.authorId || post?.userId || post?.user?.id || post?.author?.id || post?.createdBy || ''
}

export function getActivityOrganizerId(activity) {
  return activity?.organizerId || activity?.organizer?.id || activity?.organizer?.userId || activity?.createdBy || activity?.userId || ''
}

export function getReviewAuthorId(review) {
  return review?.authorId || review?.userId || review?.user?.id || review?.author?.id || review?.createdBy || ''
}

export function getRegistrationUserId(registration) {
  return registration?.userId || registration?.user?.id || registration?.attendeeId || registration?.parentId || ''
}

export function getRelatedEvent(post, fallbackEvent = null) {
  return fallbackEvent || post?.event || post?.activity || post?.relatedEvent || null
}

function sameId(left, right) {
  return Boolean(left && right && String(left) === String(right))
}

export function isEventEnded(event) {
  const rawEndTime = event?.endTime || event?.endDate || event?.endsAt
  const endTime = rawEndTime ? new Date(rawEndTime) : null
  if (endTime && !Number.isNaN(endTime.getTime())) {
    return endTime.getTime() < Date.now()
  }

  const status = String(event?.status || '').trim().toUpperCase()
  return ['ENDED', 'COMPLETED', 'PAST'].includes(status)
}

export function isConfirmedRegistration(registration) {
  return String(registration?.status || '').trim().toUpperCase() === 'CONFIRMED'
}

export function canManagePostForUser(userInfo, post) {
  if (!userInfo) {
    return false
  }

  if (isAdmin(userInfo)) {
    return true
  }

  const currentUserId = getUserId(userInfo)
  const ownerId = getPostOwnerId(post)
  return sameId(currentUserId, ownerId)
}

export function canManageActivityForUser(userInfo, activity) {
  if (!userInfo) {
    return false
  }

  if (isAdmin(userInfo)) {
    return true
  }

  if (!isOrganizer(userInfo)) {
    return false
  }

  const currentUserId = getUserId(userInfo)
  const organizerId = getActivityOrganizerId(activity)
  return sameId(currentUserId, organizerId)
}

export function canManageActivity(activity, userInfo) {
  return canManageActivityForUser(userInfo, activity)
}

export function canViewAttendees(activity, userInfo) {
  return canManageActivityForUser(userInfo, activity)
}

export function canManagePost(post, userInfo, event = null) {
  return canManagePostForUser(userInfo, event ? { ...post, event } : post)
}

export function canManageEventReview(review, event, userInfo) {
  if (!userInfo) {
    return false
  }

  if (isAdmin(userInfo)) {
    return true
  }

  const currentUserId = getUserId(userInfo)
  return sameId(currentUserId, getReviewAuthorId(review)) ||
    canManageActivityForUser(userInfo, event)
}

export function canManagePostComment(comment, post, event, userInfo) {
  if (!userInfo) {
    return false
  }

  if (isAdmin(userInfo)) {
    return true
  }

  const currentUserId = getUserId(userInfo)
  return sameId(currentUserId, getReviewAuthorId(comment)) ||
    sameId(currentUserId, getPostOwnerId(post)) ||
    canManageActivityForUser(userInfo, event || getRelatedEvent(post))
}

export function canWriteReview(event, registration, userInfo = null) {
  if (!isEventEnded(event) || !isConfirmedRegistration(registration)) {
    return false
  }

  if (!userInfo) {
    return true
  }

  return sameId(getUserId(userInfo), getRegistrationUserId(registration))
}

export function canManageRegistration(registration, event, userInfo) {
  if (!userInfo) {
    return false
  }

  return isAdmin(userInfo) || canManageActivityForUser(userInfo, event || registration?.event)
}

export function canCancelRegistration(registration, event, userInfo) {
  if (!userInfo) {
    return false
  }

  return canManageRegistration(registration, event, userInfo) ||
    sameId(getUserId(userInfo), getRegistrationUserId(registration))
}

export function roleAllowed(userInfo, allowedRoles = []) {
  if (!allowedRoles.length) {
    return true
  }

  const currentRole = normalizeRole(userInfo?.role)
  if (currentRole === ROLES.ADMIN) {
    return true
  }

  return allowedRoles.some((role) => normalizeRole(role) === currentRole)
}
