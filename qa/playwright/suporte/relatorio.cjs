const fs = require('fs');

class Relatorio {
  constructor(nome, sha) {
    this.nome = nome;
    this.sha = sha;
    this.resultados = [];
  }

  registrar(status, nome, detalhe = '', metadados = {}) {
    const item = { status, nome, detalhe, ...metadados };
    this.resultados.push(item);
    process.stdout.write(`${status} | ${nome}${detalhe ? ` | ${detalhe}` : ''}\n`);
    return item;
  }

  verificar(condicao, nome, detalhe = '', metadados = {}) {
    if (!condicao) {
      this.registrar('FAIL', nome, detalhe, metadados);
      throw new Error(`${nome}${detalhe ? `: ${detalhe}` : ''}`);
    }
    return this.registrar('PASS', nome, detalhe, metadados);
  }

  conferir(condicao, nome, detalhe = '', metadados = {}) {
    return this.registrar(condicao ? 'PASS' : 'FAIL', nome, detalhe, metadados);
  }

  pendente(nome, detalhe = '', metadados = {}) {
    return this.registrar('PENDING', nome, detalhe, metadados);
  }

  salvar(caminho, extras = {}) {
    const contagem = Object.fromEntries(
      ['PASS', 'FAIL', 'PENDING'].map(status => [
        status.toLowerCase(),
        this.resultados.filter(resultado => resultado.status === status).length,
      ])
    );
    fs.writeFileSync(caminho, JSON.stringify({
      suite: this.nome,
      sha: this.sha,
      contagem,
      resultados: this.resultados,
      ...extras,
    }, null, 2));
    return contagem;
  }
}

module.exports = { Relatorio };
