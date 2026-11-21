<#
  Instala Tan's Huge Trees (1.20.1 Forge) en una instancia de SKLauncher,
  verificando tamaño y SHA-256 antes de dejar el jar en mods/.

  Uso:
    .\install-tht.ps1 -Instance 'D:\Minecraft\SKLauncher\sklauncher-data-v4\instances\test-1'
    .\install-tht.ps1 -File 'D:\descargas\TansHugeTrees-....jar' -Instance '...'   # instalación offline

  Si no pasás -File, descarga desde la release pública v1.1-configmods.
#>

param(
    [string]$Instance = 'D:\Minecraft\SKLauncher\sklauncher-data-v4\instances\test-1',
    [string]$File = ''
)

$ErrorActionPreference = 'Stop'

# ---- Datos del build (release v1.1-configmods) ----
$n   = 'TansHugeTrees-1.8.0-Forge-1.20.1-configmods.jar'
$s   = 64550946
$h   = '2ea601b22fb1730fe8b463459f4d491783207c362ae3b346169bcf360ef3e6e3'
$uri = "https://github.com/Santi-PdR/Config-Mods/releases/download/v1.1-configmods/$n"

$d = Join-Path $Instance 'mods'
if (-not (Test-Path $d)) { New-Item -ItemType Directory -Path $d -Force | Out-Null }

if ($File -ne '') {
    $tmp = $File
    Write-Host "Usando archivo local: $tmp" -ForegroundColor Cyan
} else {
    $tmp = Join-Path $env:TEMP $n
    Write-Host "Descargando $n ..." -ForegroundColor Cyan
    Invoke-WebRequest -UseBasicParsing -Uri $uri -OutFile $tmp
}

$len = (Get-Item $tmp).Length
if ($len -ne $s) {
    Write-Host "TAMAÑO MAL: esperado $s, obtenido $len" -ForegroundColor Red
    exit 1
}

$hash = (Get-FileHash $tmp -Algorithm SHA256).Hash.ToLower()
if ($hash -ne $h) {
    Write-Host "HASH MAL: esperado $h, obtenido $hash" -ForegroundColor Red
    exit 1
}
Write-Host "OK tamano=$len sha256=$hash" -ForegroundColor Green

# Un solo jar del mod: borra builds anteriores (cualquier versión/sufijo)
Get-ChildItem $d -Filter 'TansHugeTrees-*.jar' -ErrorAction SilentlyContinue | Remove-Item -Force
$target = Join-Path $d $n
if ($tmp -ne $target) { Move-Item -Force $tmp $target }

Write-Host "Instalado: $target" -ForegroundColor Green
Write-Host "Acordate de configurar config/tanshugetrees/config.toml (world_gen_dimensions)." -ForegroundColor Yellow
