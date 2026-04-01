import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 20,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<600', 'p(99)<1200'],
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
            headers: {
                'Content-Type': 'application/json',
                'X-Request-Id': `k6-login-${__VU}-${__ITER}`,
            },
        }
    );

    const loginOk = check(loginRes, {
        'login status is 200': (r) => r.status === 200,
    });

    if (!loginOk) {
        sleep(1);
        return;
    }

    const accessToken = JSON.parse(loginRes.body).accessToken;

    const meRes = http.get(`${BASE_URL}/api/v1/users/me`, {
        headers: {
            Authorization: `Bearer ${accessToken}`,
            'X-Request-Id': `k6-me-${__VU}-${__ITER}`,
        },
    });

    check(meRes, {
        'me status is 200': (r) => r.status === 200,
        'me contains email': (r) => r.body.includes('loadtest@example.com'),
    });

    sleep(1);
}