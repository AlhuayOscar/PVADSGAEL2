"""Package approved Tactical Hold animations with an independent PVG controller.

Does not copy upstream Lua controllers or modify the installed upstream mod.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET

def package(repo, source, templates):
    manifest = []
    for version in ('42', '42.20'):
        media = repo / version / 'media'
        outputs = {
            'PVGIntegrationConfig.lua': 'lua/shared/PVG/Config.lua',
            'PVGIntegrationOptions.lua': 'lua/client/PVG/Options.lua',
            'PVGTacticalHold.lua': 'lua/client/PVG/TacticalHold.lua',
        }
        for template, target in outputs.items():
            dst = media / target
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(templates / template, dst)
        for src in sorted((source / 'anims_X').rglob('*.fbx')):
            dst = media / 'anims_X/PVGTactical' / src.name
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dst)
            if version == '42':
                manifest.append({'file': dst.relative_to(media).as_posix(),
                                 'sha256': hashlib.sha256(src.read_bytes()).hexdigest()})
        count = 0
        for src in sorted((source / 'AnimSets').rglob('*.xml')):
            tree = ET.parse(src)
            root = tree.getroot()
            # Vanilla fallback nodes are unnecessary: relinquish masks instead.
            conditions = root.findall('m_Conditions')
            masks = [c.find('m_StringValue') for c in conditions
                     if c.findtext('m_Name') in ('LeftHandMask', 'RightHandMask')]
            if not masks or any(v is None or not (v.text or '').startswith('Tac') for v in masks):
                continue
            for value in masks:
                value.text = 'PVG_' + value.text
            root.find('m_Name').text = 'PVG_' + root.findtext('m_Name')
            dst = media / 'AnimSets' / src.relative_to(source / 'AnimSets').parent / ('PVG_' + src.name)
            dst.parent.mkdir(parents=True, exist_ok=True)
            tree.write(dst, encoding='utf-8', xml_declaration=True)
            count += 1
        assert count >= 11, f'Unexpected animation node count: {count}'
        print(f'{version}: {count} namespaced nodes, {len(manifest)} approved animations')
    target = repo / 'docs/integrations/tactical-assets.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--templates', type=Path, default=Path(__file__).parent)
    args = parser.parse_args()
    package(args.repo, args.source, args.templates)
