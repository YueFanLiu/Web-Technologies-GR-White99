import request from '@/utils/request'

export async function createEventDirectChat(eventId, userId, permissionMessage = 'You can only message confirmed attendees of your own event.') {
  const existingChat = await getExistingDirectChat(userId)
  if (existingChat) return existingChat

  try {
    return await request({
      url: `/api/chats/events/${eventId}/direct`,
      method: 'post',
      data: { userId },
      headers: {
        permissionMessage
      }
    })
  } catch (error) {
    if (isDuplicateDirectChatError(error)) {
      const recoveredChat = await getExistingDirectChat(userId)
      if (recoveredChat) return recoveredChat
    }
    throw error
  }
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
  const candidateId = user?.id || user?.userId || user?.user_id || user?.user?.id || user?.profile?.id
  return Boolean(candidateId && userId && String(candidateId) === String(userId))
}

function normalizeType(type) {
  return String(type || '').trim().toUpperCase()
}

function isDirectChat(chat) {
  const type = normalizeType(chat?.type || chat?.conversationType || chat?.chatType)
  return !type || type === 'DIRECT'
}

function getChatParticipants(chat) {
  const participants = chat?.participants || chat?.users || chat?.members || []
  return Array.isArray(participants) ? participants : []
}

function getErrorText(error) {
  const data = error?.response?.data
  if (typeof data === 'string') return data
  if (data) return JSON.stringify(data)
  return error?.message || ''
}

function isDuplicateDirectChatError(error) {
  const text = getErrorText(error)
  return text.includes('duplicate key value violates unique constraint') ||
    text.includes('chat_conversations_direct_pair_uidx')
}

export function getChatId(chat) {
  return chat?.id || chat?.chatId || chat?.conversationId
}

export async function getExistingDirectChat(userId) {
  const conversations = extractList(await getChats())
  console.log('[chat] all chats:', conversations)
  console.log('[chat] target userId:', userId)
  const matchedChat = conversations.find((chat) => {
    return isDirectChat(chat) && getChatParticipants(chat).some((participant) => isSameUser(participant, userId))
  }) || null
  console.log('[chat] matched existing direct chat:', matchedChat)
  return matchedChat
}

export async function getOrCreateDirectChat(userId) {
  const existingChat = await getExistingDirectChat(userId)
  if (existingChat) return existingChat

  try {
    return await createDirectChat(userId)
  } catch (error) {
    if (isDuplicateDirectChatError(error)) {
      console.warn('[chat] direct chat already exists; recovering from duplicate create response.', error)
    }
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
