import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 20,
  duration: "1m",
  thresholds: {
    http_req_duration: ["p(95)<500"],
    http_req_failed: ["rate<0.01"]
  }
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const username = __ENV.USERNAME || "user01";
const password = __ENV.PASSWORD || "123456";

function login() {
  const payload = JSON.stringify({ username, password });
  const res = http.post(`${baseUrl}/api/v1/auth/login`, payload, {
    headers: { "Content-Type": "application/json" }
  });
  check(res, { "login success": (r) => r.status === 200 && r.json("code") === 0 });
  return res.json("data.token");
}

export default function () {
  const token = login();
  const headers = { Authorization: `Bearer ${token}` };

  const resMine = http.get(`${baseUrl}/api/v1/judge-applications/mine?pageNum=1&pageSize=10`, { headers });
  check(resMine, {
    "mine status 200": (r) => r.status === 200,
    "mine code 0": (r) => r.json("code") === 0
  });

  sleep(0.5);
}

