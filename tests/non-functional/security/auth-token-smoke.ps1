$ErrorActionPreference = "Stop"

param(
  [string]$BaseUrl = "http://localhost:8080"
)

function Assert-Unauthorized([string]$path, [hashtable]$headers, [string]$title) {
  Write-Host "[CHECK] $title"
  $resp = Invoke-RestMethod -Uri "$BaseUrl$path" -Method Get -Headers $headers
  if ($resp.code -eq 0) {
    throw "Expected unauthorized response for $path, but got success."
  }
  if ($resp.code -ne 4003) {
    throw "Expected code=4003 for $path, actual code=$($resp.code)"
  }
  Write-Host "    PASS: blocked with code=$($resp.code)"
}

Assert-Unauthorized "/api/v1/auth/me" @{} "No token should be rejected"
Assert-Unauthorized "/api/v1/auth/me" @{ Authorization = "Bearer invalid-token" } "Invalid token should be rejected"
Assert-Unauthorized "/api/v1/residents?pageNum=1&pageSize=1" @{ Authorization = "Bearer fake.jwt.payload" } "Forged token should be rejected"

Write-Host "Auth token smoke checks completed."
