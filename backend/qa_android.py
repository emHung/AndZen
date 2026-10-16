"""Small ADB helper for inspecting and driving the connected QA device."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
import re
import sys
import time

sys.stdout.reconfigure(encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=["dump", "tap", "type", "back", "swipe", "shot", "launch"])
    parser.add_argument("value", nargs="?", default="")
    parser.add_argument("--id", default="")
    parser.add_argument("--text", default="")
    parser.add_argument("--index", type=int, default=0)
    parser.add_argument("--serial", default="")
    args = parser.parse_args()
    sdk = Path(os.environ.get("ANDROID_HOME", str(Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk")))
    adb = str(sdk / "platform-tools/adb.exe")
    devices = [line.split()[0] for line in subprocess.check_output([adb, "devices"], text=True).splitlines() if line.endswith("\tdevice")]
    if not args.serial and len(devices) != 1:
        raise SystemExit("Choose a device with --serial")
    prefix = [adb, "-s", args.serial or devices[0]]
    def run(*values):
        return subprocess.check_output(prefix + list(values), text=True, encoding="utf-8")
    if args.action in {"dump", "tap"}:
        run("shell", "rm", "-f", "/sdcard/andzen-ui.xml")
        result = run("shell", "uiautomator", "dump", "/sdcard/andzen-ui.xml")
        if "dumped" not in result.lower():
            time.sleep(1)
            result = run("shell", "uiautomator", "dump", "/sdcard/andzen-ui.xml")
        if "dumped" not in result.lower():
            raise SystemExit("Could not inspect the current Android UI")
        root = ET.fromstring(run("shell", "cat", "/sdcard/andzen-ui.xml"))
        nodes = [n.attrib for n in root.iter("node")]
        if args.action == "dump":
            for n in nodes:
                if n.get("text") or n.get("content-desc") or n.get("clickable") == "true":
                    print(json.dumps({key: n.get(key) for key in ["text", "content-desc", "resource-id", "bounds", "clickable"]}, ensure_ascii=False))
        else:
            matches = [n for n in nodes if (not args.id or n.get("resource-id", "").endswith(":" + args.id) or n.get("resource-id", "").endswith("/" + args.id))
                       and (not args.text or args.text in [n.get("text"), n.get("content-desc")])]
            if not matches or args.index >= len(matches):
                raise SystemExit("UI target not found")
            coords = list(map(int, re.findall(r"\d+", matches[args.index]["bounds"])))
            run("shell", "input", "tap", str((coords[0]+coords[2])//2), str((coords[1]+coords[3])//2))
    elif args.action == "type":
        run("shell", "input", "text", args.value.replace(" ", "%s"))
    elif args.action == "back":
        run("shell", "input", "keyevent", "4")
    elif args.action == "swipe":
        run("shell", "input", "swipe", *args.value.split(","), "450")
    elif args.action == "shot":
        run("shell", "screencap", "-p", "/sdcard/andzen-qa-screen.png")
        destination = Path(args.value).resolve()
        destination.parent.mkdir(parents=True, exist_ok=True)
        run("pull", "/sdcard/andzen-qa-screen.png", str(destination))
        print(str(destination))
    else:
        run("shell", "am", "start", "-n", "com.sanbong.app/.ui.MainActivity")


if __name__ == "__main__":
    main()
