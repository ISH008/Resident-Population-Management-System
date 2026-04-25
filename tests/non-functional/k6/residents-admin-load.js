import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "20s", target: 5 },
    { duration: "40s", target: 15 },
    { duration: "20s", target: 0 }
  ],
  thresholds: {
    http_req_duration: ["p(95)<700"],
    http_req_failed: ["rate<0.01"]
  }
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const username = __ENV.ADMIN_USERNAME || "admin";
const password = __ENV.ADMIN_PASSWORD || "123456";

function loginAsAdmin() {
  const res = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ username, password }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(res, { "admin login ok": (r) => r.status === 200 && r.json("code") === 0 });
  return res.json("data.token");
}

export default function () {
  const token = loginAsAdmin();
  const headers = { Authorization: `Bearer ${token}` };
  const res = http.get(`${baseUrl}/api/v1/residents?pageNum=1&pageSize=10`, { headers });
  check(res, {
    "resident page status 200": (r) => r.status === 200,
    "resident page code 0": (r) => r.json("code") === 0
  });
  sleep(1);
}
