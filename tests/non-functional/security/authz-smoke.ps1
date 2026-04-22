$ErrorActionPreference = "Stop"

param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$UserUsername = "user01",
  [string]$UserPassword = "123456"
)

function Login([string]$username, [string]$password) {
  $body = @{ username = $username; password = $password } | ConvertTo-Json
  $resp = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $body
  if ($resp.code -ne 0) {
    throw "Login failed for $username: $($resp.message)"
  }
  return $resp.data.token
}

Write-Host "[1] Login as USER..."
$userToken = Login $UserUsername $UserPassword
$headers = @{ Authorization = "Bearer $userToken" }

Write-Host "[2] USER should be forbidden to access admin residents page..."
$residents = Invoke-RestMethod -Uri "$BaseUrl/api/v1/residents?pageNum=1&pageSize=1" -Method Get -Headers $headers
if ($residents.code -eq 0) {
  throw "Expected USER to be forbidden for /api/v1/residents, but got success."
}
Write-Host "    PASS: blocked with code=$($residents.code)"

Write-Host "[3] USER should only access own applications endpoint..."
$mine = Invoke-RestMethod -Uri "$BaseUrl/api/v1/judge-applications/mine?pageNum=1&pageSize=10" -Method Get -Headers $headers
if ($mine.code -ne 0) {
  throw "Expected USER to access /mine successfully."
}
Write-Host "    PASS: /mine is accessible"

Write-Host "Authz smoke checks completed."

