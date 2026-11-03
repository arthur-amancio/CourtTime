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
    [string]$BancoEsperado = 'courttime_demo',
    [string]$LogInicializacao
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

# 5. Credenciais: arquivo local ignorado ou variaveis de ambiente --------
# application.properties importa opcionalmente o application-local.properties
# localizado na raiz do projeto. Nao mostrar senhas em texto claro.
$props = @{}
$arquivoLocal = '.\application-local.properties'
if (Test-Path -LiteralPath $arquivoLocal) {
    foreach ($linha in (Get-Content -LiteralPath $arquivoLocal)) {
        if ($linha -match '^\s*([^#!\s][^=]*)=(.*)$') {
            $props[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    Resultado $true 'Configuracao local encontrada' '(senha nao exibida)'
} else {
    Write-Host '  [-]    application-local.properties ausente; verificando variaveis de ambiente' -ForegroundColor DarkGray
}

function ValorConfiguracao($chave, $variavel) {
    # SPRING_* sobrepoe os arquivos. O arquivo importado, por sua vez,
    # sobrepoe os placeholders COURTTIME_* de application.properties.
    $spring = $chave.Replace('.', '_').Replace('-', '_').ToUpperInvariant()
    $direto = [Environment]::GetEnvironmentVariable($spring)
    if ($direto) { return $direto }
    if ($props.ContainsKey($chave)) { return $props[$chave] }
    return [Environment]::GetEnvironmentVariable($variavel)
}
$url = ValorConfiguracao 'spring.datasource.url' 'COURTTIME_DB_URL'
if (-not $url) { $url = 'jdbc:postgresql://localhost:5432/courttime' }
$usuario = ValorConfiguracao 'spring.datasource.username' 'COURTTIME_DB_USERNAME'
if (-not $usuario) { $usuario = 'postgres' }
$senhaBanco = ValorConfiguracao 'spring.datasource.password' 'COURTTIME_DB_PASSWORD'
$senhaConfigurada = -not [string]::IsNullOrWhiteSpace($senhaBanco) -and $senhaBanco -notmatch '^TROQUE_'

Resultado $senhaConfigurada 'Senha PostgreSQL configurada (arquivo local ou ambiente)' `
    $(if (-not $senhaConfigurada) { 'Copie application-local.properties.example e substitua o placeholder.' } else { '(valor nao exibido)' })

# 6. Banco configurado deve ser exatamente o banco isolado de demonstracao.
$nomeBanco = ($url -split '/')[-1] -replace '\?.*$', ''
Resultado ($nomeBanco -eq $BancoEsperado) "Banco configurado e $BancoEsperado" `
    "Encontrado: $nomeBanco"

# 6b. Consulta somente leitura: confere servidor real, banco, usuario e permissoes.
if ($psql -and $senhaConfigurada) {
    $senhaAnterior = $env:PGPASSWORD
    $timeoutAnterior = $env:PGCONNECT_TIMEOUT
    $sslAnterior = $env:PGSSLMODE
    try {
        if (-not $url.StartsWith('jdbc:postgresql://')) { throw 'formato' }
        $uri = [Uri]$url.Substring(5)
        if ($uri.UserInfo -or $uri.Query -match '(?i)(password|user)=') { throw 'credencial na URL' }
        $portaBanco = if ($uri.Port -gt 0) { $uri.Port } else { 5432 }
        if ($nomeBanco -notmatch '^[A-Za-z0-9_]+$') { throw 'nome de banco' }
        $env:PGPASSWORD = $senhaBanco
        $env:PGCONNECT_TIMEOUT = '5'
        if ($uri.Query -match '(?:\?|&)sslmode=([^&]+)') { $env:PGSSLMODE = $Matches[1] }
        $consulta = "SELECT current_database(), current_user, current_setting('server_version'), has_database_privilege(current_user,current_database(),'CONNECT'), has_schema_privilege(current_user,'public','USAGE'), has_schema_privilege(current_user,'public','CREATE');"
        $saida = & $psql.Source -X --no-password -h $uri.Host -p $portaBanco -U $usuario -d $nomeBanco -A -t -F '|' -c $consulta 2>&1
        if ($LASTEXITCODE -ne 0) {
            # Nao imprimir mensagens que possam conter DSN, parametros ou dados privados.
            $texto = $saida -join ' '
            $categoria = if ($texto -match '(?i)password|senha|authenticat') { 'Autenticacao recusada.' }
                elseif ($texto -match '(?i)does not exist|nao existe|não existe') { 'Banco ou usuario nao encontrado.' }
                elseif ($texto -match '(?i)certificate|ssl|tls') { 'Falha de certificado/TLS no cliente psql.' }
                else { 'Servidor indisponivel, porta incorreta ou acesso recusado.' }
            Resultado $false 'Conexao PostgreSQL' $categoria
        } else {
            $campos = (($saida -join '').Trim()) -split '\|'
            Resultado ($campos.Length -eq 6 -and $campos[0] -eq $BancoEsperado) 'Banco realmente conectado' $campos[0]
            Resultado $true 'Usuario PostgreSQL e versao do servidor' "$($campos[1]); PostgreSQL $($campos[2])"
            Resultado ($campos[3] -eq 't' -and $campos[4] -eq 't') 'Permissoes CONNECT e USAGE' "CREATE no schema public: $($campos[5]) (necessario para criar o esquema local)."
            if ($campos[2] -notmatch '^(16|18)\.') {
                Write-Host '  [!]    Versao fora das matrizes 16/18 da CI. Isso nao comprova a causa de um erro; confira o log.' -ForegroundColor Yellow
            }
        }
    } catch {
        Resultado $false 'Parametros da conexao PostgreSQL' 'Confira a URL JDBC e use usuario/senha em campos separados; nenhum valor secreto foi exibido.'
    } finally {
        $env:PGPASSWORD = $senhaAnterior
        $env:PGCONNECT_TIMEOUT = $timeoutAnterior
        $env:PGSSLMODE = $sslAnterior
        $senhaBanco = $null
    }
}

# 7. Variavel perigosa ligada por engano? ------------------------------------
if ($env:COURTTIME_POSTGRES_TEST -eq 'true') {
    Resultado $false 'COURTTIME_POSTGRES_TEST NAO deve ficar ligada na demonstracao' `
        'Com ela ligada, os testes de integracao podem limpar tabelas de um banco dedicado de testes. Use: Remove-Item Env:COURTTIME_POSTGRES_TEST'
} else {
    Resultado $true 'COURTTIME_POSTGRES_TEST desligada' $null
}

# 7b. Esquema sendo recriado ao subir? -------------------------------------
# O CI usa create-drop porque o banco dele e descartavel. Copiada para esta
# maquina, essa configuracao faz a PROPRIA aplicacao apagar todas as tabelas
# do banco configurado ao iniciar, antes de qualquer guarda dos testes.
$ddl = ValorConfiguracao 'spring.jpa.hibernate.ddl-auto' 'SPRING_JPA_HIBERNATE_DDL_AUTO'
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
if ($LogInicializacao) {
    if (Test-Path -LiteralPath $LogInicializacao) {
        $textoLog = (Get-Content -LiteralPath $LogInicializacao -Tail 250) -join ' '
        $categorias = @{
            'Java incompativel' = 'UnsupportedClassVersionError|Unsupported major.minor'
            'Porta HTTP ocupada' = 'Port .* already in use|Address already in use'
            'Conexao PostgreSQL' = 'Connection refused|PSQLException|password authentication failed'
            'Esquema ou permissoes' = 'Schema-validation|permission denied|SQLGrammarException'
            'Bootstrap administrativo' = 'bootstrap.*recusado|administrador ativo'
        }
        foreach ($item in $categorias.GetEnumerator()) {
            if ($textoLog -match $item.Value) { Resultado $false "Log: $($item.Key)" 'Categoria encontrada; preserve o log completo em local privado.' }
        }
        $textoLog = $null
    } else {
        Resultado $false 'Log de inicializacao' 'Arquivo informado nao encontrado.'
    }
}
Write-Host ''
if ($problemas.Count -eq 0) {
    Write-Host 'Ambiente pronto.' -ForegroundColor Green
    exit 0
}
Write-Host "Problemas encontrados ($($problemas.Count)):" -ForegroundColor Yellow
$problemas | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
exit 1
