"""Build docs/demo/Vectorize-Demo-Script.html from the hand-edited Vectorize-Demo-Script.docx.

Usage (from the repo root): python3 docs/demo/src/build_html.py
Optional explicit paths:     python3 build_html.py <script.docx> <nodes.json> <template.html> <out.html>
The docx stays the source of truth for wording; this script only adds structure
(speaker, seconds, diagram node links) and injects it into the template.
"""
import json, re, sys, zipfile
import xml.etree.ElementTree as ET

import os
HERE = os.path.dirname(os.path.abspath(__file__))
DEMO = os.path.dirname(HERE)
DOCX, NODES, TEMPLATE, OUT = sys.argv[1:5] if len(sys.argv) >= 5 else (
    os.path.join(DEMO, 'Vectorize-Demo-Script.docx'), os.path.join(HERE, 'nodes.json'),
    os.path.join(HERE, 'template.html'), os.path.join(DEMO, 'Vectorize-Demo-Script.html'))
W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'


def ptext(p):
    s = ''
    for r in p.iter(W + 'r'):
        fonts = r.find(f'{W}rPr/{W}rFonts')
        bold = r.find(f'{W}rPr/{W}b') is not None
        t = ''.join(x.text or '' for x in r.iter(W + 't'))
        if not t:
            continue
        if fonts is not None and 'Consolas' in (fonts.get(W + 'ascii') or ''):
            t = '`' + t + '`'
        elif bold and t.strip():
            t = '**' + t + '**'
        s += t
    return s.replace('``', '').replace('****', '')


root = ET.fromstring(zipfile.ZipFile(DOCX).read('word/document.xml'))
blocks = []
for el in root.find(W + 'body'):
    if el.tag == W + 'p':
        st = el.find(f'{W}pPr/{W}pStyle')
        st = st.get(W + 'val') if st is not None else ''
        t = ptext(el).strip()
        if not t:
            continue
        kind = 'li' if el.find(f'{W}pPr/{W}numPr') is not None else st or 'p'
        blocks.append((kind, t))
    elif el.tag == W + 'tbl':
        rows = [[' '.join(ptext(p).strip() for p in tc.findall(W + 'p')).strip()
                 for tc in tr.findall(W + 'tc')] for tr in el.findall(W + 'tr')]
        blocks.append(('table', rows[1:]))  # drop header row


def strip_bold(s):
    return s.replace('**', '').strip()


def secs(t):
    m, s = t.strip().split(':')
    return int(m) * 60 + int(s)


# ---------- diagram node links (ids from the archify page) ----------
SEG_NODES = {
    1: ['app'], 2: ['view', 'controller'], 3: ['view', 'controller', 'session'],
    4: ['controller', 'preprocessor', 'colors', 'background'], 5: ['presets', 'presetfile'],
    6: ['zip', 'importer'], 7: ['coordinator', 'policy'],
    8: ['vectorization', 'preprocessor', 'engine', 'validator'],
    9: ['export', 'outdir', 'coordinator'], 10: ['importer', 'controller', 'session'],
    11: ['view', 'controller', 'coordinator', 'policy'], 12: [],
}
QA_NODES = {
    'drag and drop': ['view'], 'choose files button': ['controller'],
    'what happens after a drop': ['controller', 'coordinator', 'importer'],
    'drop screen layout': ['view'], 'editor layout': ['view'], 'all event wiring': ['controller'],
    'zoom in / out': ['controller', 'view'], 'zoom to fit': ['view'],
    'back / next': ['controller', 'session'], 'how a setting change': ['controller', 'coordinator'],
    'why the': ['controller'], 'custom colours max 5': ['view'],
    'detected colour swatches': ['controller', 'colors'], 'show the original image': ['view'],
    'show the svg': ['view'], 'checkerboard': ['view'], 'progress bar': ['controller', 'coordinator'],
    'export all button': ['controller', 'coordinator'], 'additional feature 1': ['presets', 'presetfile'],
    'additional feature 2': ['controller', 'session', 'view'], 'where the app starts': ['app'],
    'zipimport': ['zip'], 'zip slip protection': ['zip'], 'zip temp cleanup': ['zip', 'app'],
    'image import': ['importer'], 'the parallel rule': ['policy'], 'thread pools': ['coordinator'],
    'where the task is': ['coordinator'], 'preprocessing after a drop': ['coordinator', 'policy'],
    'instant preview': ['coordinator', 'session'], 'cancel': ['coordinator', 'engine'],
    'the vectorization pipeline': ['vectorization'], 'detail level': ['preprocessor', 'engine'],
    'colour detection': ['colors'], 'reducing to n colours': ['colors'],
    'custom vs unlimited': ['preprocessor'], 'remove background': ['background', 'preprocessor'],
    'calling autotrace': ['engine', 'autotrace'], 'autotrace missing': ['engine'],
    'why an interface': ['engine'], 'svg validation': ['validator'], 'export': ['export', 'outdir'],
    'settings model': ['session'], 'custom exception': [],
    # concept questions
    'what is event-driven': ['controller'], 'setonaction vs': ['controller'], 'why webview': ['view'],
    'why doesn': ['coordinator'], 'why task instead': ['coordinator'], 'when exactly is it parallel': ['policy'],
    'how many threads': ['coordinator'], 'checked or unchecked': [], 'what is zip slip': ['zip'],
    'how do the two halves': ['controller', 'coordinator'], 'how did you test': [],
}


def qa_nodes(q):
    key = strip_bold(re.sub(r'^\*\*\((Mona|Day|Both)\)\*\*\s*', '', q)).replace('`', '').lower()
    for prefix in sorted(QA_NODES, key=len, reverse=True):
        if key.startswith(prefix):
            return QA_NODES[prefix]
    return []


# ---------- walk the blocks ----------
data = {'setup': [], 'timeline': [], 'segments': [], 'fallback': [], 'rubric': [],
        'qa': {'mona': [], 'day': [], 'concept': []}, 'intro': [], 'qaIntro': ''}
section = None
seg = None
qa_target = None
for kind, val in blocks:
    if kind == 'Title':
        continue
    if kind == 'Heading1':
        low = val.lower()
        section = ('setup' if 'before you walk' in low else 'timeline' if 'timeline' in low
                   else 'demo' if 'part 1' in low else 'rubric' if 'part 2' in low
                   else 'qa' if 'part 3' in low else section)
        continue
    if kind == 'Heading2':
        m = re.match(r'(\d+:\d\d)\s*–\s*(\d+:\d\d)\s+\*\*(\w+)\s*·\s*\*\*\s*(\d+)\.\s*(.+)', val)
        if m:
            seg = {'start': secs(m.group(1)), 'end': secs(m.group(2)), 'time': f'{m.group(1)} – {m.group(2)}',
                   'who': m.group(3).lower(), 'n': int(m.group(4)), 'title': m.group(5).strip(),
                   'rubric': '', 'rows': [], 'nodes': SEG_NODES.get(int(m.group(4)), [])}
            data['segments'].append(seg)
            continue
        low = strip_bold(val).lower()
        seg = None
        if 'something goes wrong' in low:
            section = 'fallback'
        elif low.startswith('mona'):
            qa_target = 'mona'
        elif low.startswith('day'):
            qa_target = 'day'
        elif 'concept' in low:
            qa_target = 'concept'
        continue
    if kind in ('p', 'li', 'ListParagraph'):
        if section is None:
            data['intro'].append(val)
        elif section == 'setup':
            data['setup'].append(val)
        elif section == 'fallback':
            data['fallback'].append(val)
        elif section == 'demo' and seg is not None and strip_bold(val).lower().startswith('rubric:'):
            seg['rubric'] = re.sub(r'^Rubric:\s*', '', strip_bold(val))
        elif section == 'rubric':
            data['rubricIntro'] = val
        elif section == 'qa' and qa_target is None:
            data['qaIntro'] = val
        continue
    if kind == 'table':
        if section == 'timeline':
            data['timeline'] = [{'time': r[0], 'who': strip_bold(r[1]).lower(), 'what': r[2], 'rubric': r[3]} for r in val]
        elif section == 'demo' and seg is not None:
            for do, say in val:
                who = None
                m = re.match(r'^\*\*(Mona|Day):\*\*\s*', do)
                if m:
                    who = m.group(1).lower()
                    do = do[m.end():]
                seg['rows'].append({'do': do, 'say': say, 'who': who})
        elif section == 'rubric':
            data['rubric'] = [{'row': r[0], 'pts': r[1], 'by': r[2], 'sees': r[3], 'proof': r[4]} for r in val]
        elif section == 'qa' and qa_target:
            for r in val:
                if qa_target == 'concept':
                    m = re.match(r'^\*\*\((Mona|Day|Both)\)\*\*\s*', r[0])
                    who = m.group(1).lower() if m else 'both'
                    q = r[0][m.end():] if m else r[0]
                    data['qa']['concept'].append({'q': q, 'a': r[1], 'who': who, 'nodes': qa_nodes(r[0])})
                else:
                    data['qa'][qa_target].append({'q': r[0], 'where': r[1], 'say': r[2], 'who': qa_target,
                                                  'nodes': qa_nodes(r[0])})

# ---------- demo input files (docs/demo/demo-files, named <segment>-<who>-<what>) ----------
FILES_DIR = os.path.join(DEMO, 'demo-files')
FILE_NOTES = {
    '02-Mona-drop-a-logo.png': 'Flat logo, 5 colours on white. Used again in 4 for Detail, Colors and Remove Background.',
    '02-Mona-drop-b-photo.jpg': 'Photo-like JPG. Don\'t switch it to Unlimited (8 MB SVG, slow preview).',
    '02-Mona-drop-c-transparent.png': 'Heart on a transparent background.',
    '05-Mona-preset-logo-style.properties': 'Not there before the demo: Mona creates it with Save (Preset), then Loads it.',
    '06-Day-zip-images.zip': 'Total goes from 3 to 7. The app names them 0_badge.png, 1_leaf.jpg, 2_house.png, 3_sunset.jpg. readme.txt is skipped.',
    '09-Day-export-out': 'Empty folder to pick in Export All. Must be empty before the demo.',
    '10-Day-error-a-broken.jpg': 'Text renamed to .jpg: "Unsupported image format".',
    '10-Day-error-b-notes.txt': 'Not an image: "unsupported file type".',
    '10-Day-error-c-good-star.png': 'Always drop with the two bad files, or the errors stay hidden on the drop screen.',
    'qa-zip-one-bad-member.zip': 'One corrupt member: the 2 good images still import.',
    'qa-zip-slip.zip': 'Zip Slip: whole ZIP rejected, "Unsafe entry path ... ../../evil.png".',
    'qa-zip-no-images.zip': '"No supported images found".',
    'qa-preset-broken.properties': 'Load (Preset): "customColors must be between 2 and 5", app keeps running.',
}
# Segments that reuse a file dropped earlier, so every step shows what is on screen.
FILE_REUSE = {3: ['02-Mona-drop-a-logo.png', '02-Mona-drop-b-photo.jpg', '02-Mona-drop-c-transparent.png'],
              4: ['02-Mona-drop-a-logo.png'], 8: ['02-Mona-drop-b-photo.jpg', '02-Mona-drop-a-logo.png']}


def file_entry(path, rel, seg, virtual=False):
    name = os.path.basename(path)
    who = 'mona' if '-Mona-' in name else 'day' if '-Day-' in name else 'both'
    entry = {'name': name, 'path': rel, 'seg': seg, 'who': who, 'note': FILE_NOTES.get(name, ''), 'kind': 'file'}
    if virtual:
        entry['kind'] = 'created'
    elif os.path.isdir(path):
        entry['kind'] = 'folder'
    elif name.lower().endswith('.zip'):
        entry['kind'] = 'zip'
        entry['members'] = [n for n in zipfile.ZipFile(path).namelist() if not n.endswith('/')]
    else:
        head = open(path, 'rb').read(4)
        if head.startswith(b'\x89PNG') or head.startswith(b'\xff\xd8'):
            entry['kind'] = 'image'
            # PNG colour types 4 and 6 carry alpha; only those get a checkerboard behind the thumbnail.
            entry['alpha'] = head.startswith(b'\x89PNG') and open(path, 'rb').read(26)[25] in (4, 6)
    return entry


files = []
if os.path.isdir(FILES_DIR):
    for name in sorted(os.listdir(FILES_DIR)):
        full = os.path.join(FILES_DIR, name)
        m = re.match(r'^(\d+)-', name)
        if name.startswith(('.', '00-')) or not m:
            continue
        if int(m.group(1)) == 99 and os.path.isdir(full):
            for sub in sorted(os.listdir(full)):
                if not sub.startswith('.'):
                    files.append(file_entry(os.path.join(full, sub), f'demo-files/{name}/{sub}', 99))
            continue
        files.append(file_entry(full, f'demo-files/{name}', int(m.group(1))))
    preset = '05-Mona-preset-logo-style.properties'
    if not any(f['name'] == preset for f in files):
        files.append(file_entry(preset, f'demo-files/{preset}', 5, virtual=True))
    files.sort(key=lambda f: (f['seg'], f['name']))
data['files'] = files
data['fileReuse'] = FILE_REUSE

nodes = json.load(open(NODES))
if 'session' in nodes:
    nodes['session']['own'] = 'Day'
data['nodes'] = nodes

assert len(data['segments']) == 12, len(data['segments'])
assert data['qa']['mona'] and data['qa']['day'] and data['qa']['concept']
html = open(TEMPLATE, encoding='utf-8').read().replace('/*__DATA__*/null', json.dumps(data, ensure_ascii=False))
open(OUT, 'w', encoding='utf-8').write(html)
print('segments', len(data['segments']), 'qa', {k: len(v) for k, v in data['qa'].items()},
      'rubric', len(data['rubric']), 'setup', len(data['setup']), 'fallback', len(data['fallback']),
      'files', len(data['files']))
missing = [q['q'] for k in ('mona', 'day') for q in data['qa'][k] if not q['nodes']]
print('qa rows without diagram link:', missing)
