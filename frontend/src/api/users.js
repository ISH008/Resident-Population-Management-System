import request from "../utils/request";

export function usersPageApi(params) {
  return request({
    url: "/api/v1/users",
    method: "get",
    params
  });
}

export function rolesApi() {
  return request({
    url: "/api/v1/roles",
    method: "get"
  });
}

export function createUserApi(data) {
  return request({
    url: "/api/v1/users",
    method: "post",
    data
  });
}

export function updateUserApi(id, data) {
  return request({
    url: `/api/v1/users/${id}`,
    method: "put",
    data
  });
}

export function deleteUserApi(id) {
  return request({
    url: `/api/v1/users/${id}`,
    method: "delete"
  });
}
