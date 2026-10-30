"""Calcula listas/sumário pela exportação nativa; numera o PDF sem alterar seu corpo.

Uso: python paginar.py origem.pdf indices.json resultado.json [destino.pdf]
O JSON de entrada contém blocos com tag/items/text. Números contam desde a
folha de rosto; a impressão começa na introdução, conforme o manual ETEC.
"""
import json
import re
import sys
import unicodedata
from pathlib import Path
import pymupdf as fitz

source, index_file, result_file = map(Path, sys.argv[1:4])
doc = fitz.open(source)
blocks = json.loads(index_file.read_text(encoding="utf-8"))

def normal(text):
    return re.sub(r"\s+", " ", unicodedata.normalize("NFKC", text)).strip().casefold()

intro = next(i for i, page in enumerate(doc)
             if page.get_text().lstrip().startswith("INTRODUÇÃO"))
texts = [p.get_text() for p in doc]
fonts = {False: fitz.Font(fontfile="C:/Windows/Fonts/arial.ttf"),
         True: fitz.Font(fontfile="C:/Windows/Fonts/arialbd.ttf")}
mapping = {}
for block in blocks:
    bold = block["tag"].startswith("SUMARIO")
    font = fonts[bold]
    lines = []
    for item in block["items"]:
        # Exigir início/fim de linha evita confundir menções a um capítulo
        # no corpo de outro capítulo com o cabeçalho efetivamente paginado.
        pattern = re.compile(r"^" + r"\s+".join(re.escape(w) for w in item.split())
                             + r"\s*$", re.MULTILINE)
        matches = [i for i in range(intro, len(doc))
                   if pattern.search(texts[i])]
        if not matches:
            raise ValueError(f"Título não localizado: {item}")
        number = str(matches[0])
        mapping[item] = int(number)
        words = item.split()
        wrapped = []
        current = ""
        for word in words:
            test = (current + " " + word).strip()
            if current and font.text_length(test, fontsize=12) > 420:
                wrapped.append(current)
                current = word
            else:
                current = test
        wrapped.append(current)
        last = wrapped[-1]
        # Líderes calculados na fonte real, com folga mínima para o número.
        while font.text_length(last + " ...... " + number, fontsize=12) > 452:
            split = last.rsplit(" ", 1)
            if len(split) != 2:
                raise ValueError(item)
            wrapped[-1] = split[0]
            wrapped.append(split[1])
            last = wrapped[-1]
        dots = 1
        while font.text_length(last + " " + "." * (dots+1) + " " + number,
                               fontsize=12) <= 452:
            dots += 1
        wrapped[-1] = last + " " + "." * dots + " " + number
        lines.extend(wrapped)
    block["replacement"] = "\n".join(lines)

result = {"paginas": len(doc), "introducao_fisica": intro+1,
          "mapa": mapping, "blocos": blocks}
result_file.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
if len(sys.argv) > 4:
    for i in range(intro, len(doc)):
        p = doc[i]
        p.insert_font(fontname="ArialCourtTime", fontfile="C:/Windows/Fonts/arial.ttf")
        label = str(i)
        w = fonts[False].text_length(label, fontsize=12)
        p.insert_text((p.rect.width-70.866-w, p.rect.height-35.433), label,
                      fontsize=12, fontname="ArialCourtTime")
    doc.set_metadata({**doc.metadata, "title": "CourtTime — candidato acadêmico para revisão",
                      "subject": "Revisão acadêmica de 29/10/2026; sem homologação institucional"})
    doc.save(sys.argv[4], garbage=4, deflate=True)
print(json.dumps({"paginas":len(doc), "titulos_localizados":len(mapping),
                  "introducao_fisica":intro+1}, ensure_ascii=True))
