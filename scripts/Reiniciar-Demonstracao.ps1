<#
.SYNOPSIS
    Reinicia o banco de DEMONSTRACAO do CourtTime, com confirmacao explicita.

.DESCRIPTION
    Apaga e recria um banco PostgreSQL para comecar a demonstracao do zero.

    Protecoes, nesta ordem:
      1. so aceita um nome de banco que termine em "_demo";
      2. recusa nomes de uso comum, como postgres, courttime e courttime_test;
      3. mostra quantas tabelas e quantos usuarios existem no banco antes de
         qualquer coisa;
      4. exige que voce digite o nome do banco para confirmar;
      5. so entao apaga.

    Nenhum outro banco e tocado. Nao exige permissoes de administrador do
    Windows, apenas um usuario PostgreSQL com permissao para criar bancos.

.EXAMPLE
    .\scripts\Reiniciar-Demonstracao.ps1
    .\scripts\Reiniciar-Demonstracao.ps1 -Banco courttime_demo -Usuario postgres
#>
param(
    [string]$Banco = 'courttime_demo',
    [string]$Usuario = 'postgres',
    [string]$Servidor = 'localhost',
    [int]$PortaBanco = 5432
)

$ErrorActionPreference = 'Stop'

# Protecao 1: o nome precisa ser de um banco de demonstracao.
if ($Banco -notmatch '_demo$') {
    Write-Host "RECUSADO: '$Banco' nao termina em '_demo'." -ForegroundColor Red
    Write-Host "Este script so reinicia bancos de demonstracao." -ForegroundColor Red
    exit 1
}

# Protecao 2: nomes que nunca devem ser apagados por aqui.
$proibidos = @('postgres', 'template0', 'template1', 'courttime', 'courttime_test')
if ($proibidos -contains $Banco) {
    Write-Host "RECUSADO: '$Banco' esta na lista de bancos protegidos." -ForegroundColor Red
    exit 1
}

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    Write-Host 'psql nao encontrado no PATH.' -ForegroundColor Red
    exit 1
}

$conexao = @('-h', $Servidor, '-p', $PortaBanco, '-U', $Usuario)

# Protecao 3: mostrar o que existe hoje, para voce reconhecer o banco.
Write-Host "`nBanco alvo: $Banco em $Servidor`:$PortaBanco`n" -ForegroundColor Cyan
$existe = (& psql @conexao -d postgres -tAc `
    "SELECT 1 FROM pg_database WHERE datname = '$Banco'") 2>$null

if ($existe -ne '1') {
    Write-Host "O banco '$Banco' nao existe. Nada a apagar; ele sera criado." -ForegroundColor Yellow
} else {
    $tabelas = (& psql @conexao -d $Banco -tAc `
        "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'") 2>$null
    $usuarios = (& psql @conexao -d $Banco -tAc `
        "SELECT count(*) FROM usuarios") 2>$null
    Write-Host "  tabelas no schema public : $tabelas"
    Write-Host "  contas cadastradas       : $(if ($usuarios) { $usuarios } else { 'tabela ausente' })"
    Write-Host ''
    Write-Host 'Tudo acima sera APAGADO.' -ForegroundColor Yellow

    # Protecao 4: confirmacao digitada, nao apenas "s/n".
    $confirmacao = Read-Host "Digite o nome do banco para confirmar"
    if ($confirmacao -ne $Banco) {
        Write-Host 'Confirmacao nao confere. Nada foi alterado.' -ForegroundColor Green
        exit 0
    }

    & psql @conexao -d postgres -c "DROP DATABASE `"$Banco`";" | Out-Null
    Write-Host "Banco '$Banco' removido." -ForegroundColor DarkGray
}

& psql @conexao -d postgres -c "CREATE DATABASE `"$Banco`";" | Out-Null
Write-Host "Banco '$Banco' criado e vazio." -ForegroundColor Green
Write-Host 'Proximo passo: suba a aplicacao com o bootstrap do administrador.' -ForegroundColor Cyan
