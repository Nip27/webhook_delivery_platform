export interface User {
  id: string;
  email: string;
  fullName: string;
}

export interface ApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  key?: string | null;
  isActive: boolean;
  createdAt: string;
  lastUsedAt?: string | null;
}

export interface WebhookEndpoint {
  id: string;
  url: string;
  description: string;
  secretPrefix: string;
  secret?: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface Subscription {
  id: string;
  eventTypeName: string;
  endpointId: string;
  endpointUrl: string;
  isActive: boolean;
  createdAt: string;
}

export interface EventLog {
  id: string;
  eventType: string;
  status: string;
  idempotencyKey?: string | null;
  createdAt: string;
}

export interface DeliveryAttempt {
  id: string;
  eventId: string;
  eventType: string;
  endpointUrl: string;
  status: string;
  responseStatusCode?: number | null;
  responseBody?: string | null;
  attemptNumber: number;
  durationMs?: number | null;
  errorMessage?: string | null;
  createdAt: string;
}

export interface DashboardStats {
  totalDeliveries: number;
  successfulDeliveries: number;
  failedDeliveries: number;
  deadLetteredDeliveries: number;
  totalEvents: number;
  successRate: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
