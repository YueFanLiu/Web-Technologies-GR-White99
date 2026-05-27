export const ROLES = {
  PARENT: 'PARENT',
  ORGANIZER: 'ORGANIZER',
  ADMIN: 'ADMIN'
}

export function normalizeRole(role) {
  return String(role || '').replace(/^role_/i, '').toUpperCase()
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

export function canManagePostForUser(userInfo, post) {
  if (!userInfo) {
    return false
  }

  if (isAdmin(userInfo)) {
    return true
  }

  const currentUserId = getUserId(userInfo)
  const ownerId = getPostOwnerId(post)
  return Boolean(currentUserId && ownerId && String(currentUserId) === String(ownerId))
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
  return Boolean(currentUserId && organizerId && String(currentUserId) === String(organizerId))
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
