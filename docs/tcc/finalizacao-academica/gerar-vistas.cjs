const fs=require('fs'),path=require('path');
// npm ci em qa/playwright; a variável opcional permite reutilizar outra instalação local.
const {chromium}=require(require.resolve('playwright',{paths:[process.env.COURTTIME_PLAYWRIGHT_HOME||path.resolve(__dirname,'../../../qa/playwright')]}));
const root=path.join(__dirname,'figuras');fs.mkdirSync(root,{recursive:true});
const box=(name,lines)=>({name,lines});
const views=[
 {id:'classes-nucleo',title:'Classes do núcleo de reservas',nodes:[
 box('Usuario',['id: Long','nome: String','email: String','perfil: Perfil','ativo: boolean']),
 box('Quadra',['id: Long','nome: String','descricao: String','ativa: boolean']),
 box('Reserva',['id: Long','dataUtilizacao: LocalDate','horarioInicio / horarioFim: LocalTime','status: StatusReserva','rascunho: boolean','revisaoParticipantes: int','remarcacaoDe: Reserva [0..1]']),
 box('ParticipanteReserva',['id: Long','nomeCompleto: String','rmOuRg: String','categoria: CategoriaParticipante','vinculo: VinculoParticipante [0..1]','presenca: PresencaParticipante [0..1]','adicionadoNaPortaria: Boolean [0..1]'])],edges:[[0,2,'1','0..*','solicitante'],[1,2,'1','0..*','quadra'],[2,3,'1','0..*','participantes']],note:'Atributos selecionados. Remarcação: Reserva → Reserva, 0..1 em cada ponta; FK única.'},
 {id:'classes-documentacao',title:'Classes de documentação e contribuição',nodes:[
 box('Reserva',['id: Long','status: StatusReserva','rascunho: boolean','liberadaEm: Instant [0..1]']),
 box('DocumentoReserva',['id: Long','versao: int','revisaoParticipantes: int','chaveArmazenamento: String','sha256: String','tamanhoBytes: long','enviadoEm: Instant']),
 box('ContribuicaoApm',['id: Long','valor: BigDecimal','valorDevido: BigDecimal [0..1]','creditoAplicado: BigDecimal [0..1]','contribuicaoOrigem: ContribuicaoApm [0..1]','registradaEm: Instant']),
 box('ConferenciaDocumento',['id: Long','resultado: ResultadoConferenciaDocumento','justificativa: String [0..1]','assinaturaGovBrConferida: Boolean [0..1]','conferidoEm: Instant']),
 box('PagamentoAdicional',['id: Long','quantidadePessoas: int','valor: BigDecimal','local: LocalPagamento','registradoEm: Instant','repasseConfirmadoEm: Instant [0..1]']),
 box('AjusteContribuicaoApm',['id: Long','valorAnterior / valorNovo: BigDecimal','motivo: String','ajustadaEm: Instant'])],edges:[[0,1,'1','0..*','documentos'],[1,3,'1','0..1','conferência'],[3,2,'1','0..1','base'],[0,2,'1','0..1','contribuição'],[0,4,'1','0..*','complementos'],[2,5,'1','0..*','ajustes']],note:'Atributos selecionados; operadores referenciam Usuario. Sem composição ou exclusão em cascata.'},
 {id:'der-nucleo',title:'DER — núcleo e agenda',nodes:[
 box('usuarios',['PK id: bigint','UQ NN email: varchar(254)','NN nome: varchar(150)','NN perfil: varchar(20)','NN ativo: boolean']),
 box('quadras',['PK id: bigint','NN nome: varchar(100)','NN descricao: varchar(500)','NN ativa: boolean']),
 box('reservas',['PK id: bigint','FK NN solicitante_id → usuarios.id','FK NN quadra_id → quadras.id','UQ FK remarcacao_de_id → reservas.id','NN data_utilizacao: date','NN horario_inicio / horario_fim: time','NN status: varchar(30)','NN rascunho: boolean DEFAULT false','NN revisao_participantes: integer']),
 box('participantes_reserva',['PK id: bigint','FK NN reserva_id → reservas.id','NN nome_completo: varchar(150)','NN rm_ou_rg: varchar(30)','NN categoria: varchar(10)','vinculo / presenca: varchar','adicionado_na_portaria: boolean']),
 box('disponibilidades_quadra',['PK id: bigint','FK NN quadra_id → quadras.id','NN dia_semana: varchar(10)','NN horario_inicio / horario_fim: time','NN ativa: boolean']),
 box('bloqueios_quadra',['PK id: bigint','FK NN quadra_id → quadras.id','NN data: date','NN horario_inicio / horario_fim: time','NN motivo: varchar(200)','NN ativo: boolean'])],edges:[[0,2,'1','0..*','solicitante'],[1,2,'1','0..*','quadra'],[2,3,'1','0..*','lista'],[1,4,'1','0..*','grade'],[1,5,'1','0..*','bloqueios']],note:'Vista parcial derivada de JPA. PK: chave primária; FK: estrangeira; UQ: única; NN: obrigatória.'},
 {id:'der-documentacao',title:'DER — documentos, contribuições e portaria',nodes:[
 box('reservas (referência)',['PK id: bigint','NN rascunho: boolean DEFAULT false']),
 box('documentos_reserva',['PK id: bigint','FK NN reserva_id → reservas.id','UQ (reserva_id, versao)','UQ NN chave_armazenamento','NN revisao_participantes: integer','NN sha256: varchar(64)','NN tamanho_bytes: bigint','FK NN enviado_por_id → usuarios.id']),
 box('contribuicoes_apm',['PK id: bigint','UQ FK NN reserva_id → reservas.id','UQ FK NN conferencia_documento_id','NN valor: numeric(12,2)','valor_devido / credito_aplicado: numeric','FK contribuicao_origem_id → própria tabela','FK NN registrada_por_id → usuarios.id']),
 box('conferencias_documento',['PK id: bigint','UQ FK NN documento_id','NN resultado: varchar(30)','justificativa: varchar(500)','assinatura_gov_br_conferida: boolean','FK NN conferido_por_id → usuarios.id']),
 box('pagamentos_adicionais',['PK id: bigint','FK NN reserva_id → reservas.id','NN quantidade_pessoas: integer','NN valor: numeric(12,2)','NN local: varchar(12)','FK NN registrado_por_id → usuarios.id','FK repasse_confirmado_por_id → usuarios.id','repasse_confirmado_em: Instant (JPA)']),
 box('ajustes_contribuicao_apm',['PK id: bigint','FK NN contribuicao_id','NN valor_anterior / valor_novo: numeric','NN motivo: varchar(500)','FK NN ajustada_por_id → usuarios.id'])],edges:[[0,1,'1','0..*','versões'],[1,3,'1','0..1','conferência'],[3,2,'1','0..1','base'],[0,2,'1','0..1','contribuição'],[0,4,'1','0..*','complementos'],[2,5,'1','0..*','ajustes']],note:'Vista parcial. Campos opcionais preservam legado; recebimento na portaria não confirma repasse.'}
];
const esc=s=>s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
(async()=>{const browser=await chromium.launch({headless:true});for(const v of views){
 const h=v.nodes.length===4?780:1080,coords=v.nodes.map((_,i)=>({x:35+(i%2)*395,y:90+Math.floor(i/2)*305,w:340,h:245}));
 let s=`<svg xmlns="http://www.w3.org/2000/svg" width="800" height="${h}" viewBox="0 0 800 ${h}"><rect width="800" height="${h}" fill="white"/><style>text{font-family:Arial,sans-serif;fill:#14283c} .line{fill:none;stroke:#40566b;stroke-width:1.5}</style><text x="35" y="36" font-size="24" font-weight="bold">${esc(v.title)}</text><text x="35" y="62" font-size="14">CourtTime · árvore funcional 4fb7a06 · atributos e relações selecionados</text>`;
 let edgeSvg='';for(const [a,b,l,r,label]of v.edges){const A=coords[a],B=coords[b];let x1,y1,x2,y2,pts;
 if(Math.floor(a/2)===Math.floor(b/2)){x1=A.x+(a%2===0?A.w:0);y1=A.y+125;x2=B.x+(b%2===0?B.w:0);y2=B.y+125;pts=`${x1},${y1} ${x2},${y2}`;}
 else if(a%2===b%2){x1=A.x+170;y1=A.y+A.h;x2=B.x+170;y2=B.y;if(Math.abs(a-b)>2){const right=a%2===1;x1=A.x+(right?A.w:0);y1=A.y+190;x2=B.x+(right?B.w:0);y2=B.y+190;const gutter=right?785:14;pts=`${x1},${y1} ${gutter},${y1} ${gutter},${y2} ${x2},${y2}`;}else pts=`${x1},${y1} ${x2},${y2}`;}
 else {x1=A.x+(a%2===0?A.w:0);y1=A.y+218;x2=B.x+(b%2===0?B.w:0);y2=B.y+218;pts=`${x1},${y1} 405,${y1} 405,${y2} ${x2},${y2}`;}
 edgeSvg+=`<polyline class="line" points="${pts}"/><text x="${x1===A.x?x1-16:x1+3}" y="${y1+15}" font-size="13">${l}</text><text x="${x2===B.x?x2-25:x2+3}" y="${y2-5}" font-size="13">${r}</text>`;
 }
 v.nodes.forEach((n,i)=>{const p=coords[i];s+=`<rect x="${p.x}" y="${p.y}" width="${p.w}" height="${p.h}" fill="white" stroke="#17365d" stroke-width="2"/><rect x="${p.x}" y="${p.y}" width="${p.w}" height="36" fill="#e4edf4" stroke="#17365d"/><text x="${p.x+10}" y="${p.y+24}" font-size="17" font-weight="bold">${esc(n.name)}</text>`;const lines=n.lines.flatMap(l=>l.length>43&&l.includes(': ')?[l.split(': ')[0]+':',l.split(': ').slice(1).join(': ')]:[l]);lines.forEach((l,j)=>s+=`<text x="${p.x+10}" y="${p.y+60+j*20}" font-size="${l.length>46?14:16}">${esc(l)}</text>`);});
 s+=edgeSvg; s+=`<text x="35" y="${h-35}" font-size="13">${esc(v.note)}</text><text x="35" y="${h-14}" font-size="13">Fonte: Elaborado pelos autores (2026). Fonte editável: SVG e gerador versionado.</text></svg>`;
 const svg=path.join(root,v.id+'.svg');fs.writeFileSync(svg,s);const page=await browser.newPage({viewport:{width:800,height:h},deviceScaleFactor:2});await page.goto(require('url').pathToFileURL(svg).href);await page.screenshot({path:path.join(root,v.id+'.png')});await page.close();}
 await browser.close();})();
