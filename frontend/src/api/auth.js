import request from "../utils/request";

export function loginApi(data) {
  return request({
    url: "/api/v1/auth/login",
    method: "post",
    data
  });
}

export function meApi() {
  return request({
    url: "/api/v1/auth/me",
    method: "get"
  });
}
