"""Auditoria reproduzível de PDF; cria mapas de páginas e pranchas de revisão."""
import json,re,sys,unicodedata
from pathlib import Path
import pymupdf

src=Path(sys.argv[1]);out=Path(sys.argv[2]);out.mkdir(parents=True,exist_ok=True)
d=pymupdf.open(src)
def norm(s):
    return re.sub(r'\s+',' ',unicodedata.normalize('NFKC',s).replace('\u200b','')).strip()
intro=next(i for i,p in enumerate(d) if p.get_text().lstrip().startswith('INTRODUÇÃO'))
report={'arquivo':src.name,'paginas':len(d),'introducao_fisica':intro+1,'paginas_vazias':[],'fora_margens':[],'figuras':{},'quadros':{},'pages':[], 'a4':True, 'foliacao':[]}
for i,p in enumerate(d):
    if abs(p.rect.width-595.276)>1 or abs(p.rect.height-841.89)>1:report['a4']=False
    text=p.get_text();report['pages'].append({'fisica':i+1,'numero':i if i>=intro else None,'texto':text,'imagens':len(p.get_images())})
    if len(text.strip())<3 and not p.get_images():report['paginas_vazias'].append(i+1)
    if i>=intro:
        for k,key in [('Figura','figuras'),('Quadro','quadros')]:
            for m in re.finditer(k+r'\s+(\d+)\s*[—–.]',text):report[key].setdefault(m.group(1),i)
    for b in p.get_text('dict')['blocks']:
        if b.get('type')!=0:continue
        for l in b['lines']:
            for s in l['spans']:
                x0,y0,x1,y1=s['bbox']
                folio=s['text'].strip()==str(i) and i>=intro and y0>p.rect.height-50
                if folio:report['foliacao'].append({'fisica':i+1,'numero':i})
                if not folio and (x0<68 or x1>p.rect.width-68 or y0<60 or y1>p.rect.height-60):
                    report['fora_margens'].append({'pagina':i+1,'bbox':[round(v,1) for v in s['bbox']],'texto':s['text'][:90]})
for group in range(0,len(d),4):
    sheet=pymupdf.open();page=sheet.new_page(width=1190.55,height=1683.78)
    for j in range(4):
        if group+j>=len(d):break
        x=(j%2)*595.276;y=(j//2)*841.89
        page.show_pdf_page(pymupdf.Rect(x,y,x+595.276,y+841.89),d,group+j)
        page.insert_text((x+8,y+16),f'FISICA {group+j+1}',fontsize=10,color=(0.8,0,0))
    page.get_pixmap().save(out/f'prancha-{group//4+1:02}.png');sheet.close()
(out/'auditoria.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k!='pages'},ensure_ascii=True)[:1500])
