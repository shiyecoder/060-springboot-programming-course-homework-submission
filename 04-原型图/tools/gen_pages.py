#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 design/ 下全部可点击 HTML。用法：python3 tools/gen_pages.py [页面前缀...]"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pages_a, pages_b, pages_c  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

PAGES = pages_a.PAGES + pages_b.PAGES + pages_c.PAGES

only = sys.argv[1:]
n = 0
for name, fn in PAGES:
    if only and not any(name.startswith(o) for o in only):
        continue
    p = os.path.join(OUT, name + '.html')
    with open(p, 'w', encoding='utf-8') as f:
        f.write(fn())
    n += 1
    print('wrote', name + '.html')
print('total', n)
