import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 20,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
    const payload = JSON.stringify({
        email: 'loadtest@example.com',
        password: 'StrongPass123',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Request-Id': `k6-login-${__VU}-${__ITER}`,
        },
    };

    const res = http.post(`${BASE_URL}/api/v1/auth/login`, payload, params);

    check(res, {
        'login status is 200': (r) => r.status === 200,
        'login has accessToken': (r) => {
            try {
                return JSON.parse(r.body).accessToken !== undefined;
            } catch (e) {
                return false;
            }
        },
    });

    sleep(1);
}