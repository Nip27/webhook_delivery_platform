import { useState, useEffect, useCallback } from 'react';
import { listEndpoints, createEndpoint, deleteEndpoint } from '../services/endpointService';
import Card, { CardHeader, CardBody } from '../components/ui/Card';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Spinner from '../components/ui/Spinner';
import { formatDate, truncate } from '../utils/helpers';

export default function EndpointsPage() {
  const [endpoints, setEndpoints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ url: '', description: '' });
  const [creating, setCreating] = useState(false);
  const [newSecret, setNewSecret] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    listEndpoints()
      .then((res) => setEndpoints(res.data))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (e) => {
    e.preventDefault(); setCreating(true); setError('');
    try {
      const res = await createEndpoint(form.url, form.description);
      setNewSecret(res.data.secret);
      setForm({ url: '', description: '' });
      setShowForm(false);
      load();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create endpoint');
    } finally { setCreating(false); }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this endpoint? This cannot be undone.')) return;
    await deleteEndpoint(id).catch(console.error);
    load();
  };

  if (loading) return <Spinner />;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Webhook Endpoints</h1>
        <Button onClick={() => setShowForm(true)}>+ Add Endpoint</Button>
      </div>

      {newSecret && (
        <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg">
          <p className="text-sm font-medium text-green-800 mb-1">✅ Endpoint created! Save this signing secret — it won't be shown again.</p>
          <code className="text-xs bg-white px-3 py-2 rounded border border-green-200 block break-all">{newSecret}</code>
          <Button variant="ghost" className="mt-2 text-xs" onClick={() => setNewSecret(null)}>I've saved it</Button>
        </div>
      )}

      {showForm && (
        <Card className="mb-6">
          <CardHeader><h2 className="font-semibold">Register New Endpoint</h2></CardHeader>
          <CardBody>
            <form onSubmit={handleCreate} className="space-y-4 max-w-lg">
              <Input label="Endpoint URL" type="url" value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} placeholder="https://your-app.com/webhooks" required />
              <Input label="Description (optional)" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} placeholder="Production webhook receiver" />
              {error && <p className="text-sm text-red-600">{error}</p>}
              <div className="flex gap-2">
                <Button type="submit" loading={creating}>Create</Button>
                <Button type="button" variant="secondary" onClick={() => setShowForm(false)}>Cancel</Button>
              </div>
            </form>
          </CardBody>
        </Card>
      )}

      <Card>
        <CardBody className="p-0">
          {endpoints.length === 0 ? (
            <div className="text-center py-12 text-gray-500">No endpoints yet. Add your first endpoint above.</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">URL</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Secret Prefix</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Created</th>
                  <th className="px-6 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {endpoints.map((ep) => (
                  <tr key={ep.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 font-mono text-xs">{truncate(ep.url, 60)}</td>
                    <td className="px-6 py-4 text-gray-600">{ep.description || '—'}</td>
                    <td className="px-6 py-4 font-mono text-xs text-gray-500">{ep.secretPrefix}...</td>
                    <td className="px-6 py-4 text-gray-500 text-xs">{formatDate(ep.createdAt)}</td>
                    <td className="px-6 py-4 text-right">
                      <Button variant="danger" className="text-xs py-1" onClick={() => handleDelete(ep.id)}>Delete</Button>
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
