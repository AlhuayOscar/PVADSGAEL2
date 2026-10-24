"""Package new PVG sling resources; never writes the live game/mod folder."""
from pathlib import Path
import argparse, shutil, json, xml.etree.ElementTree as ET

def package(repo, templates, jar):
    for version in ('42','42.20'):
        media=repo/version/'media'
        files={
            'PVGIntegrationConfig.lua':'lua/shared/PVG/Config.lua',
            'PVGIntegrationOptions.lua':'lua/client/PVG/Options.lua',
            'PVGSlingShared.lua':'lua/shared/PVG/SlingShared.lua',
            'PVGSlingClient.lua':'lua/client/PVG/Sling.lua',
            'PVGSling_items.txt':'scripts/PVGaelSling_items.txt',
            'PVGaelSling.xml':'clothing/clothingItems/PVGaelSling.xml',
        }
        for src,dst in files.items():
            target=media/dst;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(templates/src,target)
        shutil.copy2(jar,media/'java/PVADSGAEL2.jar')
        registry=media/'registries.lua'
        text=registry.read_text(encoding='utf-8') if registry.exists() else ''
        if 'pvg:slingfront' not in text:
            registry.write_text(text+'\nItemBodyLocation.register("pvg:slingfront")\n',encoding='utf-8')
        guid=media/'fileGuidTable.xml'
        root=ET.parse(guid).getroot() if guid.exists() else ET.Element('fileGuidTable')
        name='media/clothing/clothingItems/PVGaelSling.xml'
        if not any(node.findtext('path')==name for node in root.findall('files')):
            entry=ET.SubElement(root,'files');ET.SubElement(entry,'path').text=name
            ET.SubElement(entry,'guid').text='318ee0ca-4c5d-478a-af05-b5bcc9d987b2'
        ET.indent(root);ET.ElementTree(root).write(guid,encoding='utf-8',xml_declaration=True)
        sandbox=media/'sandbox-options.txt';text=sandbox.read_text(encoding='utf-8')
        if 'option PVADSGAEL2.IntegratedSling' not in text:
            text+='''
option PVADSGAEL2.IntegratedSling
{
    type = boolean,
    default = false,
    page = PVADSGAEL2,
    translation = PVADSGAEL2_IntegratedSling,
}
'''
            sandbox.write_text(text,encoding='utf-8')
        labels={
            'EN': ('Integrated PVADSGael sling (experimental)', 'Enables the original PVADSGael sling slot and crafting in multiplayer. Requires a restart and the matching Java build.'),
            'ES': ('Sling integrado PVADSGael (experimental)', 'Activa la ranura y fabricación del sling propio en multijugador. Requiere reiniciar y el JAR correspondiente.'),
        }
        for lang,(label,tip) in labels.items():
            target=media/f'lua/shared/Translate/{lang}/Sandbox.json'
            data=json.loads(target.read_text(encoding='utf-8-sig')) if target.exists() else {}
            data['Sandbox_PVADSGAEL2_IntegratedSling']=label
            data['Sandbox_PVADSGAEL2_IntegratedSling_tooltip']=tip
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Packaged optional original sling for 42 and 42.20; live install untouched.')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,required=True);p.add_argument('--jar',type=Path,required=True)
    p.add_argument('--templates',type=Path,default=Path(__file__).parent);a=p.parse_args();package(a.repo,a.templates,a.jar)
