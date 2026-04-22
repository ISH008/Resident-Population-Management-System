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

export function registerApi(data) {
  return request({
    url: "/api/v1/auth/register",
    method: "post",
    data
  });
}

export function changePasswordApi(data) {
  return request({
    url: "/api/v1/auth/change-password",
    method: "post",
    data
  });
}

export function updateProfileApi(data) {
  return request({
    url: "/api/v1/auth/profile",
    method: "post",
    data
  });
}
