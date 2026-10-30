param(
    [string]$ChromePath = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
)

$ErrorActionPreference = 'Stop'
$baseDir = $PSScriptRoot
$svgDir = Join-Path $baseDir 'svg'
$htmlDir = Join-Path $baseDir 'impressao'
$pngDir = Join-Path $baseDir 'png'
$pdfDir = Join-Path $baseDir 'pdf'
$profileDir = Join-Path $env:TEMP 'courttime-diagramas-chrome'

if (-not (Test-Path -LiteralPath $ChromePath)) {
    $edgePath = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
    if (Test-Path -LiteralPath $edgePath) {
        $ChromePath = $edgePath
    } else {
        throw "Google Chrome ou Microsoft Edge não encontrado."
    }
}

New-Item -ItemType Directory -Force -Path $pngDir, $pdfDir, $profileDir | Out-Null

$svgs = Get-ChildItem -LiteralPath $svgDir -Filter '*.svg' | Sort-Object Name
foreach ($svg in $svgs) {
    $stem = $svg.BaseName
    $pngPath = Join-Path $pngDir "$stem.png"
    $htmlPath = Join-Path $htmlDir "$stem.html"
    $pdfPath = Join-Path $pdfDir "$stem.pdf"

    $pngArgs = @(
        '--headless=new',
        '--disable-gpu',
        '--hide-scrollbars',
        '--run-all-compositor-stages-before-draw',
        "--user-data-dir=$profileDir",
        '--window-size=3508,2480',
        "--screenshot=$pngPath",
        ([Uri]::new($svg.FullName).AbsoluteUri)
    )
    $pngProcess = Start-Process -FilePath $ChromePath -ArgumentList $pngArgs `
        -Wait -PassThru -NoNewWindow
    if ($pngProcess.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $pngPath)) {
        throw "Falha ao gerar PNG de $($svg.Name)"
    }

    $pdfArgs = @(
        '--headless=new',
        '--disable-gpu',
        '--no-pdf-header-footer',
        "--user-data-dir=$profileDir",
        "--print-to-pdf=$pdfPath",
        ([Uri]::new((Resolve-Path -LiteralPath $htmlPath).Path).AbsoluteUri)
    )
    $pdfProcess = Start-Process -FilePath $ChromePath -ArgumentList $pdfArgs `
        -Wait -PassThru -NoNewWindow
    if ($pdfProcess.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $pdfPath)) {
        throw "Falha ao gerar PDF de $stem"
    }
}

Write-Output "Exportados $($svgs.Count) PNGs em 3508 × 2480 e $($svgs.Count) PDFs vetoriais A4 paisagem."
