import { useState, useEffect, useCallback } from 'react';
import toast from 'react-hot-toast';
import { listApiKeys, createApiKey, revokeApiKey } from '../services/apiKeyService';
import Button from '../components/ui/Button';
import Input from '../components/ui/Input';
import Card, { CardBody, CardHeader } from '../components/ui/Card';
import Spinner from '../components/ui/Spinner';
import { formatDate } from '../utils/helpers';

export default function ApiKeysPage() {
  const [keys, setKeys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [newKeyName, setNewKeyName] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [revealedKey, setRevealedKey] = useState(null);

  const fetchKeys = useCallback(() => {
    setLoading(true);
    listApiKeys()
      .then((res) => setKeys(res.data))
      .catch(() => toast.error('Failed to load API keys'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchKeys(); }, [fetchKeys]);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!newKeyName.trim()) return;
    setCreating(true);
    try {
      const res = await createApiKey(newKeyName.trim());
      setRevealedKey(res.data);
      setNewKeyName('');
      setShowForm(false);
      fetchKeys();
      toast.success('API key created — copy it now, it won\'t be shown again');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create API key');
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (id, name) => {
    if (!window.confirm(`Revoke key "${name}"? This cannot be undone.`)) return;
    try {
      await revokeApiKey(id);
      toast.success('API key revoked');
      fetchKeys();
    } catch {
      toast.error('Failed to revoke API key');
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">API Keys</h1>
          <p className="text-gray-500 text-sm mt-1">Use API keys to publish events programmatically</p>
        </div>
        <Button onClick={() => setShowForm((v) => !v)}>
          {showForm ? 'Cancel' : '+ New Key'}
        </Button>
      </div>

      {revealedKey && (
        <div className="mb-6 bg-yellow-50 border border-yellow-300 rounded-xl p-4">
          <p className="text-sm font-semibold text-yellow-800 mb-2">
            ⚠ Copy your API key — it will not be shown again
          </p>
          <code className="block break-all bg-white border border-yellow-200 rounded-lg px-3 py-2 text-sm font-mono text-gray-800">
            {revealedKey.key}
          </code>
          <Button
            variant="secondary"
            className="mt-3 text-xs"
            onClick={() => {
              navigator.clipboard.writeText(revealedKey.key);
              toast.success('Copied to clipboard');
            }}
          >
            Copy to Clipboard
          </Button>
          <button
            className="ml-3 text-xs text-gray-500 hover:text-gray-700"
            onClick={() => setRevealedKey(null)}
          >
            Dismiss
          </button>
        </div>
      )}

      {showForm && (
        <Card className="mb-6">
          <CardHeader>
            <h2 className="font-semibold text-gray-800">Create API Key</h2>
          </CardHeader>
          <CardBody>
            <form onSubmit={handleCreate} className="flex gap-3 items-end">
              <div className="flex-1">
                <Input
                  label="Key Name"
                  value={newKeyName}
                  onChange={(e) => setNewKeyName(e.target.value)}
                  placeholder="e.g. Production Integration"
                  required
                />
              </div>
              <Button type="submit" loading={creating}>Create</Button>
            </form>
          </CardBody>
        </Card>
      )}

      {loading ? (
        <Spinner />
      ) : keys.length === 0 ? (
        <Card>
          <CardBody>
            <p className="text-center text-gray-500 py-8">No API keys yet. Create one to start publishing events.</p>
          </CardBody>
        </Card>
      ) : (
        <Card>
          <div className="divide-y divide-gray-100">
            {keys.map((key) => (
              <div key={key.id} className="flex items-center justify-between px-6 py-4">
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900">{key.name}</p>
                  <div className="flex items-center gap-4 mt-1">
                    <code className="text-xs font-mono text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                      {key.keyPrefix}••••••••
                    </code>
                    <span className="text-xs text-gray-400">Created {formatDate(key.createdAt)}</span>
                    {key.lastUsedAt && (
                      <span className="text-xs text-gray-400">Last used {formatDate(key.lastUsedAt)}</span>
                    )}
                  </div>
                </div>
                <Button variant="danger" className="text-xs" onClick={() => handleRevoke(key.id, key.name)}>
                  Revoke
                </Button>
              </div>
            ))}
          </div>
        </Card>
      )}

      <Card className="mt-6">
        <CardHeader>
          <h2 className="font-semibold text-gray-800">Usage</h2>
        </CardHeader>
        <CardBody>
          <p className="text-sm text-gray-600 mb-3">Send events using the <code className="bg-gray-100 px-1 rounded">X-API-Key</code> header:</p>
          <pre className="bg-gray-900 text-green-400 rounded-lg px-4 py-3 text-xs overflow-x-auto">
{`curl -X POST http://localhost:8080/api/v1/events/publish \\
  -H "Content-Type: application/json" \\
  -H "X-API-Key: whk_live_your_key_here" \\
  -d '{
    "eventType": "order.created",
    "payload": { "orderId": "123", "amount": 99.99 }
  }'`}
          </pre>
        </CardBody>
      </Card>
    </div>
  );
}
