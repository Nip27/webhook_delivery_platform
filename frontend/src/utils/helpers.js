import { format, formatDistanceToNow } from 'date-fns';

export const formatDate = (dateString) => {
  if (!dateString) return '—';
  return format(new Date(dateString), 'MMM d, yyyy HH:mm:ss');
};

export const formatRelative = (dateString) => {
  if (!dateString) return '—';
  return formatDistanceToNow(new Date(dateString), { addSuffix: true });
};

export const truncate = (str, length = 50) => {
  if (!str) return '';
  return str.length > length ? str.substring(0, length) + '...' : str;
};

export const getStatusColor = (status) => {
  const colors = {
    SUCCESS: 'text-green-600 bg-green-50',
    FAILED: 'text-red-600 bg-red-50',
    DEAD_LETTERED: 'text-gray-600 bg-gray-100',
    PENDING: 'text-yellow-600 bg-yellow-50',
    PROCESSING: 'text-blue-600 bg-blue-50',
    DELIVERED: 'text-green-600 bg-green-50',
  };
  return colors[status] || 'text-gray-600 bg-gray-100';
};

export const statusBadgeColor = (status) => {
  const colors = {
    SUCCESS: 'green',
    FAILED: 'red',
    DEAD_LETTERED: 'gray',
    PENDING: 'yellow',
    PROCESSING: 'blue',
    DELIVERED: 'green',
  };
  return colors[status] || 'gray';
};
