const axios = require('axios');

const API_BASE = 'http://localhost:8080/api/v1';

async function seed() {
    try {
        console.log('Registering user...');
        const userRes = await axios.post(`${API_BASE}/auth/register`, {
            email: `test${Date.now()}@example.com`,
            password: 'password123',
            fullName: 'Test User'
        });
        
        const token = userRes.data.data.accessToken;
        const config = { headers: { Authorization: `Bearer ${token}` } };
        
        console.log('Creating API key...');
        const apiKeyRes = await axios.post(`${API_BASE}/api-keys`, {
            name: 'Test Key'
        }, config);
        
        console.log('Registering Webhook Endpoint...');
        const endpointRes = await axios.post(`${API_BASE}/endpoints`, {
            name: 'My Test Endpoint',
            url: 'https://httpbin.org/post',
            description: 'Test endpoint',
            isActive: true
        }, config);
        
        const endpointId = endpointRes.data.data.id;
        
        console.log('Creating Subscriptions...');
        await axios.post(`${API_BASE}/endpoints/${endpointId}/subscriptions`, {
            eventTypeName: 'user.created',
            isActive: true
        }, config);
        
        await axios.post(`${API_BASE}/endpoints/${endpointId}/subscriptions`, {
            eventTypeName: 'payment.success',
            isActive: true
        }, config);
        
        console.log('Publishing Events...');
        await axios.post(`${API_BASE}/events`, {
            eventType: 'user.created',
            payload: { userId: 123, role: 'admin' },
            idempotencyKey: `user-created-${Date.now()}`
        }, config);
        
        await axios.post(`${API_BASE}/events`, {
            eventType: 'payment.success',
            payload: { amount: 5000, currency: 'USD' },
            idempotencyKey: `payment-${Date.now()}`
        }, config);
        
        console.log('Done! Dashboard is populated.');
    } catch (e) {
        console.error('Error seeding data:', e.response?.data || e.message);
    }
}

seed();
