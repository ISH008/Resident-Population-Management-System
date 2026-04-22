import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 10 },
    { duration: "1m", target: 10 },
    { duration: "30s", target: 0 }
  ],
  thresholds: {
    http_req_duration: ["p(95)<600"],
    http_req_failed: ["rate<0.01"]
  }
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const username = __ENV.USERNAME || "user01";
const password = __ENV.PASSWORD || "123456";

function login() {
  const res = http.post(
    `${baseUrl}/api/v1/auth/login`,
    JSON.stringify({ username, password }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(res, { "login ok": (r) => r.status === 200 && r.json("code") === 0 });
  return res.json("data.token");
}

export default function () {
  const token = login();
  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`
  };

  const meRes = http.get(`${baseUrl}/api/v1/auth/me`, { headers });
  check(meRes, { "me ok": (r) => r.status === 200 && r.json("code") === 0 });

  const profileRes = http.post(
    `${baseUrl}/api/v1/auth/profile`,
    JSON.stringify({
      username,
      realName: "性能测试用户",
      phone: "13800000000"
    }),
    { headers }
  );
  check(profileRes, { "profile update ok": (r) => r.status === 200 && r.json("code") === 0 });

  sleep(1);
}

