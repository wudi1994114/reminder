import { request } from './http.js';

export const dailyRecordsApi = {
  getTrend: ({ metric, end, days }) => request({ url: '/daily-records/trend', data: { metric, end, days } }),
  getMonth: month => request({ url: '/daily-records', data: { month } }),
  getDay: date => request({ url: `/daily-records/${encodeURIComponent(date)}` }),
  saveDay: (date, data) => request({ url: `/daily-records/${encodeURIComponent(date)}`, method: 'PUT', data }),
  deleteDay: (date, version, recordId) => request({ url: `/daily-records/${encodeURIComponent(date)}?version=${encodeURIComponent(version)}&recordId=${encodeURIComponent(recordId)}`, method: 'DELETE' }),
};
