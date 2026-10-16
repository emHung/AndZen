"""Vendor selected official Remix Icon paths as Android vectors; preview by default."""
import argparse
import json
import time
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError
from xml.etree import ElementTree as ET
from xml.sax.saxutils import quoteattr

ROOT = Path(__file__).resolve().parents[1]
ICONS = {
    "System/refresh-line.svg": "ic_ri_refresh_line",
    "System/add-line.svg": "ic_ri_add_line",
    "System/lock-line.svg": "ic_ri_lock_line",
    "System/lock-unlock-line.svg": "ic_ri_lock_unlock_line",
    "System/check-line.svg": "ic_ri_check_line",
    "System/arrow-down-s-line.svg": "ic_ri_arrow_down_s_line",
    "Document/file-list-3-line.svg": "ic_ri_file_list_3_line",
}

def fetch(url):
    request = Request(url, headers={"User-Agent": "AndZen-icon-vendor", "Accept": "application/vnd.github+json"})
    for attempt in range(3):
        try:
            with urlopen(request, timeout=30) as reply:
                return reply.read()
        except (HTTPError, URLError) as error:
            if isinstance(error, HTTPError) and error.code < 500 and error.code != 429:
                raise
            if attempt == 2:
                raise
            time.sleep(0.5 * 2 ** attempt)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    if not args.apply:
        print(json.dumps({"mode": "preview", "icons": ICONS}))
        return
    commit = json.loads(fetch("https://api.github.com/repos/Remix-Design/RemixIcon/commits/master"))["sha"]
    tree = json.loads(fetch(f"https://api.github.com/repos/Remix-Design/RemixIcon/git/trees/{commit}?recursive=1"))["tree"]
    staged = {}
    sources = {}
    for relative, name in ICONS.items():
        candidates = [row["path"] for row in tree if row["path"].startswith("icons/") and row["path"].endswith("/" + relative.split("/")[-1])]
        if len(candidates) != 1:
            raise RuntimeError(f"Expected exactly one official icon: {relative}")
        sources[name] = candidates[0]
        url = f"https://raw.githubusercontent.com/Remix-Design/RemixIcon/{commit}/{candidates[0]}"
        svg = ET.fromstring(fetch(url))
        paths = [p.attrib["d"] for p in svg.iter() if p.tag.endswith("path") and p.get("d")]
        if not paths:
            raise RuntimeError(f"No vector paths: {relative}")
        vector = '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'
        vector += ''.join(f'    <path android:fillColor="@color/primary_green" android:pathData={quoteattr(path)} />\n' for path in paths)
        staged[name] = vector + '</vector>\n'
    for name, vector in staged.items():
        (ROOT / "app/src/main/res/drawable" / (name + ".xml")).write_text(vector, encoding="utf-8")
    target = ROOT / "design/admin/remixicon-sources.json"
    target.write_text(json.dumps({"repository": "https://github.com/Remix-Design/RemixIcon", "commit": commit,
        "license": "https://github.com/Remix-Design/RemixIcon/blob/" + commit + "/License", "icons": sources}, indent=2), encoding="utf-8")
    print(json.dumps({"status": "saved", "icons": len(staged), "commit": commit}))

if __name__ == "__main__":
    main()
