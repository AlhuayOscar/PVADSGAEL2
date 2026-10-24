from pathlib import Path
import re, json

repo=Path(r'C:\Users\carlos\Documents\Codex\2026-10-18\fijate-en-github-la-ultima-rama\work\PVADSGAEL2-latest')
user=Path(r'C:\Users\carlos\Zomboid\Lua\PVADS_UserProfiles.txt')
optics=Path(r'C:\Users\carlos\Zomboid\Lua\PVADS_OpticProfiles.txt')
def parse(text):
    result={}
    for line in text.splitlines():
        if '=>' not in line: continue
        key,raw=line.split('=>',1)
        result[key]=dict(re.findall(r'([\w_]+)=([^;]+)',raw))
    return result
profiles=parse(user.read_text(encoding='utf-8-sig'))
optic_profiles=parse(optics.read_text(encoding='utf-8-sig'))
refs=(repo/'42/media/lua/client/ZZ_PVADS_GaelGlassReferences.lua').read_text()
mapping=dict(re.findall(r'GaelGlassReferences\["([^"]+)"\]\s*=\s*\{\s*mesh\s*=\s*"[^|]+\|([^"]+)"',refs))
lines=['-- AK74 calibrations exported from Carlos user + optic profiles, 2026-10-24.',
       '-- Shipped defaults only: later personal calibration still takes precedence.',
       'require "PVADS_GaelSavedProfiles"','']
count=0
for key,values in sorted(profiles.items()):
    if not key.startswith('Base.AK74|'): continue
    weapon,optic=key.split('|',1)
    mesh=mapping.get(optic,optic.rsplit('.',1)[-1]).lower()
    merged={}
    if optic:
        merged.update(optic_profiles.get(mesh+'@scope',{}))
        merged.update(optic_profiles.get(mesh+'@scope@ak74',{}))
    merged.update(values)
    merged.pop('gaelGlassBaseVersion',None)
    fields=[]
    for name,value in sorted(merged.items()):
        if name in ('mode','reticle'):
            fields.append(name+' = '+json.dumps(value))
        else:
            try: number=float(value)
            except ValueError: continue
            if not (float('-inf')<number<float('inf')): raise ValueError(key)
            fields.append(name+' = '+format(number,'.9g'))
    lines.append('PVADS.RegisterWeaponOpticOverride('+json.dumps(weapon)+', '+json.dumps(optic)+', { '+', '.join(fields)+' })')
    count+=1
assert count==45,count
for version in ('42','42.20'):
    (repo/version/'media/lua/client/ZZZZZ_PVADS_AK74Calibrations.lua').write_text('\n'.join(lines)+'\n',encoding='utf8')
print('Exported',count,'AK74 defaults, including relevant optic calibration, to both variants.')
