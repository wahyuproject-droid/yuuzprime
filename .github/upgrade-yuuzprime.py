import copy, hashlib, pathlib, re, sys, zipfile

OLD = '0dae06beb5445e8a49a7232bed4c41d7fba044a690c83cd229498cbca0fce0f9'
NEW = '1919c7c95d0ab1858105db39c39eaf46911aeb8dc298f007c8fe6d3566251f6e'
archive = pathlib.Path(sys.argv[1])

def manifest(z):
    return hashlib.sha256(''.join(f'{n}\t{hashlib.sha256(z.read(n)).hexdigest()}\n' for n in sorted(z.namelist())).encode()).hexdigest()

def replace(s, a, b, count=1):
    if s.count(a) < count:
        raise ValueError('Expected source fragment absent')
    return s.replace(a,b,count)

with zipfile.ZipFile(archive) as original:
    if manifest(original) != OLD:
        if manifest(original) == NEW:
            print('Already updated to 1.0.5')
            sys.exit(0)
        raise SystemExit('Unexpected source archive; refusing to overwrite')
    workflow = original.read('.github/workflows/build-apk.yml').decode()
    workflow = replace(workflow, "      - 'YuuZPrime-Valora-Full.zip'\n", "      - 'YuuZPrime-Valora-Full.zip'\n      - 'app.config.js'\n      - 'package.json'\n      - 'package-lock.json'\n      - 'src/**'\n      - 'assets/**'\n      - 'plugins/**'\n")
    workflow = replace(workflow, "            unzip -q \"$GITHUB_WORKSPACE/YuuZPrime-Valora-Full.zip\" -d \"$project\"", "            archive=\"$GITHUB_WORKSPACE/YuuZPrime-Valora-Full.zip\"\n            test -f \"$archive\" || { echo '::error::Upload YuuZPrime-Valora-Full.zip at repository root'; exit 1; }\n            unzip -tq \"$archive\"\n            unzip -q \"$archive\" -d \"$project\"\n            if [[ ! -f \"$project/package.json\" ]]; then\n              mapfile -t candidates < <(find \"$project\" -maxdepth 3 -name app.config.js -type f)\n              if [[ ${#candidates[@]} -ne 1 ]]; then echo '::error::Cannot uniquely locate Expo project'; exit 1; fi\n              project=\"$(dirname \"${candidates[0]}\")\"\n            fi")
    workflow = replace(workflow, "          echo \"directory=$project\" >> \"$GITHUB_OUTPUT\"", "          echo \"Source file count: $(find \"$project\" -type f | wc -l)\"\n          echo \"directory=$project\" >> \"$GITHUB_OUTPUT\"")
    assert hashlib.sha256(workflow.encode()).hexdigest() == '46f5b2df5c18c947a085581a39f3938da834f66ad30d11d2bb0cc589c3951e38'
    changed = {'.github/workflows/build-apk.yml': workflow, 'README.md': '''# YuuZPrime 1.0.5

Independent modification by YuuZGilakPrompt, based on the supplied Valora Film (Valora / Zyxone) React Native / Expo project. Original Apache-2.0 license and attribution retained; see LICENSE, NOTICE-YUUZPRIME.md and README-UPSTREAM.md.

## Build APK via GitHub Actions
1. Upload `YuuZPrime-Valora-Full.zip` to the repository root (replace the old ZIP).
2. Copy the supplied `build-yuuzprime-v7.yml` into `.github/workflows/build-apk.yml` (replace the old workflow). The ZIP also contains the workflow for extracted-source repositories.
3. Commit the changes. The workflow extracts the ZIP automatically, uses locked npm dependencies, generates the Android project, and pins Google Cast 22.1.0 and MediaRouter 1.8.1.
4. Find the signed testing APK under Actions > Build YuuZPrime APK > Artifacts > YuuZPrime-APK.

No full Android build has been executed in this editing environment. The included debug signing key is for testing only. Production distribution requires your own secure signing key.

## Catalog
Original search and screens remain. The optional Katalog TMDB tab offers movies, TV and anime metadata with pagination. Set your authorized TMDB API key in settings if required. Metadata does not imply playable video availability. The project does not host media; use only content you are authorized to access. Existing upstream provider implementation is unmodified.

Support: https://sociabuzz.com/yuuzdisini
'''}
    def get(n): return original.read(n).decode()
    n='NOTICE-YUUZPRIME.md'; changed[n]=get(n)+'\nVersion 1.0.5: re-applied branding and metadata catalog to supplied source, original search default restored, CI extraction improved.\n'
    n='app.config.js'; changed[n]=replace(replace(get(n),"version: '1.0.4'","version: '1.0.5'"),'versionCode: 194','versionCode: 195')
    n='package.json'; changed[n]=replace(get(n),'"version": "1.0.4"','"version": "1.0.5"')
    n='package-lock.json'; changed[n]=replace(get(n),'"version": "1.0.4"','"version": "1.0.5"',2)
    n='src/components/FirstOpenProviderPopup.tsx'; changed[n]=replace(replace(replace(get(n),'For better results, install more providers! If you face any issues finding streams, try changing your active provider.','Jelajahi katalog film dan serial. Gunakan hanya sumber media yang kamu punya izin untuk akses.'),"backgroundColor: '#38bdf8', // Light blue accent color",'backgroundColor: colors.primary,'),"color: '#000000', fontWeight: 'bold'","color: colors.onPrimary, fontWeight: 'bold'")
    n='src/lib/services/searchApi.ts'; s,num=re.subn(r"const TMDB_API_KEY = '[^']+';", "import {getTmdbApiKey} from '../hooks/useTmdbStory';",get(n),count=1); assert num==1; changed[n]=replace(replace(s,'${TMDB_API_KEY}','${encodeURIComponent(getTmdbApiKey())}'),".filter((item: any) => item.media_type === 'movie' || item.media_type === 'tv')",".filter((item: any) => !item.adult && (item.media_type === 'movie' || item.media_type === 'tv'))")
    n='src/screens/Search.tsx'; changed[n]=replace(replace(get(n),'const [showCatalog, setShowCatalog] = useState(true);','const [showCatalog, setShowCatalog] = useState(false);'),'''            <Button compact variant={showCatalog ? 'filled' : 'text'} onPress={() => setShowCatalog(true)}>Katalog</Button>
            <Button compact variant={!showCatalog ? 'filled' : 'text'} onPress={() => setShowCatalog(false)}>Pencarian asli / Riwayat</Button>''','''            <Button compact variant={!showCatalog ? 'filled' : 'text'} onPress={() => setShowCatalog(false)}>Pencarian</Button>
            <Button compact variant={showCatalog ? 'filled' : 'text'} onPress={() => setShowCatalog(true)}>Katalog TMDB</Button>''')
    n='src/screens/home/Home.tsx'; changed[n]=replace(get(n),'        error={item.error}\n','')
    n='src/screens/settings/About.tsx'; changed[n]=replace(get(n),"'1.0.4'","'1.0.5'")
    temp=archive.with_suffix('.tmp.zip')
    with zipfile.ZipFile(temp,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as output:
        for info in original.infolist():
            entry=copy.copy(info)
            if entry.filename in changed:
                entry.date_time=(2026,9,2,12,19,54) if entry.filename=='src/screens/home/Home.tsx' else (2026,11,28,17,0,38)
                data=changed[entry.filename].encode()
            else:
                data=original.read(info)
            output.writestr(entry,data,compresslevel=6)
with zipfile.ZipFile(temp) as output:
    assert output.testzip() is None and manifest(output)==NEW, 'Source verification failed'
archive.write_bytes(temp.read_bytes()); temp.unlink()
print('Verified 293 files match v1.0.5; archive SHA256:',hashlib.sha256(archive.read_bytes()).hexdigest())
