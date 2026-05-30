import request from '@/utils/request'

export function createEventDirectChat(eventId, userId, permissionMessage = 'You can only message confirmed attendees of your own event.') {
  return request({
    url: `/api/chats/events/${eventId}/direct`,
    method: 'post',
    data: { userId },
    headers: {
      permissionMessage
    }
  })
}

export function createDirectChat(userId) {
  return request({
    url: '/api/chats/direct',
    method: 'post',
    data: { userId },
    headers: {
      permissionMessage: 'You need to add this user as a friend before messaging.'
    }
  })
}

function extractList(res) {
  const data = res?.data ?? res
  if (Array.isArray(data)) return data
  return data?.rows || data?.list || data?.content || []
}

function isSameUser(user, userId) {
  return Boolean(user?.id && userId && String(user.id) === String(userId))
}

export async function getExistingDirectChat(userId) {
  const conversations = extractList(await getChats())
  return conversations.find((chat) => {
    return !chat.event && Array.isArray(chat.participants) &&
      chat.participants.some((participant) => isSameUser(participant, userId))
  }) || null
}

export async function getOrCreateDirectChat(userId) {
  const existingChat = await getExistingDirectChat(userId)
  if (existingChat) return existingChat

  try {
    return await createDirectChat(userId)
  } catch (error) {
    const recoveredChat = await getExistingDirectChat(userId)
    if (recoveredChat) return recoveredChat
    throw error
  }
}

export function getChats() {
  return request({
    url: '/api/chats',
    method: 'get'
  })
}

export function getChatMessages(chatId, query) {
  return request({
    url: `/api/chats/${chatId}/messages`,
    method: 'get',
    params: query || undefined
  })
}

export function sendChatMessage(chatId, content) {
  return request({
    url: `/api/chats/${chatId}/messages`,
    method: 'post',
    data: {
      content,
      messageType: 'TEXT'
    },
    headers: {
      permissionMessage: 'You do not have permission to send messages in this chat.'
    }
  })
}

export function markChatRead(chatId) {
  return request({
    url: `/api/chats/${chatId}/read`,
    method: 'post',
    data: {},
    headers: {
      showErrorMessage: false
    }
  })
}
