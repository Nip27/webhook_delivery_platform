import { useState, useEffect } from 'react';
import { getStats } from '../services/dashboardService';
import Card, { CardBody } from '../components/ui/Card';
import Spinner from '../components/ui/Spinner';

function StatCard({ label, value, color = 'indigo' }) {
  const colors = { indigo: 'text-indigo-600', green: 'text-green-600', red: 'text-red-600', gray: 'text-gray-600' };
  return (
    <Card>
      <CardBody>
        <p className="text-sm text-gray-500 mb-1">{label}</p>
        <p className={`text-3xl font-bold ${colors[color]}`}>{value}</p>
      </CardBody>
    </Card>
  );
}

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getStats()
      .then((res) => setStats(res.data))
      .catch(() => setError('Failed to load stats'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Dashboard</h1>
      <p className="text-gray-500 mb-6">Last 30 days delivery statistics</p>
      {error && <div className="mb-4 text-sm text-red-600 bg-red-50 p-3 rounded-lg">{error}</div>}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard label="Total Events" value={stats?.totalEvents ?? 0} />
        <StatCard label="Total Deliveries" value={stats?.totalDeliveries ?? 0} />
        <StatCard label="Success Rate" value={`${stats?.successRate ?? 0}%`} color="green" />
        <StatCard label="Successful" value={stats?.successfulDeliveries ?? 0} color="green" />
        <StatCard label="Failed" value={stats?.failedDeliveries ?? 0} color="red" />
        <StatCard label="Dead Lettered" value={stats?.deadLetteredDeliveries ?? 0} color="gray" />
      </div>
    </div>
  );
}
