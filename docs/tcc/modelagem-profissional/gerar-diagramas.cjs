/*
 * Gera as fontes editáveis do diagrams.net e as versões SVG dos diagramas.
 * Não usa dependências externas. Execute com: node gerar-diagramas.cjs
 */

const fs = require('fs');
const path = require('path');

const ROOT = __dirname;
const DIRS = {
  fontes: path.join(ROOT, 'fontes'),
  svg: path.join(ROOT, 'svg'),
  impressao: path.join(ROOT, 'impressao')
};

Object.values(DIRS).forEach((dir) => fs.mkdirSync(dir, { recursive: true }));

const W = 1123;
const H = 794;
const COLORS = {
  ink: '#172033',
  navy: '#17365D',
  blue: '#DCEAF7',
  teal: '#0E7490',
  tealLight: '#DDF3F5',
  gold: '#A16207',
  goldLight: '#FEF3C7',
  red: '#B42318',
  redLight: '#FDE8E7',
  green: '#347A46',
  greenLight: '#E5F4E8',
  gray: '#667085',
  grayLight: '#F2F4F7',
  line: '#475467',
  white: '#FFFFFF'
};

function xml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;');
}

function svgText(value) {
  return xml(value);
}

function n(id, type, x, y, w, h, title, lines = [], extra = {}) {
  return { id, type, x, y, w, h, title, lines, ...extra };
}

function e(id, source, target, points, extra = {}) {
  return { id, source, target, points, ...extra };
}

function pt(x, y) {
  return [x, y];
}

function wrap(text, max = 24) {
  const explicit = String(text).split('\n');
  const result = [];
  for (const paragraph of explicit) {
    const words = paragraph.split(/\s+/);
    let line = '';
    for (const word of words) {
      if (!line) {
        line = word;
      } else if (`${line} ${word}`.length <= max) {
        line += ` ${word}`;
      } else {
        result.push(line);
        line = word;
      }
    }
    if (line) result.push(line);
  }
  return result;
}

function textLines(lines, x, y, options = {}) {
  const {
    size = 13,
    weight = 400,
    anchor = 'start',
    color = COLORS.ink,
    lineHeight = size * 1.25,
    italic = false
  } = options;
  const content = lines.map((line, index) =>
    `<tspan x="${x}" dy="${index === 0 ? 0 : lineHeight}">${svgText(line)}</tspan>`
  ).join('');
  return `<text x="${x}" y="${y}" text-anchor="${anchor}" font-size="${size}" font-weight="${weight}" fill="${color}"${italic ? ' font-style="italic"' : ''}>${content}</text>`;
}

function polyline(points, options = {}) {
  const {
    dashed = false,
    arrow = false,
    openArrow = false,
    color = COLORS.line,
    width = 1.5
  } = options;
  const marker = arrow ? ' marker-end="url(#arrow)"' : openArrow ? ' marker-end="url(#open-arrow)"' : '';
  return `<polyline points="${points.map((p) => p.join(',')).join(' ')}" fill="none" stroke="${color}" stroke-width="${width}"${dashed ? ' stroke-dasharray="6 5"' : ''}${marker}/>`;
}

function svgNode(node) {
  const { x, y, w, h, title, lines, type } = node;
  if (type === 'actor') {
    const cx = x + w / 2;
    return [
      `<circle cx="${cx}" cy="${y + 14}" r="10" fill="white" stroke="${COLORS.navy}" stroke-width="2"/>`,
      `<line x1="${cx}" y1="${y + 24}" x2="${cx}" y2="${y + 55}" stroke="${COLORS.navy}" stroke-width="2"/>`,
      `<line x1="${cx - 18}" y1="${y + 37}" x2="${cx + 18}" y2="${y + 37}" stroke="${COLORS.navy}" stroke-width="2"/>`,
      `<line x1="${cx}" y1="${y + 55}" x2="${cx - 15}" y2="${y + 75}" stroke="${COLORS.navy}" stroke-width="2"/>`,
      `<line x1="${cx}" y1="${y + 55}" x2="${cx + 15}" y2="${y + 75}" stroke="${COLORS.navy}" stroke-width="2"/>`,
      textLines(wrap(title, 18), cx, y + 94, { anchor: 'middle', size: 13, weight: 700, lineHeight: 15 })
    ].join('');
  }
  if (type === 'usecase') {
    const wrapped = wrap(title, node.wrap || 25);
    const start = y + h / 2 - ((wrapped.length - 1) * 8);
    return `<ellipse cx="${x + w / 2}" cy="${y + h / 2}" rx="${w / 2}" ry="${h / 2}" fill="${node.fill || COLORS.white}" stroke="${node.stroke || COLORS.navy}" stroke-width="1.8"/>`
      + textLines(wrapped, x + w / 2, start, { anchor: 'middle', size: 13, weight: 600, lineHeight: 16 });
  }
  if (type === 'boundary') {
    return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="5" fill="none" stroke="${COLORS.navy}" stroke-width="2"/>`
      + `<rect x="${x}" y="${y}" width="${w}" height="27" rx="5" fill="${COLORS.blue}" stroke="none"/>`
      + textLines([title], x + 12, y + 19, { size: 13, weight: 700, color: COLORS.navy });
  }
  if (type === 'class' || type === 'entity') {
    const header = node.header || (type === 'entity' ? COLORS.tealLight : COLORS.blue);
    const stroke = node.stroke || (type === 'entity' ? COLORS.teal : COLORS.navy);
    const headerH = node.stereotype ? 38 : 29;
    const bodyY = y + headerH;
    let out = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="3" fill="white" stroke="${stroke}" stroke-width="1.6"/>`;
    out += `<rect x="${x}" y="${y}" width="${w}" height="${headerH}" rx="3" fill="${header}" stroke="none"/>`;
    if (node.stereotype) out += textLines([`«${node.stereotype}»`], x + w / 2, y + 13, { anchor: 'middle', size: 10, color: COLORS.gray });
    out += textLines([title], x + w / 2, y + (node.stereotype ? 29 : 20), { anchor: 'middle', size: 13, weight: 700, color: stroke });
    out += `<line x1="${x}" y1="${bodyY}" x2="${x + w}" y2="${bodyY}" stroke="${stroke}" stroke-width="1"/>`;
    const size = node.fontSize || 10.5;
    const lineHeight = node.lineHeight || 13.5;
    out += textLines(lines, x + 7, bodyY + 14, { size, lineHeight, color: COLORS.ink });
    return out;
  }
  if (type === 'lane') {
    return `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${node.fill || COLORS.white}" stroke="${COLORS.line}" stroke-width="1"/>`
      + `<rect x="${x}" y="${y}" width="${node.labelWidth || 96}" height="${h}" fill="${node.header || COLORS.grayLight}" stroke="${COLORS.line}" stroke-width="1"/>`
      + textLines(wrap(title, 14), x + (node.labelWidth || 96) / 2, y + h / 2 - 7, { anchor: 'middle', size: 12, weight: 700, lineHeight: 15 });
  }
  if (type === 'action') {
    const wrapped = wrap(title, node.wrap || 24);
    const start = y + h / 2 - ((wrapped.length - 1) * 7);
    return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="10" fill="${node.fill || COLORS.white}" stroke="${node.stroke || COLORS.navy}" stroke-width="1.6"/>`
      + textLines(wrapped, x + w / 2, start, { anchor: 'middle', size: node.fontSize || 11.5, weight: 600, lineHeight: 14 });
  }
  if (type === 'decision') {
    const cx = x + w / 2;
    const cy = y + h / 2;
    const wrapped = wrap(title, node.wrap || 16);
    return `<polygon points="${cx},${y} ${x + w},${cy} ${cx},${y + h} ${x},${cy}" fill="${node.fill || COLORS.goldLight}" stroke="${node.stroke || COLORS.gold}" stroke-width="1.6"/>`
      + textLines(wrapped, cx, cy - ((wrapped.length - 1) * 6), { anchor: 'middle', size: node.fontSize || 10.5, weight: 700, lineHeight: 12 });
  }
  if (type === 'start') {
    return `<circle cx="${x + w / 2}" cy="${y + h / 2}" r="${Math.min(w, h) / 2}" fill="${COLORS.ink}"/>`;
  }
  if (type === 'end') {
    const r = Math.min(w, h) / 2;
    return `<circle cx="${x + w / 2}" cy="${y + h / 2}" r="${r}" fill="white" stroke="${COLORS.ink}" stroke-width="2"/>`
      + `<circle cx="${x + w / 2}" cy="${y + h / 2}" r="${r - 5}" fill="${COLORS.ink}"/>`;
  }
  if (type === 'note') {
    return `<path d="M ${x} ${y} H ${x + w - 14} L ${x + w} ${y + 14} V ${y + h} H ${x} Z" fill="${node.fill || COLORS.goldLight}" stroke="${node.stroke || COLORS.gold}" stroke-width="1.2"/>`
      + `<path d="M ${x + w - 14} ${y} V ${y + 14} H ${x + w}" fill="none" stroke="${node.stroke || COLORS.gold}" stroke-width="1.2"/>`
      + textLines(lines.length ? lines : wrap(title, 30), x + 8, y + 18, { size: node.fontSize || 10.5, lineHeight: 13 });
  }
  if (type === 'component' || type === 'database' || type === 'external') {
    const fill = node.fill || (type === 'external' ? COLORS.grayLight : COLORS.blue);
    const stroke = node.stroke || (type === 'external' ? COLORS.gray : COLORS.navy);
    let shape;
    if (type === 'database') {
      shape = `<path d="M ${x} ${y + 12} C ${x} ${y - 4}, ${x + w} ${y - 4}, ${x + w} ${y + 12} V ${y + h - 12} C ${x + w} ${y + h + 4}, ${x} ${y + h + 4}, ${x} ${y + h - 12} Z" fill="${fill}" stroke="${stroke}" stroke-width="1.6"/>`
        + `<ellipse cx="${x + w / 2}" cy="${y + 12}" rx="${w / 2}" ry="12" fill="${fill}" stroke="${stroke}" stroke-width="1.6"/>`;
    } else {
      shape = `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="5" fill="${fill}" stroke="${stroke}" stroke-width="1.6"/>`;
    }
    const wrapped = wrap(title, node.wrap || 25);
    shape += textLines(wrapped, x + w / 2, y + h / 2 - ((wrapped.length - 1) * 7), { anchor: 'middle', size: 12, weight: 700, lineHeight: 15, color: stroke });
    if (lines.length) shape += textLines(lines, x + w / 2, y + h - 12 - ((lines.length - 1) * 12), { anchor: 'middle', size: 9.5, lineHeight: 12, color: COLORS.gray });
    return shape;
  }
  return '';
}

function svgEdge(edge) {
  let out = polyline(edge.points, {
    dashed: edge.dashed,
    arrow: edge.arrow,
    openArrow: edge.openArrow,
    color: edge.color,
    width: edge.width
  });
  if (edge.label) {
    const [x, y] = edge.labelPos || edge.points[Math.floor(edge.points.length / 2)];
    out += `<rect x="${x - (edge.labelWidth || 48) / 2}" y="${y - 11}" width="${edge.labelWidth || 48}" height="16" rx="2" fill="white" opacity="0.92"/>`;
    out += textLines([edge.label], x, y + 1, { anchor: 'middle', size: 10, weight: 600, color: COLORS.gray });
  }
  if (edge.startLabel) {
    const [x, y] = edge.startLabelPos || edge.points[0];
    out += textLines([edge.startLabel], x, y, { anchor: 'middle', size: 10, weight: 700, color: COLORS.navy });
  }
  if (edge.endLabel) {
    const [x, y] = edge.endLabelPos || edge.points[edge.points.length - 1];
    out += textLines([edge.endLabel], x, y, { anchor: 'middle', size: 10, weight: 700, color: COLORS.navy });
  }
  return out;
}

function renderSvg(diagram) {
  const backgrounds = diagram.nodes.filter((node) => node.type === 'lane' || node.type === 'boundary');
  const foreground = diagram.nodes.filter((node) => node.type !== 'lane' && node.type !== 'boundary');
  const content = [
    `<?xml version="1.0" encoding="UTF-8"?>`,
    `<svg xmlns="http://www.w3.org/2000/svg" width="3508" height="2480" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`,
    `<title id="title">${svgText(diagram.title)}</title>`,
    `<desc id="desc">${svgText(diagram.subtitle)}</desc>`,
    `<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="${COLORS.line}"/></marker><marker id="open-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10" fill="white" stroke="${COLORS.line}" stroke-width="1.5"/></marker></defs>`,
    `<rect width="${W}" height="${H}" fill="white"/>`,
    textLines([diagram.title], 28, 30, { size: 21, weight: 700, color: COLORS.navy }),
    textLines([diagram.subtitle], 28, 50, { size: 10.5, color: COLORS.gray }),
    `<line x1="28" y1="59" x2="1095" y2="59" stroke="${COLORS.navy}" stroke-width="1.5"/>`,
    backgrounds.map(svgNode).join(''),
    diagram.edges.map(svgEdge).join(''),
    foreground.map(svgNode).join(''),
    `<line x1="28" y1="739" x2="1095" y2="739" stroke="${COLORS.gray}" stroke-width="0.8"/>`,
    textLines([`Figura ${diagram.figure} – ${diagram.caption}`], W / 2, 760, { anchor: 'middle', size: 11.5, weight: 700 }),
    textLines(['Fonte: Elaborado pelos autores (2026).'], W / 2, 778, { anchor: 'middle', size: 10, color: COLORS.gray }),
    `</svg>`
  ];
  return content.join('\n');
}

function drawioStyle(node) {
  const base = 'html=1;whiteSpace=wrap;';
  switch (node.type) {
    case 'actor': return `${base}shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;align=center;fontStyle=1;fontColor=${COLORS.navy};strokeColor=${COLORS.navy};`;
    case 'usecase': return `${base}ellipse;align=center;verticalAlign=middle;fontStyle=1;fillColor=${node.fill || COLORS.white};strokeColor=${node.stroke || COLORS.navy};strokeWidth=2;`;
    case 'boundary': return `${base}swimlane;startSize=27;horizontal=1;rounded=0;fillColor=${COLORS.blue};swimlaneFillColor=${COLORS.white};strokeColor=${COLORS.navy};strokeWidth=2;fontStyle=1;`;
    case 'class': return `${base}swimlane;startSize=${node.stereotype ? 38 : 29};horizontal=1;rounded=0;fillColor=${node.header || COLORS.blue};swimlaneFillColor=${COLORS.white};strokeColor=${node.stroke || COLORS.navy};fontStyle=1;`;
    case 'entity': return `${base}swimlane;startSize=29;horizontal=1;rounded=0;fillColor=${node.header || COLORS.tealLight};swimlaneFillColor=${COLORS.white};strokeColor=${node.stroke || COLORS.teal};fontStyle=1;`;
    case 'lane': return `${base}swimlane;startSize=${node.labelWidth || 96};horizontal=0;fillColor=${node.header || COLORS.grayLight};swimlaneFillColor=${node.fill || COLORS.white};strokeColor=${COLORS.line};fontStyle=1;`;
    case 'action': return `${base}rounded=1;arcSize=18;align=center;verticalAlign=middle;fontStyle=1;fillColor=${node.fill || COLORS.white};strokeColor=${node.stroke || COLORS.navy};`;
    case 'decision': return `${base}rhombus;align=center;verticalAlign=middle;fontStyle=1;fillColor=${node.fill || COLORS.goldLight};strokeColor=${node.stroke || COLORS.gold};`;
    case 'start': return `${base}ellipse;aspect=fixed;fillColor=${COLORS.ink};strokeColor=${COLORS.ink};`;
    case 'end': return `${base}shape=doubleEllipse;aspect=fixed;fillColor=${COLORS.ink};strokeColor=${COLORS.ink};`;
    case 'note': return `${base}shape=note;align=left;verticalAlign=top;spacing=6;fillColor=${node.fill || COLORS.goldLight};strokeColor=${node.stroke || COLORS.gold};`;
    case 'database': return `${base}shape=cylinder3;boundedLbl=1;backgroundOutline=1;align=center;verticalAlign=middle;fontStyle=1;fillColor=${node.fill || COLORS.blue};strokeColor=${node.stroke || COLORS.navy};`;
    case 'external': return `${base}rounded=1;dashed=1;align=center;verticalAlign=middle;fontStyle=1;fillColor=${node.fill || COLORS.grayLight};strokeColor=${node.stroke || COLORS.gray};`;
    default: return `${base}rounded=1;align=center;verticalAlign=middle;fontStyle=1;fillColor=${node.fill || COLORS.blue};strokeColor=${node.stroke || COLORS.navy};`;
  }
}

function drawioValue(node) {
  if (node.type === 'class' || node.type === 'entity') {
    const stereotype = node.stereotype ? `<div style="font-size:10px;color:#667085">&laquo;${node.stereotype}&raquo;</div>` : '';
    const body = node.lines.length ? `<div style="text-align:left;font-size:11px;padding:4px">${node.lines.join('<br>')}</div>` : '';
    return `${stereotype}<b>${node.title}</b>${body}`;
  }
  if (node.type === 'note') return node.lines.join('<br>');
  return node.title.replaceAll('\n', '<br>');
}

function renderDrawioDiagram(diagram) {
  const vertices = diagram.nodes.map((node) =>
    `<mxCell id="${xml(node.id)}" value="${xml(drawioValue(node))}" style="${xml(drawioStyle(node))}" vertex="1" parent="1"><mxGeometry x="${node.x}" y="${node.y}" width="${node.w}" height="${node.h}" as="geometry"/></mxCell>`
  ).join('');
  const edges = diagram.edges.map((edge) => {
    const style = `edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;${edge.dashed ? 'dashed=1;' : ''}${edge.arrow ? 'endArrow=block;endFill=1;' : edge.openArrow ? 'endArrow=open;endFill=0;' : 'endArrow=none;'}strokeColor=${edge.color || COLORS.line};`;
    const points = edge.points && edge.points.length > 2
      ? `<Array as="points">${edge.points.slice(1, -1).map(([x, y]) => `<mxPoint x="${x}" y="${y}"/>`).join('')}</Array>`
      : '';
    const labels = [
      edge.startLabel ? `<mxCell id="${edge.id}-start" value="${xml(edge.startLabel)}" style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontStyle=1;" vertex="1" connectable="0" parent="${edge.id}"><mxGeometry x="-0.82" relative="1" as="geometry"><mxPoint y="-10" as="offset"/></mxGeometry></mxCell>` : '',
      edge.endLabel ? `<mxCell id="${edge.id}-end" value="${xml(edge.endLabel)}" style="edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];fontStyle=1;" vertex="1" connectable="0" parent="${edge.id}"><mxGeometry x="0.82" relative="1" as="geometry"><mxPoint y="-10" as="offset"/></mxGeometry></mxCell>` : ''
    ].join('');
    return `<mxCell id="${edge.id}" value="${xml(edge.label || '')}" style="${xml(style)}" edge="1" parent="1" source="${xml(edge.source)}" target="${xml(edge.target)}"><mxGeometry relative="1" as="geometry">${points}</mxGeometry></mxCell>${labels}`;
  }).join('');
  const captionId = `caption-${diagram.figure}`;
  const captions = `<mxCell id="${captionId}" value="${xml(`Figura ${diagram.figure} – ${diagram.caption}\nFonte: Elaborado pelos autores (2026).`)}" style="text;html=1;align=center;verticalAlign=middle;fontStyle=1;whiteSpace=wrap;" vertex="1" parent="1"><mxGeometry x="260" y="742" width="603" height="42" as="geometry"/></mxCell>`;
  return `<diagram id="fig-${diagram.figure}" name="Figura ${diagram.figure}"><mxGraphModel dx="1422" dy="794" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1123" pageHeight="794" background="#FFFFFF"><root><mxCell id="0"/><mxCell id="1" parent="0"/>${vertices}${edges}${captions}</root></mxGraphModel></diagram>`;
}

function renderDrawio(diagrams) {
  return `<?xml version="1.0" encoding="UTF-8"?><mxfile host="app.diagrams.net" modified="2026-10-22T00:00:00.000Z" agent="CourtTime" version="24.7.17" type="device" compressed="false">${diagrams.map(renderDrawioDiagram).join('')}</mxfile>`;
}

function useCaseGeneral() {
  const nodes = [
    n('boundary', 'boundary', 205, 80, 710, 620, 'CourtTime'),
    n('aluno', 'actor', 35, 120, 110, 100, 'Aluno'),
    n('secretaria', 'actor', 35, 300, 110, 100, 'Secretaria'),
    n('admin', 'actor', 960, 150, 120, 100, 'Administrador'),
    n('seguranca', 'actor', 960, 390, 120, 100, 'Segurança'),
    n('uc-calendario', 'usecase', 270, 110, 220, 60, 'Consultar calendário e disponibilidade'),
    n('uc-solicitar', 'usecase', 270, 205, 220, 60, 'Criar e acompanhar solicitação'),
    n('uc-participantes', 'usecase', 270, 300, 220, 60, 'Manter participantes e documentação'),
    n('uc-conferir', 'usecase', 270, 455, 220, 60, 'Conferir documento e assinatura gov.br'),
    n('uc-contribuicao', 'usecase', 270, 560, 220, 60, 'Registrar contribuição presencial'),
    n('uc-analisar', 'usecase', 630, 145, 220, 60, 'Analisar solicitação'),
    n('uc-agenda', 'usecase', 630, 250, 220, 60, 'Administrar agenda e usuários'),
    n('uc-cancelar', 'usecase', 630, 355, 220, 60, 'Cancelar reserva institucionalmente'),
    n('uc-portaria', 'usecase', 630, 485, 220, 60, 'Controlar presença e liberar quadra'),
    n('uc-repasse', 'usecase', 630, 590, 220, 60, 'Confirmar repasse da portaria')
  ];
  const edges = [
    e('g1', 'aluno', 'uc-calendario', [pt(145, 170), pt(270, 140)]),
    e('g2', 'aluno', 'uc-solicitar', [pt(145, 170), pt(210, 170), pt(210, 235), pt(270, 235)]),
    e('g3', 'aluno', 'uc-participantes', [pt(145, 170), pt(190, 170), pt(190, 330), pt(270, 330)]),
    e('g4', 'secretaria', 'uc-conferir', [pt(145, 350), pt(210, 350), pt(210, 485), pt(270, 485)]),
    e('g5', 'secretaria', 'uc-contribuicao', [pt(145, 350), pt(185, 350), pt(185, 590), pt(270, 590)]),
    e('g6', 'secretaria', 'uc-repasse', [pt(145, 350), pt(175, 350), pt(175, 660), pt(740, 660), pt(740, 620)]),
    e('g7', 'admin', 'uc-analisar', [pt(960, 200), pt(850, 175)]),
    e('g8', 'admin', 'uc-agenda', [pt(960, 200), pt(900, 200), pt(900, 280), pt(850, 280)]),
    e('g9', 'admin', 'uc-cancelar', [pt(960, 200), pt(920, 200), pt(920, 385), pt(850, 385)]),
    e('g10', 'seguranca', 'uc-portaria', [pt(960, 440), pt(850, 515)])
  ];
  return { figure: 1, file: '01-casos-de-uso-gerais', title: 'Casos de uso do CourtTime — visão geral', subtitle: 'Responsabilidades dos quatro perfis dentro da fronteira do sistema', caption: 'Casos de uso gerais do CourtTime', nodes, edges };
}

function useCasePortaria() {
  const nodes = [
    n('boundary', 'boundary', 180, 80, 760, 620, 'CourtTime — Portaria e Secretaria'),
    n('seguranca', 'actor', 30, 260, 110, 100, 'Segurança'),
    n('secretaria', 'actor', 970, 285, 110, 100, 'Secretaria'),
    n('consultar', 'usecase', 235, 115, 220, 58, 'Consultar reserva aprovada'),
    n('presencas', 'usecase', 235, 215, 220, 58, 'Registrar presenças e ausências'),
    n('adicional', 'usecase', 235, 315, 220, 58, 'Cadastrar pessoa adicional'),
    n('identificar', 'usecase', 245, 430, 200, 55, 'Validar identificação, categoria e vínculo', [], { fill: COLORS.grayLight, stroke: COLORS.gray, wrap: 23 }),
    n('pag-portaria', 'usecase', 235, 545, 220, 58, 'Registrar pagamento adicional na portaria'),
    n('liberar', 'usecase', 635, 215, 220, 58, 'Liberar quadra'),
    n('minimo', 'usecase', 645, 325, 200, 55, 'Verificar mínimo de 10 presentes', [], { fill: COLORS.grayLight, stroke: COLORS.gray }),
    n('pendencia', 'usecase', 645, 420, 200, 55, 'Verificar ausência de valor pendente', [], { fill: COLORS.grayLight, stroke: COLORS.gray }),
    n('pag-secretaria', 'usecase', 635, 525, 220, 58, 'Registrar pagamento adicional na Secretaria'),
    n('repasse', 'usecase', 635, 615, 220, 58, 'Confirmar repasse recebido da portaria')
  ];
  const edges = [
    e('p1', 'seguranca', 'consultar', [pt(140, 310), pt(190, 310), pt(190, 144), pt(235, 144)]),
    e('p2', 'seguranca', 'presencas', [pt(140, 310), pt(235, 244)]),
    e('p3', 'seguranca', 'adicional', [pt(140, 310), pt(235, 344)]),
    e('p4', 'seguranca', 'pag-portaria', [pt(140, 310), pt(180, 310), pt(180, 574), pt(235, 574)]),
    e('p5', 'seguranca', 'liberar', [pt(140, 310), pt(510, 310), pt(510, 244), pt(635, 244)]),
    e('p6', 'adicional', 'identificar', [pt(345, 373), pt(345, 430)], { dashed: true, openArrow: true, label: '«include»', labelPos: pt(395, 405), labelWidth: 70 }),
    e('p7', 'liberar', 'minimo', [pt(745, 273), pt(745, 325)], { dashed: true, openArrow: true, label: '«include»', labelPos: pt(795, 300), labelWidth: 70 }),
    e('p8', 'liberar', 'pendencia', [pt(855, 244), pt(895, 244), pt(895, 447), pt(845, 447)], { dashed: true, openArrow: true, label: '«include»', labelPos: pt(895, 345), labelWidth: 70 }),
    e('p9', 'secretaria', 'pag-secretaria', [pt(970, 335), pt(930, 335), pt(930, 554), pt(855, 554)]),
    e('p10', 'secretaria', 'repasse', [pt(970, 335), pt(950, 335), pt(950, 644), pt(855, 644)])
  ];
  return { figure: 2, file: '02-casos-de-uso-portaria-secretaria', title: 'Casos de uso — portaria e Secretaria', subtitle: 'Detalhamento das ações do dia da reserva e da confirmação posterior de repasse', caption: 'Casos de uso da portaria e da Secretaria', nodes, edges };
}

function classesCore() {
  const nodes = [
    n('usuario', 'class', 35, 100, 210, 150, 'Usuario', ['- id: Long', '- nome: String', '- email: String', '- perfil: Perfil', '- ativo: boolean', '- versaoAcesso: long']),
    n('quadra', 'class', 35, 395, 210, 120, 'Quadra', ['- id: Long', '- nome: String', '- descricao: String', '- ativa: boolean']),
    n('reserva', 'class', 400, 105, 320, 285, 'Reserva', ['- id: Long', '- dataUtilizacao: LocalDate', '- horarioInicio: LocalTime', '- horarioFim: LocalTime', '- finalidade: String', '- status: StatusReserva', '- rascunho: boolean', '- revisaoParticipantes: int', '- criadaEm / atualizadaEm: Instant', '- analisadaEm / canceladaEm: Instant [0..1]', '- liberadaEm: Instant [0..1]', '+ isIntervaloValido(): boolean', '+ isLiberada(): boolean', '+ registrarLiberacao(usuario, instante): void'], { stereotype: 'entity', fontSize: 10.5, lineHeight: 14 }),
    n('participante', 'class', 825, 90, 260, 230, 'ParticipanteReserva', ['- id: Long', '- nomeCompleto: String', '- rmOuRg: String', '- categoria: CategoriaParticipante', '- vinculo: VinculoParticipante [0..1]', '- presenca: PresencaParticipante [0..1]', '- adicionadoNaPortaria: Boolean [0..1]', '+ registrarPresenca(...): void', '+ marcarAdicionadoNaPortaria(...): void'], { stereotype: 'entity', fontSize: 10.3, lineHeight: 14 }),
    n('disponibilidade', 'class', 360, 500, 260, 155, 'DisponibilidadeQuadra', ['- id: Long', '- diaSemana: DiaSemana', '- horarioInicio: LocalTime', '- horarioFim: LocalTime', '- ativa: boolean', '+ isIntervaloValido(): boolean'], { stereotype: 'entity' }),
    n('bloqueio', 'class', 760, 500, 260, 155, 'BloqueioQuadra', ['- id: Long', '- data: LocalDate', '- horarioInicio: LocalTime', '- horarioFim: LocalTime', '- motivo: String', '- ativo: boolean', '+ isIntervaloValido(): boolean'], { stereotype: 'entity' }),
    n('note', 'note', 35, 565, 255, 120, '', ['Associações são simples:', 'o código não declara cascade', 'nem orphanRemoval. Por isso', 'não se usa composição UML.'], { fontSize: 11 })
  ];
  const edges = [
    e('c1', 'usuario', 'reserva', [pt(245, 175), pt(400, 175)], { label: 'solicitante', labelPos: pt(322, 160), labelWidth: 70, startLabel: '1', endLabel: '0..*', startLabelPos: pt(260, 168), endLabelPos: pt(383, 168) }),
    e('c2', 'quadra', 'reserva', [pt(245, 455), pt(320, 455), pt(320, 330), pt(400, 330)], { label: 'agenda', labelPos: pt(320, 410), startLabel: '1', endLabel: '0..*', startLabelPos: pt(260, 447), endLabelPos: pt(382, 322) }),
    e('c3', 'reserva', 'participante', [pt(720, 235), pt(825, 235)], { label: 'participantes', labelPos: pt(772, 217), labelWidth: 78, startLabel: '1', endLabel: '0..*', startLabelPos: pt(737, 228), endLabelPos: pt(808, 228) }),
    e('c4', 'quadra', 'disponibilidade', [pt(245, 455), pt(295, 455), pt(295, 577), pt(360, 577)], { label: 'horários', labelPos: pt(295, 535), startLabel: '1', endLabel: '0..*', startLabelPos: pt(260, 447), endLabelPos: pt(345, 570) }),
    e('c5', 'quadra', 'bloqueio', [pt(140, 515), pt(140, 700), pt(890, 700), pt(890, 655)], { label: 'bloqueios', labelPos: pt(525, 694), labelWidth: 70, startLabel: '1', endLabel: '0..*', startLabelPos: pt(155, 530), endLabelPos: pt(900, 672) }),
    e('c6', 'reserva', 'reserva', [pt(560, 390), pt(560, 455), pt(735, 455), pt(735, 300), pt(720, 300)], { label: 'remarcação de', labelPos: pt(650, 447), labelWidth: 92, startLabel: '0..1', endLabel: '0..1', startLabelPos: pt(575, 405), endLabelPos: pt(735, 285) })
  ];
  return { figure: 3, file: '03-classes-nucleo-reservas', title: 'Diagrama de classes — núcleo de reservas', subtitle: 'Entidades centrais, agenda e multiplicidades derivadas do mapeamento JPA', caption: 'Diagrama de classes do núcleo de reservas', nodes, edges };
}

function classesDocumentation() {
  const nodes = [
    n('reserva', 'class', 20, 95, 205, 135, 'Reserva', ['- status: StatusReserva', '- rascunho: boolean', '- revisaoParticipantes: int', '- liberadaEm: Instant [0..1]', '+ isLiberada(): boolean'], { stereotype: 'entity' }),
    n('participante', 'class', 20, 360, 235, 175, 'ParticipanteReserva', ['- categoria: CategoriaParticipante', '- vinculo: VinculoParticipante [0..1]', '- presenca: PresencaParticipante [0..1]', '- adicionadoNaPortaria: Boolean [0..1]', '- adicionadoEm: Instant [0..1]'], { stereotype: 'entity', fontSize: 10.2 }),
    n('documento', 'class', 300, 85, 235, 185, 'DocumentoReserva', ['- versao: int', '- revisaoParticipantes: int', '- chaveArmazenamento: String', '- sha256: String', '- tamanhoBytes: long', '- enviadoEm: Instant'], { stereotype: 'entity' }),
    n('conferencia', 'class', 590, 85, 235, 185, 'ConferenciaDocumento', ['- resultado: ResultadoConferenciaDocumento', '- justificativa: String [0..1]', '- assinaturaGovBrConferida: Boolean [0..1]', '- conferidoEm: Instant'], { stereotype: 'entity', fontSize: 9.8 }),
    n('contribuicao', 'class', 875, 85, 225, 205, 'ContribuicaoApm', ['- valor: BigDecimal', '- valorDevido: BigDecimal [0..1]', '- creditoAplicado: BigDecimal [0..1]', '- observacoes: String [0..1]', '- registradaEm: Instant', '+ getValorEfetivamentePago(): BigDecimal'], { stereotype: 'entity', fontSize: 9.8 }),
    n('pagamento', 'class', 300, 425, 235, 185, 'PagamentoAdicional', ['- quantidadePessoas: int', '- valor: BigDecimal', '- local: LocalPagamento', '- registradoEm: Instant', '- repasseConfirmadoEm: Instant [0..1]', '+ aguardaRepasse(): boolean'], { stereotype: 'entity', fontSize: 9.8 }),
    n('ajuste', 'class', 875, 440, 225, 185, 'AjusteContribuicaoApm', ['- valorAnterior: BigDecimal', '- valorNovo: BigDecimal', '- motivo: String', '- observacoesAnteriores: String [0..1]', '- observacoesNovas: String [0..1]', '- ajustadaEm: Instant'], { stereotype: 'entity', fontSize: 9.5 }),
    n('operadores', 'note', 585, 385, 235, 250, '', ['Os eventos preservam o operador:', '', 'Documento: enviadoPor', 'Conferência: conferidoPor', 'Contribuição: registradaPor', 'Ajuste: ajustadaPor', 'Participante: presença/adicionadoPor', 'Pagamento: registradoPor e', 'repasseConfirmadoPor.', '', 'Todos referenciam Usuario.'], { fontSize: 10.3 })
  ];
  const edges = [
    e('d1', 'reserva', 'documento', [pt(225, 160), pt(300, 160)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(240, 153), endLabelPos: pt(283, 153) }),
    e('d2', 'documento', 'conferencia', [pt(535, 170), pt(590, 170)], { startLabel: '1', endLabel: '0..1', startLabelPos: pt(550, 163), endLabelPos: pt(574, 163) }),
    e('d3', 'conferencia', 'contribuicao', [pt(825, 170), pt(875, 170)], { startLabel: '1', endLabel: '0..1', startLabelPos: pt(838, 163), endLabelPos: pt(860, 163) }),
    e('d4', 'reserva', 'contribuicao', [pt(122, 230), pt(122, 320), pt(985, 320), pt(985, 290)], { label: 'contribuição', labelPos: pt(675, 314), labelWidth: 75, startLabel: '1', endLabel: '0..1', startLabelPos: pt(135, 246), endLabelPos: pt(998, 306) }),
    e('d5', 'reserva', 'participante', [pt(122, 230), pt(122, 360)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(135, 246), endLabelPos: pt(137, 350) }),
    e('d6', 'reserva', 'pagamento', [pt(225, 195), pt(270, 195), pt(270, 518), pt(300, 518)], { label: 'adicionais', labelPos: pt(270, 350), labelWidth: 68, startLabel: '1', endLabel: '0..*', startLabelPos: pt(240, 188), endLabelPos: pt(284, 510) }),
    e('d7', 'contribuicao', 'ajuste', [pt(987, 290), pt(987, 440)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(1000, 307), endLabelPos: pt(1000, 430) }),
    e('d8', 'contribuicao', 'contribuicao', [pt(1100, 130), pt(1112, 130), pt(1112, 350), pt(850, 350), pt(850, 240), pt(875, 240)], { label: 'origem do crédito', labelPos: pt(985, 345), labelWidth: 95, startLabel: '0..*', endLabel: '0..1', startLabelPos: pt(1090, 145), endLabelPos: pt(860, 232) })
  ];
  return { figure: 4, file: '04-classes-documentacao-contribuicao-portaria', title: 'Diagrama de classes — documentação, contribuição e portaria', subtitle: 'Histórico documental e financeiro ligado à reserva; operadores preservados por referência', caption: 'Diagrama de classes de documentação, contribuição e portaria', nodes, edges };
}

function erd() {
  const entity = (id, x, y, w, h, title, lines, extra = {}) => n(id, 'entity', x, y, w, h, title, lines, extra);
  const nodes = [
    entity('usuarios', 20, 82, 205, 160, 'usuarios', ['PK id: bigint', 'UQ NN email: varchar(254)', 'NN nome: varchar(150)', 'NN senha: varchar(100)', 'NN perfil: varchar(20)', 'NN ativo: boolean', 'NN versao_acesso: bigint'], { fontSize: 9.4, lineHeight: 12.2 }),
    entity('quadras', 270, 82, 185, 118, 'quadras', ['PK id: bigint', 'NN nome: varchar(100)', 'NN descricao: varchar(500)', 'NN ativa: boolean'], { fontSize: 9.5 }),
    entity('disponibilidades', 500, 82, 250, 135, 'disponibilidades_quadra', ['PK id: bigint', 'FK NN quadra_id → quadras.id', 'NN dia_semana: varchar(10)', 'NN horario_inicio / horario_fim: time', 'NN ativa: boolean'], { fontSize: 9.2 }),
    entity('bloqueios', 800, 82, 250, 135, 'bloqueios_quadra', ['PK id: bigint', 'FK NN quadra_id → quadras.id', 'NN data: date', 'NN horario_inicio / horario_fim: time', 'NN motivo: varchar(200)', 'NN ativo: boolean'], { fontSize: 9.2 }),
    entity('reservas', 250, 280, 300, 245, 'reservas', ['PK id: bigint', 'FK NN quadra_id → quadras.id', 'FK NN solicitante_id → usuarios.id', 'FK analisada_por_id → usuarios.id', 'FK cancelada_por_id → usuarios.id', 'FK liberada_por_id → usuarios.id', 'UQ FK remarcacao_de_id → reservas.id', 'NN data_utilizacao: date', 'NN horario_inicio / horario_fim: time', 'NN finalidade: varchar(300)', 'NN status: varchar(30)', 'NN rascunho: boolean DEFAULT false', 'NN revisao_participantes: integer', 'criada_em / atualizada_em: timestamp', 'analisada_em / cancelada_em / liberada_em'], { fontSize: 8.8, lineHeight: 12.2 }),
    entity('participantes', 20, 305, 200, 205, 'participantes_reserva', ['PK id: bigint', 'FK NN reserva_id → reservas.id', 'NN nome_completo: varchar(150)', 'NN rm_ou_rg: varchar(30)', 'NN categoria: varchar(10)', 'vinculo: varchar(20)', 'presenca: varchar(10)', 'FK presenca_registrada_por_id', 'adicionado_na_portaria: boolean', 'FK adicionado_por_id', 'presenca_registrada_em / adicionado_em'], { fontSize: 8.5, lineHeight: 11.5 }),
    entity('documentos', 600, 265, 230, 190, 'documentos_reserva', ['PK id: bigint', 'FK NN reserva_id → reservas.id', 'UQ (reserva_id, versao)', 'NN versao / revisao_participantes: integer', 'UQ NN chave_armazenamento: varchar(80)', 'NN nome_original: varchar(255)', 'NN sha256: char(64)', 'NN tamanho_bytes: bigint', 'FK NN enviado_por_id → usuarios.id', 'NN enviado_em: timestamp'], { fontSize: 8.7, lineHeight: 11.7 }),
    entity('conferencias', 870, 265, 230, 170, 'conferencias_documento', ['PK id: bigint', 'UQ FK NN documento_id', 'NN resultado: varchar(30)', 'justificativa: varchar(500)', 'assinatura_gov_br_conferida: boolean', 'FK NN conferido_por_id → usuarios.id', 'NN conferido_em: timestamp'], { fontSize: 8.8, lineHeight: 12 }),
    entity('pagamentos', 20, 555, 250, 160, 'pagamentos_adicionais', ['PK id: bigint', 'FK NN reserva_id → reservas.id', 'NN quantidade_pessoas: integer', 'NN valor: numeric(12,2)', 'NN local: varchar(12)', 'FK NN registrado_por_id → usuarios.id', 'FK repasse_confirmado_por_id → usuarios.id', 'NN registrado_em: timestamp', 'repasse_confirmado_em: timestamp'], { fontSize: 8.5, lineHeight: 11.7 }),
    entity('contribuicoes', 330, 555, 270, 170, 'contribuicoes_apm', ['PK id: bigint', 'UQ FK NN reserva_id → reservas.id', 'UQ FK NN conferencia_documento_id', 'NN valor: numeric(12,2)', 'valor_devido / credito_aplicado: numeric(12,2)', 'FK contribuicao_origem_id → contribuicoes_apm.id', 'FK NN registrada_por_id → usuarios.id', 'observacoes: varchar(500)', 'NN registrada_em: timestamp'], { fontSize: 8.4, lineHeight: 11.6 }),
    entity('ajustes', 650, 535, 225, 190, 'ajustes_contribuicao_apm', ['PK id: bigint', 'FK NN contribuicao_id', 'NN valor_anterior / valor_novo: numeric(12,2)', 'observacoes_anteriores / novas: varchar(500)', 'NN motivo: varchar(500)', 'FK NN ajustada_por_id → usuarios.id', 'NN ajustada_em: timestamp'], { fontSize: 8.2, lineHeight: 11.4 }),
    entity('auditorias', 910, 500, 190, 225, 'auditorias_usuario', ['PK id: bigint', 'FK NN operador_id', 'FK NN usuario_afetado_id', 'NN operacao: varchar(30)', 'perfil_anterior / novo: varchar(20)', 'ativo_anterior / novo: boolean', 'NN realizada_em: timestamp'], { fontSize: 8.5, lineHeight: 11.7 })
  ];
  const edges = [
    e('r1', 'quadras', 'reservas', [pt(362, 200), pt(362, 280)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(376, 216), endLabelPos: pt(376, 270) }),
    e('r2', 'quadras', 'disponibilidades', [pt(455, 135), pt(500, 135)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(467, 127), endLabelPos: pt(487, 127) }),
    e('r3', 'quadras', 'bloqueios', [pt(362, 82), pt(362, 68), pt(925, 68), pt(925, 82)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(375, 78), endLabelPos: pt(938, 78) }),
    e('r4', 'usuarios', 'reservas', [pt(122, 242), pt(122, 265), pt(300, 265), pt(300, 280)], { label: 'solicitante e papéis', labelPos: pt(210, 259), labelWidth: 110, startLabel: '1', endLabel: '0..*', startLabelPos: pt(136, 254), endLabelPos: pt(314, 275) }),
    e('r5', 'reservas', 'participantes', [pt(250, 400), pt(220, 400)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(238, 392), endLabelPos: pt(232, 415) }),
    e('r6', 'reservas', 'documentos', [pt(550, 350), pt(600, 350)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(564, 342), endLabelPos: pt(585, 342) }),
    e('r7', 'documentos', 'conferencias', [pt(830, 350), pt(870, 350)], { startLabel: '1', endLabel: '0..1', startLabelPos: pt(842, 342), endLabelPos: pt(857, 342) }),
    e('r8', 'reservas', 'pagamentos', [pt(300, 525), pt(300, 640), pt(270, 640)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(313, 541), endLabelPos: pt(286, 632) }),
    e('r9', 'reservas', 'contribuicoes', [pt(455, 525), pt(455, 555)], { startLabel: '1', endLabel: '0..1', startLabelPos: pt(468, 541), endLabelPos: pt(468, 548) }),
    e('r10', 'conferencias', 'contribuicoes', [pt(985, 435), pt(985, 470), pt(560, 470), pt(560, 555)], { label: 'base da contribuição', labelPos: pt(760, 463), labelWidth: 112, startLabel: '1', endLabel: '0..1', startLabelPos: pt(997, 450), endLabelPos: pt(573, 548) }),
    e('r11', 'contribuicoes', 'ajustes', [pt(600, 640), pt(650, 640)], { startLabel: '1', endLabel: '0..*', startLabelPos: pt(612, 632), endLabelPos: pt(637, 632) }),
    e('r12', 'usuarios', 'auditorias', [pt(20, 160), pt(8, 160), pt(8, 730), pt(1005, 730), pt(1005, 725)], { label: 'operador / afetado', labelPos: pt(500, 724), labelWidth: 105, startLabel: '1', endLabel: '0..*', startLabelPos: pt(15, 174), endLabelPos: pt(1018, 716) }),
    e('r13', 'reservas', 'reservas', [pt(550, 430), pt(575, 430), pt(575, 545), pt(235, 545), pt(235, 455), pt(250, 455)], { label: 'remarcacao_de (UQ)', labelPos: pt(405, 539), labelWidth: 110, startLabel: '0..1', endLabel: '0..1', startLabelPos: pt(560, 445), endLabelPos: pt(238, 470) })
  ];
  return { figure: 5, file: '05-der-postgresql', title: 'DER lógico do banco PostgreSQL', subtitle: 'Derivado das 12 entidades JPA da base funcional 4fb7a06; PK, FK, UQ e NN indicam restrições persistidas', caption: 'Diagrama entidade-relacionamento lógico do CourtTime', nodes, edges };
}

function activityComplete() {
  const nodes = [
    n('lane-aluno', 'lane', 20, 85, 1080, 120, 'Aluno', [], { header: COLORS.blue }),
    n('lane-sistema', 'lane', 20, 205, 1080, 135, 'CourtTime', [], { header: COLORS.tealLight }),
    n('lane-secretaria', 'lane', 20, 340, 1080, 120, 'Secretaria', [], { header: COLORS.goldLight }),
    n('lane-admin', 'lane', 20, 460, 1080, 115, 'Administração', [], { header: COLORS.redLight }),
    n('lane-seguranca', 'lane', 20, 575, 1080, 135, 'Segurança', [], { header: COLORS.greenLight }),
    n('start', 'start', 112, 130, 18, 18, ''),
    n('criar', 'action', 155, 112, 125, 55, 'Selecionar período e salvar rascunho'),
    n('validar', 'action', 315, 245, 135, 55, 'Validar antecedência, grade e disponibilidade'),
    n('participantes', 'action', 480, 112, 140, 55, 'Cadastrar 10 pessoas e formalizar pedido'),
    n('formulario', 'action', 650, 112, 130, 55, 'Gerar, assinar no gov.br e enviar PDF'),
    n('armazenar', 'action', 815, 245, 125, 55, 'Versionar PDF e travar lista'),
    n('conferir', 'action', 155, 372, 135, 55, 'Conferir PDF e assinatura no ITI'),
    n('ok-doc', 'decision', 330, 365, 95, 70, 'Documento correto?'),
    n('contribuir', 'action', 470, 372, 150, 55, 'Registrar R$ 2,00 × participantes'),
    n('pendente', 'action', 665, 245, 120, 55, 'Mudar para PENDENTE_ANALISE', [], { fill: COLORS.tealLight }),
    n('analisar', 'action', 680, 490, 135, 55, 'Revalidar elegibilidade e conflito'),
    n('aprovar', 'decision', 850, 482, 95, 70, 'Aprovar?'),
    n('registrar-presenca', 'action', 155, 615, 145, 55, 'Registrar presença e pessoas adicionais'),
    n('liberar', 'action', 355, 615, 150, 55, 'Liberar com 10 presentes e sem pendência'),
    n('end', 'end', 560, 631, 24, 24, ''),
    n('rejeitada', 'end', 1015, 505, 24, 24, ''),
    n('nota', 'note', 650, 600, 390, 85, '', ['Após a liberação, participantes e presenças ficam imutáveis.', 'A assinatura e o uso do ITI são ações humanas externas.'], { fontSize: 10.5 })
  ];
  const edges = [
    e('a1', 'start', 'criar', [pt(130, 139), pt(155, 139)], { arrow: true }),
    e('a2', 'criar', 'validar', [pt(280, 139), pt(300, 139), pt(300, 272), pt(315, 272)], { arrow: true }),
    e('a3', 'validar', 'participantes', [pt(450, 272), pt(465, 272), pt(465, 139), pt(480, 139)], { arrow: true }),
    e('a4', 'participantes', 'formulario', [pt(620, 139), pt(650, 139)], { arrow: true }),
    e('a5', 'formulario', 'armazenar', [pt(780, 139), pt(800, 139), pt(800, 272), pt(815, 272)], { arrow: true }),
    e('a6', 'armazenar', 'conferir', [pt(877, 300), pt(877, 325), pt(222, 325), pt(222, 372)], { arrow: true }),
    e('a7', 'conferir', 'ok-doc', [pt(290, 399), pt(330, 399)], { arrow: true }),
    e('a8', 'ok-doc', 'contribuir', [pt(425, 399), pt(470, 399)], { arrow: true, label: 'sim', labelPos: pt(447, 389), labelWidth: 26 }),
    e('a9', 'ok-doc', 'formulario', [pt(377, 365), pt(377, 185), pt(715, 185), pt(715, 167)], { arrow: true, label: 'não / corrigir no prazo', labelPos: pt(520, 179), labelWidth: 110 }),
    e('a10', 'contribuir', 'pendente', [pt(620, 399), pt(640, 399), pt(640, 272), pt(665, 272)], { arrow: true }),
    e('a11', 'pendente', 'analisar', [pt(725, 300), pt(725, 490)], { arrow: true }),
    e('a12', 'analisar', 'aprovar', [pt(815, 517), pt(850, 517)], { arrow: true }),
    e('a13', 'aprovar', 'registrar-presenca', [pt(897, 552), pt(897, 590), pt(227, 590), pt(227, 615)], { arrow: true, label: 'sim / no dia', labelPos: pt(550, 584), labelWidth: 70 }),
    e('a14', 'aprovar', 'rejeitada', [pt(945, 517), pt(1015, 517)], { arrow: true, label: 'não', labelPos: pt(980, 507), labelWidth: 28 }),
    e('a15', 'registrar-presenca', 'liberar', [pt(300, 642), pt(355, 642)], { arrow: true }),
    e('a16', 'liberar', 'end', [pt(505, 642), pt(560, 642)], { arrow: true })
  ];
  return { figure: 6, file: '06-atividade-reserva-completa', title: 'Atividades — ciclo completo da reserva', subtitle: 'Da solicitação preliminar à liberação da quadra, com responsabilidades por perfil', caption: 'Diagrama de atividades da reserva completa', nodes, edges };
}

function activityReschedule() {
  const nodes = [
    n('lane-admin', 'lane', 20, 90, 1080, 150, 'Administração', [], { header: COLORS.redLight }),
    n('lane-system', 'lane', 20, 240, 1080, 175, 'CourtTime', [], { header: COLORS.tealLight }),
    n('lane-aluno', 'lane', 20, 415, 1080, 135, 'Aluno', [], { header: COLORS.blue }),
    n('lane-secretaria', 'lane', 20, 550, 1080, 160, 'Secretaria', [], { header: COLORS.goldLight }),
    n('start', 'start', 115, 145, 18, 18, ''),
    n('cancelar', 'action', 160, 125, 145, 58, 'Cancelar reserva aprovada com motivo'),
    n('comunicar', 'action', 355, 125, 155, 58, 'Avisar o aluno manualmente pelo WhatsApp'),
    n('registrar-cancelamento', 'action', 160, 285, 160, 58, 'Registrar CANCELADA e preservar histórico'),
    n('credito', 'action', 370, 285, 155, 58, 'Disponibilizar valor efetivamente pago como crédito'),
    n('solicitar', 'action', 575, 450, 145, 58, 'Solicitar remarcação e escolher novo período'),
    n('validar-origem', 'action', 735, 285, 135, 58, 'Travar origem e validar sucessora única', [], { wrap: 21, fontSize: 10.8 }),
    n('copiar', 'decision', 750, 445, 110, 75, 'Copiar participantes?'),
    n('nova', 'action', 900, 285, 170, 58, 'Criar rascunho vinculado; formalizar com 10 pessoas'),
    n('documentar', 'action', 900, 450, 170, 58, 'Refazer documento e aprovação'),
    n('calcular', 'action', 575, 595, 175, 60, 'Calcular devido, crédito, diferença e excedente'),
    n('registrar', 'action', 800, 595, 160, 60, 'Registrar uso do crédito e eventual diferença'),
    n('end', 'end', 1010, 613, 24, 24, ''),
    n('nota', 'note', 120, 585, 370, 85, '', ['Não há devolução automática. O saldo excedente apenas é exibido,', 'pois seu destino institucional permanece sem decisão.'], { fontSize: 10.5 })
  ];
  const edges = [
    e('m1', 'start', 'cancelar', [pt(133, 154), pt(160, 154)], { arrow: true }),
    e('m2', 'cancelar', 'registrar-cancelamento', [pt(232, 183), pt(232, 285)], { arrow: true }),
    e('m3', 'registrar-cancelamento', 'credito', [pt(320, 314), pt(370, 314)], { arrow: true }),
    e('m4', 'credito', 'comunicar', [pt(447, 285), pt(447, 183)], { arrow: true }),
    e('m5', 'comunicar', 'solicitar', [pt(510, 154), pt(550, 154), pt(550, 479), pt(575, 479)], { arrow: true }),
    e('m6', 'solicitar', 'validar-origem', [pt(647, 450), pt(647, 314), pt(735, 314)], { arrow: true }),
    e('m7', 'validar-origem', 'copiar', [pt(802, 343), pt(802, 445)], { arrow: true }),
    e('m8', 'copiar', 'nova', [pt(860, 482), pt(880, 482), pt(880, 314), pt(900, 314)], { arrow: true, label: 'sim ou não', labelPos: pt(880, 405), labelWidth: 62 }),
    e('m9', 'nova', 'documentar', [pt(985, 343), pt(985, 450)], { arrow: true }),
    e('m10', 'documentar', 'calcular', [pt(985, 508), pt(985, 535), pt(662, 535), pt(662, 595)], { arrow: true }),
    e('m11', 'calcular', 'registrar', [pt(750, 625), pt(800, 625)], { arrow: true }),
    e('m12', 'registrar', 'end', [pt(960, 625), pt(1010, 625)], { arrow: true })
  ];
  return { figure: 7, file: '07-atividade-cancelamento-remarcacao', title: 'Atividades — cancelamento institucional e remarcação', subtitle: 'Nova solicitação conserva o vínculo e o crédito, mas repete documentação e aprovação', caption: 'Diagrama de atividades do cancelamento e da remarcação', nodes, edges };
}

function activityRelease() {
  const nodes = [
    n('lane-security', 'lane', 20, 90, 1080, 210, 'Segurança', [], { header: COLORS.greenLight }),
    n('lane-system', 'lane', 20, 300, 1080, 255, 'CourtTime', [], { header: COLORS.tealLight }),
    n('lane-secretary', 'lane', 20, 555, 1080, 150, 'Secretaria', [], { header: COLORS.goldLight }),
    n('start', 'start', 112, 145, 18, 18, ''),
    n('consultar', 'action', 145, 120, 130, 55, 'Consultar reserva aprovada de hoje'),
    n('presencas', 'action', 315, 120, 135, 55, 'Marcar presentes e ausentes'),
    n('extra', 'decision', 490, 112, 100, 72, 'Há pessoa adicional?'),
    n('cadastrar', 'action', 625, 110, 150, 60, 'Cadastrar identificação, categoria e vínculo'),
    n('pag-portaria', 'action', 825, 205, 150, 58, 'Registrar pagamento adicional na portaria'),
    n('resumir', 'action', 150, 345, 150, 60, 'Contar presentes e vagas já pagas'),
    n('valor', 'decision', 350, 337, 105, 78, 'Há valor adicional?'),
    n('pag-local', 'decision', 500, 337, 110, 78, 'Pagamento na Secretaria?'),
    n('minimo', 'decision', 660, 437, 105, 78, 'Há 10 presentes?'),
    n('sem-pendencia', 'decision', 810, 437, 105, 78, 'Valor pendente = zero?'),
    n('liberar', 'action', 940, 450, 105, 55, 'Registrar liberação e congelar alterações', [], { wrap: 17, fontSize: 10.2 }),
    n('end', 'end', 1065, 465, 24, 24, ''),
    n('bloqueado', 'end', 1065, 520, 24, 24, ''),
    n('pag-secretaria', 'action', 475, 600, 165, 58, 'Registrar pagamento adicional na Secretaria'),
    n('repasse', 'action', 690, 600, 170, 58, 'Confirmar repasse recebido da portaria'),
    n('nota', 'note', 885, 585, 185, 90, '', ['Vaga paga permanece no histórico.', 'Se alguém sair, outra pessoa pode', 'ocupá-la sem cobrança duplicada.'], { fontSize: 9.7 })
  ];
  const edges = [
    e('l1', 'start', 'consultar', [pt(130, 154), pt(145, 154)], { arrow: true }),
    e('l2', 'consultar', 'presencas', [pt(275, 147), pt(315, 147)], { arrow: true }),
    e('l3', 'presencas', 'extra', [pt(450, 147), pt(490, 147)], { arrow: true }),
    e('l4', 'extra', 'cadastrar', [pt(590, 148), pt(625, 148)], { arrow: true, label: 'sim', labelPos: pt(608, 138), labelWidth: 28 }),
    e('l5', 'extra', 'resumir', [pt(540, 184), pt(540, 280), pt(225, 280), pt(225, 345)], { arrow: true, label: 'não', labelPos: pt(380, 274), labelWidth: 28 }),
    e('l6', 'cadastrar', 'resumir', [pt(700, 170), pt(700, 290), pt(225, 290), pt(225, 345)], { arrow: true }),
    e('l7', 'resumir', 'valor', [pt(300, 375), pt(350, 375)], { arrow: true }),
    e('l8', 'valor', 'pag-local', [pt(455, 375), pt(500, 375)], { arrow: true, label: 'sim', labelPos: pt(478, 365), labelWidth: 28 }),
    e('l9', 'valor', 'minimo', [pt(402, 415), pt(402, 425), pt(712, 425), pt(712, 437)], { arrow: true, label: 'não', labelPos: pt(555, 419), labelWidth: 28 }),
    e('l10', 'pag-local', 'pag-secretaria', [pt(555, 415), pt(555, 600)], { arrow: true, label: 'sim', labelPos: pt(578, 515), labelWidth: 28 }),
    e('l11', 'pag-local', 'pag-portaria', [pt(610, 375), pt(790, 375), pt(790, 234), pt(825, 234)], { arrow: true, label: 'não / acordo', labelPos: pt(705, 369), labelWidth: 62 }),
    e('l12', 'pag-secretaria', 'minimo', [pt(640, 629), pt(650, 629), pt(650, 476), pt(660, 476)], { arrow: true }),
    e('l13', 'pag-portaria', 'minimo', [pt(900, 263), pt(900, 420), pt(712, 420), pt(712, 437)], { arrow: true }),
    e('l14', 'minimo', 'sem-pendencia', [pt(765, 476), pt(810, 476)], { arrow: true, label: 'sim', labelPos: pt(788, 466), labelWidth: 28 }),
    e('l15', 'minimo', 'bloqueado', [pt(712, 515), pt(712, 532), pt(1065, 532)], { arrow: true, label: 'não', labelPos: pt(890, 526), labelWidth: 28 }),
    e('l16', 'sem-pendencia', 'liberar', [pt(915, 476), pt(940, 476)], { arrow: true, label: 'sim', labelPos: pt(928, 466), labelWidth: 28 }),
    e('l17', 'sem-pendencia', 'bloqueado', [pt(862, 515), pt(862, 527), pt(1065, 527)], { arrow: true, label: 'não', labelPos: pt(970, 521), labelWidth: 28 }),
    e('l18', 'liberar', 'end', [pt(1045, 477), pt(1065, 477)], { arrow: true }),
    e('l19', 'pag-portaria', 'repasse', [pt(900, 263), pt(900, 575), pt(775, 575), pt(775, 600)], { arrow: true, dashed: true, label: 'depois', labelPos: pt(925, 560), labelWidth: 40 })
  ];
  return { figure: 8, file: '08-atividade-liberacao-quadra', title: 'Atividades — conferência e liberação da quadra', subtitle: 'Controle transacional de presença, vagas pagas, valor adicional e repasse', caption: 'Diagrama de atividades da liberação da quadra', nodes, edges };
}

function architecture() {
  const nodes = [
    n('browser', 'external', 35, 265, 150, 90, 'Navegador', ['HTML + CSS + JS', 'sessão e CSRF']),
    n('security', 'component', 240, 120, 170, 85, 'Spring Security', ['autenticação', 'autorização por perfil']),
    n('mvc', 'component', 240, 270, 170, 100, 'Spring MVC + Thymeleaf', ['controllers', 'formulários e páginas']),
    n('services', 'component', 485, 230, 190, 140, 'Serviços de aplicação', ['regras de negócio', '@Transactional', 'bloqueios pessimistas']),
    n('repositories', 'component', 750, 120, 170, 90, 'Spring Data JPA', ['repositories', 'Hibernate ORM']),
    n('postgres', 'database', 750, 285, 170, 105, 'PostgreSQL', ['dados relacionais', 'schema via ddl-auto=update']),
    n('pdfbox', 'component', 485, 455, 190, 95, 'Apache PDFBox', ['preenche o formulário', 'oficial preservado']),
    n('files', 'database', 750, 455, 170, 105, 'Diretório privado', ['PDFs enviados', 'acesso mediado pelo serviço']),
    n('external-human', 'external', 960, 215, 135, 175, 'Serviços externos usados por pessoas', ['gov.br: assinatura', 'ITI: conferência', 'WhatsApp: aviso manual']),
    n('note', 'note', 245, 505, 175, 90, '', ['Aplicação monolítica Java 17.', 'Sem API REST pública e sem', 'frontend separado.'], { fontSize: 10.5 }),
    n('deployment', 'boundary', 215, 82, 735, 535, 'Aplicação CourtTime — Spring Boot 3.5.16')
  ];
  const edges = [
    e('ar1', 'browser', 'security', [pt(185, 290), pt(210, 290), pt(210, 162), pt(240, 162)], { arrow: true, label: 'HTTP', labelPos: pt(210, 230), labelWidth: 34 }),
    e('ar2', 'browser', 'mvc', [pt(185, 315), pt(240, 315)], { arrow: true, label: 'requisição / resposta', labelPos: pt(212, 305), labelWidth: 100 }),
    e('ar3', 'security', 'mvc', [pt(325, 205), pt(325, 270)], { arrow: true, label: 'sessão validada', labelPos: pt(370, 240), labelWidth: 85 }),
    e('ar4', 'mvc', 'services', [pt(410, 320), pt(485, 320)], { arrow: true }),
    e('ar5', 'services', 'repositories', [pt(675, 270), pt(700, 270), pt(700, 165), pt(750, 165)], { arrow: true }),
    e('ar6', 'repositories', 'postgres', [pt(835, 210), pt(835, 285)], { arrow: true, label: 'SQL', labelPos: pt(860, 250), labelWidth: 28 }),
    e('ar7', 'services', 'pdfbox', [pt(580, 370), pt(580, 455)], { arrow: true }),
    e('ar8', 'services', 'files', [pt(675, 340), pt(710, 340), pt(710, 507), pt(750, 507)], { arrow: true, label: 'armazenar / carregar', labelPos: pt(710, 425), labelWidth: 105 }),
    e('ar9', 'external-human', 'services', [pt(960, 300), pt(935, 300), pt(935, 420), pt(635, 420), pt(635, 370)], { dashed: true, openArrow: true, label: 'sem integração automática', labelPos: pt(790, 413), labelWidth: 120 })
  ];
  return { figure: 9, file: '09-arquitetura-courttime', title: 'Arquitetura do CourtTime', subtitle: 'Aplicação web monolítica com páginas renderizadas no servidor e persistência relacional', caption: 'Arquitetura lógica e tecnológica do CourtTime', nodes, edges };
}

const diagrams = [
  useCaseGeneral(),
  useCasePortaria(),
  classesCore(),
  classesDocumentation(),
  erd(),
  activityComplete(),
  activityReschedule(),
  activityRelease(),
  architecture()
];

const combined = renderDrawio(diagrams);
fs.writeFileSync(path.join(DIRS.fontes, 'courttime-modelagem-profissional.drawio'), combined, 'utf8');

for (const diagram of diagrams) {
  const single = renderDrawio([diagram]);
  fs.writeFileSync(path.join(DIRS.fontes, `${diagram.file}.drawio`), single, 'utf8');
  fs.writeFileSync(path.join(DIRS.svg, `${diagram.file}.svg`), renderSvg(diagram), 'utf8');
  fs.writeFileSync(path.join(DIRS.impressao, `${diagram.file}.html`), `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>${xml(diagram.title)}</title><style>@page{size:A4 landscape;margin:0}html,body{margin:0;width:297mm;height:210mm;overflow:hidden}img{display:block;width:297mm;height:210mm;object-fit:contain}</style></head><body><img src="../svg/${diagram.file}.svg" alt="${xml(diagram.title)}"></body></html>`, 'utf8');
}

const manifest = {
  generatedAt: '2026-10-29',
  baseCommit: '4fb7a06c8343234487bd15260c67e31a3b765aaa',
  pullRequests: [85, 86, 87],
  canvas: { format: 'A4 landscape', width: W, height: H },
  diagrams: diagrams.map((diagram) => ({
    figure: diagram.figure,
    file: diagram.file,
    title: diagram.title,
    caption: diagram.caption,
    editableSource: `fontes/${diagram.file}.drawio`,
    vector: `svg/${diagram.file}.svg`,
    printSource: `impressao/${diagram.file}.html`
  }))
};
fs.writeFileSync(path.join(ROOT, 'manifesto.json'), `${JSON.stringify(manifest, null, 2)}\n`, 'utf8');

console.log(`Gerados ${diagrams.length} diagramas e o arquivo multipágina.`);
