import { useState, useEffect, useCallback } from 'react';
import { listDeliveries } from '../services/deliveryService';
import Card, { CardBody } from '../components/ui/Card';
import Badge from '../components/ui/Badge';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import { formatDate, truncate, statusBadgeColor } from '../utils/helpers';

export default function DeliveryLogsPage() {
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    listDeliveries(page, 20)
      .then((res) => setData(res.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Delivery Logs</h1>
        <Button variant="secondary" onClick={load}>Refresh</Button>
      </div>

      {loading ? <Spinner /> : (
        <Card>
          <CardBody className="p-0">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Event Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Endpoint</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">HTTP</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Attempt</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Duration</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.content.map((d) => (
                  <>
                    <tr
                      key={d.id}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => setExpanded(expanded === d.id ? null : d.id)}
                    >
                      <td className="px-6 py-3 font-mono text-xs">{d.eventType}</td>
                      <td className="px-6 py-3 text-xs font-mono text-gray-600">{truncate(d.endpointUrl, 40)}</td>
                      <td className="px-6 py-3">
                        <Badge color={statusBadgeColor(d.status)}>{d.status}</Badge>
                      </td>
                      <td className="px-6 py-3 text-xs">{d.responseStatusCode || '—'}</td>
                      <td className="px-6 py-3 text-xs text-center">{d.attemptNumber}</td>
                      <td className="px-6 py-3 text-xs">{d.durationMs != null ? `${d.durationMs}ms` : '—'}</td>
                      <td className="px-6 py-3 text-xs text-gray-500">{formatDate(d.createdAt)}</td>
                    </tr>
                    {expanded === d.id && (
                      <tr key={`${d.id}-detail`}>
                        <td colSpan={7} className="px-6 py-3 bg-gray-50 border-t border-gray-100">
                          {d.errorMessage && (
                            <div className="mb-2">
                              <span className="text-xs font-medium text-red-600">Error: </span>
                              <span className="text-xs text-red-500">{d.errorMessage}</span>
                            </div>
                          )}
                          {d.responseBody && (
                            <div>
                              <span className="text-xs font-medium text-gray-600">Response: </span>
                              <pre className="text-xs text-gray-600 whitespace-pre-wrap mt-1 max-h-32 overflow-auto bg-white p-2 rounded border">
                                {truncate(d.responseBody, 500)}
                              </pre>
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>

            <div className="flex items-center justify-between px-6 py-3 border-t border-gray-200">
              <Button variant="secondary" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
                Previous
              </Button>
              <span className="text-sm text-gray-500">Page {page + 1} of {data.totalPages || 1}</span>
              <Button variant="secondary" onClick={() => setPage((p) => p + 1)} disabled={page >= (data.totalPages || 1) - 1}>
                Next
              </Button>
            </div>
          </CardBody>
        </Card>
      )}
    </div>
  );
}
