"""Capture unedited APK screenshots through ADB, without writing application data."""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ.get("ANDROID_HOME", str(Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk")))
ADB = str(SDK / "platform-tools/adb.exe")
OUTPUT = ROOT / "design/admin/screenshots"
sys_devices = subprocess.check_output([ADB, "devices"], text=True)
devices = [line.split()[0] for line in sys_devices.splitlines() if line.endswith("\tdevice")]
if len(devices) != 1:
    raise SystemExit("Exactly one Android device is required")
PREFIX = [ADB, "-s", devices[0]]

def run(*args):
    return subprocess.check_output(PREFIX + list(args), timeout=20).decode("utf-8")

def nodes():
    run("shell", "rm", "-f", "/sdcard/andzen-capture.xml")
    result = run("shell", "uiautomator", "dump", "/sdcard/andzen-capture.xml")
    if "dumped" not in result.lower():
        return []
    return [node.attrib for node in ET.fromstring(run("shell", "cat", "/sdcard/andzen-capture.xml")).iter("node")]

def find(text=None, resource=None, clickable=False):
    for attempt in range(12):
        for node in nodes():
            if clickable and node.get("clickable") != "true": continue
            if text is not None and text not in (node.get("text"), node.get("content-desc")): continue
            if resource and not node.get("resource-id", "").endswith("/" + resource): continue
            return node
        time.sleep(0.5)
    raise RuntimeError("UI did not load: " + str(text or resource))

def tap(text=None, resource=None):
    node = find(text, resource, True)
    x1,y1,x2,y2 = map(int,re.findall(r"\d+",node["bounds"]))
    run("shell","input","tap",str((x1+x2)//2),str((y1+y2)//2))

size = re.findall(r"(\d+)x(\d+)", run("shell", "wm", "size"))[-1]
width, height = map(int,size)

def swipe(up=True):
    start,end = (0.88,0.34) if up else (0.34,0.88)
    run("shell","input","swipe",str(width//2),str(int(height*start)),str(width//2),str(int(height*end)),"500")

def shot(name):
    OUTPUT.mkdir(parents=True,exist_ok=True)
    time.sleep(0.3)
    png = subprocess.check_output(PREFIX+["exec-out","screencap","-p"], timeout=20)
    if not png.startswith(b"\x89PNG"): raise RuntimeError("Invalid screenshot")
    (OUTPUT / name).write_bytes(png)
    print(json.dumps({"screenshot":name,"device":devices[0]},ensure_ascii=False),flush=True)

def top():
    swipe(False); swipe(False)

find(resource="adminPitchName")
shot("pitches-top.png"); swipe(); shot("pitches-content.png"); top()
tap("Lịch đặt"); find("Quản lý lịch đặt"); tap("Đã xác nhận")
shot("bookings-top.png"); swipe(); shot("bookings-content.png"); top()
tap("Khung giờ"); find("Còn trống")
from datetime import datetime,timedelta
from zoneinfo import ZoneInfo
tomorrow=datetime.now(ZoneInfo("Asia/Ho_Chi_Minh"))+timedelta(days=1)
tap(f"Ngày mai, {tomorrow:%d/%m}"); find("Còn trống")
shot("slots-top.png"); swipe(); shot("slots-content.png"); top()
tap("Nhân viên"); find("Quản lý nhân viên")
shot("staff-top.png"); swipe(); shot("staff-content.png")
(OUTPUT/"capture.json").write_text(json.dumps({"device":devices[0],"resolution":[width,height],"source":"ADB exec-out screencap -p","edited":False},indent=2),encoding="utf-8")
