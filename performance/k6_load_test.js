import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 }, // Ramp-up to 20 concurrent users
    { duration: '1m', target: 20 },  // Steady load
    { duration: '15s', target: 0 },  // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% of requests under 500ms
    http_req_failed: ['rate<0.01'],                 // Error rate < 1%
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080/api';

export function setup() {
  // Login to acquire bearer JWT
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({
      email: 'admin@stockpilot.io',
      password: 'adminpassword',
    }),
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  const token = loginRes.json('data.token');
  return { token };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  // 1. Inventory Catalog Read
  const invRes = http.get(`${BASE_URL}/inventory?page=0&size=20`, { headers });
  check(invRes, {
    'inventory status is 200': (r) => r.status === 200,
    'inventory has records': (r) => r.json('data.content') !== undefined,
  });

  // 2. Stockout Risk Analytics Read
  const riskRes = http.get(`${BASE_URL}/analytics/stockout-risk`, { headers });
  check(riskRes, {
    'risk analytics status is 200': (r) => r.status === 200,
  });

  // 3. System Config / Actuator Health
  const healthRes = http.get(`${BASE_URL}/settings/config`, { headers });
  check(healthRes, {
    'settings config status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
