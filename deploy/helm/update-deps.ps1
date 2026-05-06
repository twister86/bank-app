# Сохрани как deploy/helm/update-deps.ps1
foreach ($svc in @('accounts','cash','transfer','notifications','front-ui')) {
    Write-Host "=== Updating $svc ==="
    Push-Location ".\$svc"
    Remove-Item .\charts\* -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item .\Chart.lock -Force -ErrorAction SilentlyContinue
    helm dependency update
    Pop-Location
}
Push-Location .\bank
Remove-Item .\charts\* -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item .\Chart.lock -Force -ErrorAction SilentlyContinue
helm dependency update
Pop-Location
Write-Host "All charts ready."