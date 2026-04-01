import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<700', 'p(99)<1200'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    const loginPayload = JSON.stringify({
        email: 'loadtest@example.com',
        password: 'StrongPass123',
    });

    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        loginPayload,
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );

    if (loginRes.status !== 200) {
        sleep(1);
        return;
    }

    const refreshToken = JSON.parse(loginRes.body).refreshToken;

    const refreshRes = http.post(
        `${BASE_URL}/api/v1/auth/refresh`,
        JSON.stringify({ refreshToken }),
        {
            headers: { 'Content-Type': 'application/json' },
        }
    );

    check(refreshRes, {
        'refresh status is 200': (r) => r.status === 200,
        'refresh returned new access token': (r) => {
            try {
                return JSON.parse(r.body).accessToken !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    sleep(1);
}