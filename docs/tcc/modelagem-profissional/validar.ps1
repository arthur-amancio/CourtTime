$ErrorActionPreference = 'Stop'
$baseDir = $PSScriptRoot
$manifest = Get-Content -Raw -Encoding UTF8 (Join-Path $baseDir 'manifesto.json') | ConvertFrom-Json

if ($manifest.diagrams.Count -ne 9) {
    throw "O manifesto deve conter 9 diagramas."
}

$combinedPath = Join-Path $baseDir 'fontes\courttime-modelagem-profissional.drawio'
[xml]$combined = Get-Content -Raw -Encoding UTF8 $combinedPath
if (@($combined.mxfile.diagram).Count -ne 9) {
    throw "O fonte multipagina deve conter 9 paginas editaveis."
}

foreach ($diagram in $manifest.diagrams) {
    $drawio = Join-Path $baseDir $diagram.editableSource
    $svg = Join-Path $baseDir $diagram.vector
    $png = Join-Path $baseDir "png\$($diagram.file).png"
    $pdf = Join-Path $baseDir "pdf\$($diagram.file).pdf"

    foreach ($required in @($drawio, $svg, $png, $pdf)) {
        if (-not (Test-Path -LiteralPath $required)) {
            throw "Entrega ausente: $required"
        }
        if ((Get-Item -LiteralPath $required).Length -le 1000) {
            throw "Entrega pequena demais para ser válida: $required"
        }
    }

    [xml]$drawioXml = Get-Content -Raw -Encoding UTF8 $drawio
    if (@($drawioXml.mxfile.diagram).Count -ne 1) {
        throw "O fonte individual deve conter exatamente uma pagina: $drawio"
    }
    if ($drawioXml.SelectNodes('//mxCell[@vertex="1"]').Count -eq 0 -or
            $drawioXml.SelectNodes('//mxCell[@edge="1"]').Count -eq 0) {
        throw "O fonte nao contem formas e conectores editaveis: $drawio"
    }

    [xml]$svgXml = Get-Content -Raw -Encoding UTF8 $svg
    if ($svgXml.svg.width -ne '3508' -or $svgXml.svg.height -ne '2480' -or
            $svgXml.svg.viewBox -ne '0 0 1123 794') {
        throw "O SVG nao usa o canvas A4 definido: $svg"
    }

    $pngBytes = [System.IO.File]::ReadAllBytes($png)
    $pngWidth = [System.BitConverter]::ToUInt32(@($pngBytes[19], $pngBytes[18], $pngBytes[17], $pngBytes[16]), 0)
    $pngHeight = [System.BitConverter]::ToUInt32(@($pngBytes[23], $pngBytes[22], $pngBytes[21], $pngBytes[20]), 0)
    if ($pngWidth -ne 3508 -or $pngHeight -ne 2480) {
        throw "O PNG nao possui 3508 x 2480 pixels: $png"
    }

    $pdfBytes = [System.IO.File]::ReadAllBytes($pdf)
    $pdfHeader = [System.Text.Encoding]::ASCII.GetString($pdfBytes, 0, 5)
    if ($pdfHeader -ne '%PDF-') {
        throw "Cabecalho PDF invalido: $pdf"
    }
}

$classSource = Get-Content -Raw -Encoding UTF8 `
    (Join-Path $baseDir 'svg\03-classes-nucleo-reservas.svg')
$erdSource = Get-Content -Raw -Encoding UTF8 `
    (Join-Path $baseDir 'svg\05-der-postgresql.svg')
$releaseSource = Get-Content -Raw -Encoding UTF8 `
    (Join-Path $baseDir 'svg\08-atividade-liberacao-quadra.svg')

foreach ($term in @('0..*', '0..1', 'Reserva', 'ParticipanteReserva')) {
    if (-not $classSource.Contains($term)) {
        throw "Elemento esperado ausente no diagrama de classes: $term"
    }
}
foreach ($term in @('PK', 'FK', 'UQ', 'pagamentos_adicionais', 'conferencias_documento')) {
    if (-not $erdSource.Contains($term)) {
        throw "Elemento esperado ausente no DER: $term"
    }
}
foreach ($term in @('10 presentes', 'congelar', 'duplicada')) {
    if (-not $releaseSource.Contains($term)) {
        throw "Regra esperada ausente na liberação: $term"
    }
}

Write-Output 'Validacao estrutural concluida: 9 fontes editaveis, SVG A4, PNG 3508 x 2480 e PDF validos.'
