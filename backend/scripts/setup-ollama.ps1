# Start Redis only (Ollama runs locally via desktop app)
Set-Location $PSScriptRoot\..

docker compose up -d redis

Write-Host "Checking local Ollama..."
try {
    $tags = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 5
    Write-Host "Local Ollama OK. Models:"
    $tags.models | ForEach-Object { Write-Host "  - $($_.name)" }
} catch {
    Write-Error "Local Ollama not reachable. Open the Ollama desktop app first."
    exit 1
}

$model = if ($env:OLLAMA_MODEL) { $env:OLLAMA_MODEL } else { "llama3:latest" }
Write-Host "Using model: $model (set OLLAMA_MODEL in .env to change)"

Write-Host "Running integration test..."
mvn test "-Dtest=OllamaIntegrationTest" -q
