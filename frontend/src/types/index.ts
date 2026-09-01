// User types
export interface User {
  id: string;
  email: string;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
}

// Auth types
export interface LoginRequest {
  emailOrUsername: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  displayName?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: User;
}

// Conversation types
export type ConversationType = 'DIRECT' | 'GROUP';
export type MemberRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export interface Conversation {
  id: string;
  type: ConversationType;
  name: string | null;
  avatarUrl: string | null;
  members: ConversationMember[];
  lastMessage: Message | null;
  unreadCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationMember {
  id: string;
  userId: string;
  user: User;
  role: MemberRole;
  lastReadMessageId: string | null;
  joinedAt: string;
}

// Message types
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE';

export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  sender: User;
  content: string | null;
  type: MessageType;
  replyTo: Message | null;
  edited: boolean;
  deleted: boolean;
  createdAt: string;
  updatedAt: string;
}

// WebSocket event types
export type WebSocketEventType =
  | 'MESSAGE_SENT'
  | 'MESSAGE_UPDATED'
  | 'MESSAGE_DELETED'
  | 'MESSAGE_READ'
  | 'TYPING'
  | 'USER_ONLINE'
  | 'USER_OFFLINE';

export interface WebSocketEvent<T = unknown> {
  type: WebSocketEventType;
  payload: T;
  timestamp: string;
}

// API response types
export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface CursorPageResponse<T> {
  content: T[];
  hasMore: boolean;
  nextCursor: string | null;
}
