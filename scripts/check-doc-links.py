#!/usr/bin/env python3
"""Verifica links relativos em documentos Markdown do repositório."""

import re
import sys
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parent.parent
LINK = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
IGNORAR = {".git", "build", "bin", "out", ".venv", "node_modules"}


def main():
    erros = []
    documentos = 0
    for arquivo in ROOT.rglob("*.md"):
        if any(parte in IGNORAR for parte in arquivo.relative_to(ROOT).parts):
            continue
        documentos += 1
        for numero, linha in enumerate(arquivo.read_text(encoding="utf-8").splitlines(), 1):
            for match in LINK.finditer(linha):
                destino = match.group(1).strip().strip("<>")
                if not destino or destino.startswith("#"):
                    continue
                parsed = urlsplit(destino)
                if parsed.scheme or parsed.netloc:
                    continue
                caminho = unquote(parsed.path)
                if not caminho:
                    continue
                resolvido = (arquivo.parent / caminho).resolve()
                if not resolvido.is_relative_to(ROOT) or not resolvido.exists():
                    erros.append(f"{arquivo.relative_to(ROOT)}:{numero}: {destino}")
    if erros:
        print("Links relativos inválidos:")
        for erro in erros:
            print(f"  {erro}")
        return 1
    print(f"Links Markdown verificados: {documentos} arquivos.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
