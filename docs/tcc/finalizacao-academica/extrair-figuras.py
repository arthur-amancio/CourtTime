"""Extrai as ilustrações originais incorporadas; preserva as fontes SVG/PNG novas."""
import json, re, hashlib, sys
from pathlib import Path
import pymupdf as fitz

root=Path(__file__).parent
doc=fitz.open(sys.argv[1])
intro=next(i for i,p in enumerate(doc) if p.get_text().lstrip().startswith('INTRODUÇÃO'))
known={15:'classes-nucleo.png',16:'classes-documentacao.png',17:'der-nucleo.png',18:'der-documentacao.png'}
screens=['pagina-publica','login','admin-painel','aluno-calendario','aluno-nova-solicitacao',
         'aluno-participantes','aluno-documentos','final-detalhe-cancelada','secretaria-documento',
         'secretaria-contribuicoes','admin-solicitacoes','admin-horarios','seguranca-reservas','seguranca-reserva']
known.update({n:'tela-'+name+'.png' for n,name in enumerate(screens,20)})
rows=[]
for i in range(intro,len(doc)):
    p=doc[i]
    images=p.get_image_info(xrefs=True)
    for block in p.get_text('blocks'):
        text=re.sub(r'\s+',' ',block[4]).strip()
        m=re.match(r'^Figura (\d+) [—–] ',text)
        if not m:continue
        number=int(m.group(1))
        below=[im for im in images if im['bbox'][1]>=block[3]-2]
        if not below:raise ValueError(f'Imagem ausente para a figura {number}')
        im=min(below,key=lambda x:x['bbox'][1])
        target=root/'figuras'/known.get(number,f'figura-{number:02}.png')
        if number not in known:
            pix=fitz.Pixmap(doc,im['xref'])
            mask=doc.extract_image(im['xref']).get('smask',0)
            if mask:pix=fitz.Pixmap(pix,fitz.Pixmap(doc,mask))
            if pix.n-pix.alpha>3:pix=fitz.Pixmap(fitz.csRGB,pix)
            pix.save(target)
        display=im['bbox'][2]-im['bbox'][0]
        rows.append({'figura':number,'legenda':text,'pagina':i,'fisica':i+1,
                     'arquivo':'figuras/'+target.name,'pixels':[im['width'],im['height']],
                     'dpi_horizontal':round(im['width']/display*72),
                     'sha256':hashlib.sha256(target.read_bytes()).hexdigest()})
rows.sort(key=lambda x:x['figura'])
assert [x['figura'] for x in rows]==list(range(1,34))
(root/'catalogo-figuras.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'figuras':len(rows),'dpi_min':min(x['dpi_horizontal'] for x in rows)}))
