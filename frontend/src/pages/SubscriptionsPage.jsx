import { useState, useEffect, useCallback } from 'react';
import { listSubscriptions, createSubscription, deleteSubscription } from '../services/subscriptionService';
import { listEndpoints } from '../services/endpointService';
import Card, { CardBody } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Spinner from '../components/ui/Spinner';
import Badge from '../components/ui/Badge';
import { formatDate, truncate } from '../utils/helpers';

export default function SubscriptionsPage() {
  const [subscriptions, setSubscriptions] = useState([]);
  const [endpoints, setEndpoints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ webhookEndpointId: '', eventTypeName: '' });
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [subRes, epRes] = await Promise.all([listSubscriptions(), listEndpoints()]);
      setSubscriptions(subRes.data);
      setEndpoints(epRes.data);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (e) => {
    e.preventDefault(); setError(''); setCreating(true);
    try {
      await createSubscription(form.webhookEndpointId, form.eventTypeName);
      setForm({ webhookEndpointId: '', eventTypeName: '' });
      setShowForm(false);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create subscription');
    } finally { setCreating(false); }
  };

  const handleDelete = async (id) => {
    if (!confirm('Remove this subscription?')) return;
    await deleteSubscription(id).catch(console.error);
    load();
  };

  if (loading) return <Spinner />;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Subscriptions</h1>
        <Button onClick={() => setShowForm(true)}>+ Add Subscription</Button>
      </div>

      {showForm && (
        <Card className="mb-6">
          <CardBody>
            <form onSubmit={handleCreate} className="space-y-4 max-w-md">
              <div>
                <label className="text-sm font-medium text-gray-700 block mb-1">Webhook Endpoint</label>
                <select
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                  value={form.webhookEndpointId}
                  onChange={(e) => setForm({ ...form, webhookEndpointId: e.target.value })}
                  required
                >
                  <option value="">Select endpoint...</option>
                  {endpoints.map((ep) => (
                    <option key={ep.id} value={ep.id}>{truncate(ep.url, 60)}</option>
                  ))}
                </select>
              </div>
              <Input label="Event Type" value={form.eventTypeName} onChange={(e) => setForm({ ...form, eventTypeName: e.target.value })} placeholder="e.g. payment.success" required />
              {error && <p className="text-sm text-red-600">{error}</p>}
              <div className="flex gap-2">
                <Button type="submit" loading={creating}>Subscribe</Button>
                <Button type="button" variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button>
              </div>
            </form>
          </CardBody>
        </Card>
      )}

      <Card>
        <CardBody className="p-0">
          {subscriptions.length === 0 ? (
            <div className="text-center py-12 text-gray-500">No subscriptions yet. Create one to start receiving events.</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Event Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Endpoint</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Created</th>
                  <th className="px-6 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {subscriptions.map((s) => (
                  <tr key={s.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 font-mono text-xs font-medium">{s.eventTypeName}</td>
                    <td className="px-6 py-4 text-xs text-gray-600 font-mono">{truncate(s.endpointUrl, 50)}</td>
                    <td className="px-6 py-4">
                      <Badge color={s.isActive ? 'green' : 'gray'}>{s.isActive ? 'Active' : 'Inactive'}</Badge>
                    </td>
                    <td className="px-6 py-4 text-xs text-gray-500">{formatDate(s.createdAt)}</td>
                    <td className="px-6 py-4 text-right">
                      <Button variant="danger" className="text-xs py-1" onClick={() => handleDelete(s.id)}>Remove</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
