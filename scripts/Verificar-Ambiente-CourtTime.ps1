<#
.SYNOPSIS
    Confere se este computador esta pronto para rodar o CourtTime.

.DESCRIPTION
    Script de verificacao. NAO altera nada, NAO cria banco e NAO apaga nada.
    Serve para descobrir problemas antes do ensaio ou da apresentacao, quando
    ainda da tempo de corrigir.

    Nao exige permissoes de administrador.

.EXAMPLE
    .\scripts\Verificar-Ambiente-CourtTime.ps1
    .\scripts\Verificar-Ambiente-CourtTime.ps1 -Porta 8080
#>
param(
    [int]$Porta = 8080,
    [string]$BancoEsperado = 'courttime_demo'
)

$ErrorActionPreference = 'Continue'
$problemas = @()

function Resultado($ok, $titulo, $detalhe) {
    $marca = if ($ok) { '  [ok]  ' } else { '  [FALHA]' }
    Write-Host "$marca $titulo"
    if ($detalhe) { Write-Host "          $detalhe" -ForegroundColor DarkGray }
    if (-not $ok) { $script:problemas += $titulo }
}

Write-Host "`nVerificacao de ambiente do CourtTime`n" -ForegroundColor Cyan

# 1. Java 17 -----------------------------------------------------------------
# O projeto compila com Java 17. Versoes maiores podem funcionar, mas a CI usa
# a 17 e e a unica combinacao realmente testada.
$java = $null
try { $java = (& java -version 2>&1) -join ' ' } catch { }
if (-not $java) {
    Resultado $false 'Java instalado' 'Comando "java" nao encontrado no PATH.'
} else {
    $versao = if ($java -match 'version "(\d+)') { [int]$Matches[1] } else { 0 }
    Resultado ($versao -eq 17) "Java 17 (encontrado: $versao)" `
        $(if ($versao -ne 17) { 'Defina JAVA_HOME para um JDK 17 e refaca o PATH.' } else { $null })
}

# 2. JAVA_HOME ---------------------------------------------------------------
Resultado ([bool]$env:JAVA_HOME) 'JAVA_HOME definido' $env:JAVA_HOME

# 3. Maven Wrapper -----------------------------------------------------------
Resultado (Test-Path '.\mvnw.cmd') 'Maven Wrapper presente' `
    'Rode o script a partir da raiz do repositorio.'

# 4. PostgreSQL --------------------------------------------------------------
$psql = Get-Command psql -ErrorAction SilentlyContinue
Resultado ([bool]$psql) 'Cliente psql disponivel' `
    $(if (-not $psql) { 'Adicione a pasta bin do PostgreSQL ao PATH.' } else { $psql.Source })

# 5. Variaveis de conexao ----------------------------------------------------
Resultado ([bool]$env:COURTTIME_DB_URL) 'COURTTIME_DB_URL definida' $env:COURTTIME_DB_URL
Resultado ([bool]$env:COURTTIME_DB_PASSWORD) 'COURTTIME_DB_PASSWORD definida' `
    $(if ($env:COURTTIME_DB_PASSWORD) { '(valor nao exibido)' } else { 'Defina a senha apenas nesta sessao.' })

# 6. O banco configurado e mesmo o de demonstracao? --------------------------
# Importante: os testes de integracao apagam todas as tabelas. Apontar para um
# banco que nao seja o de demonstracao e o erro mais caro possivel aqui.
if ($env:COURTTIME_DB_URL) {
    $nomeBanco = ($env:COURTTIME_DB_URL -split '/')[-1] -replace '\?.*$', ''
    Resultado ($nomeBanco -eq $BancoEsperado) "Banco configurado e $BancoEsperado" `
        "Encontrado: $nomeBanco"
}

# 7. Variavel perigosa ligada por engano? ------------------------------------
if ($env:COURTTIME_POSTGRES_TEST -eq 'true') {
    Resultado $false 'COURTTIME_POSTGRES_TEST NAO deve ficar ligada na demonstracao' `
        'Com ela ligada, "mvnw clean verify" apaga as tabelas do banco configurado. Use: Remove-Item Env:COURTTIME_POSTGRES_TEST'
} else {
    Resultado $true 'COURTTIME_POSTGRES_TEST desligada' $null
}

# 7b. Esquema sendo recriado ao subir? -------------------------------------
# O CI usa create-drop porque o banco dele e descartavel. Copiada para esta
# maquina, essa configuracao faz a PROPRIA aplicacao apagar todas as tabelas
# do banco configurado ao iniciar, antes de qualquer guarda dos testes.
$ddl = $env:SPRING_JPA_HIBERNATE_DDL_AUTO
if ($ddl -and $ddl -match '^(create|create-drop)$') {
    Resultado $false "SPRING_JPA_HIBERNATE_DDL_AUTO=$ddl apaga o esquema ao iniciar" `
        'Use: Remove-Item Env:SPRING_JPA_HIBERNATE_DDL_AUTO (o padrao do projeto e update)'
} else {
    Resultado $true 'Esquema preservado ao iniciar (ddl-auto padrao)' $null
}

# 8. Bootstrap esquecido ligado? ---------------------------------------------
if ($env:COURTTIME_BOOTSTRAP_ADMIN_ENABLED -eq 'true') {
    Resultado $false 'COURTTIME_BOOTSTRAP_ADMIN_ENABLED ainda ligada' `
        'Se ja existe administrador, a aplicacao falha ao iniciar. Remova a variavel depois da primeira execucao.'
} else {
    Resultado $true 'COURTTIME_BOOTSTRAP_ADMIN_ENABLED desligada' $null
}

# 9. A aplicacao responde? ---------------------------------------------------
# So faz sentido se ela ja estiver no ar; serve para o ensaio.
try {
    $r = Invoke-WebRequest -Uri "http://localhost:$Porta/" -UseBasicParsing -TimeoutSec 4
    Resultado ($r.StatusCode -eq 200) "Aplicacao respondendo em localhost:$Porta" `
        "HTTP $($r.StatusCode)"
} catch {
    Write-Host "  [-]    Aplicacao nao esta no ar em localhost:$Porta (normal antes de inicia-la)" -ForegroundColor DarkGray
}

# Resumo ---------------------------------------------------------------------
Write-Host ''
if ($problemas.Count -eq 0) {
    Write-Host 'Ambiente pronto.' -ForegroundColor Green
    exit 0
}
Write-Host "Problemas encontrados ($($problemas.Count)):" -ForegroundColor Yellow
$problemas | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
exit 1
