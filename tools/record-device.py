#!/usr/bin/env python3
"""Native screenrecord supervisor on a dedicated CI emulator; no generated frames or AI calls."""
from pathlib import Path
import re
import signal
import subprocess
import time

root = '/data/local/tmp/one-move-lab'
output = Path('lab-evidence/recorder')
output.mkdir(parents=True, exist_ok=True)
running = True
recorder = None
log = None
last = ''

def shell(command: str) -> str:
    return subprocess.run(['adb', 'shell', command], capture_output=True, text=True, timeout=10).stdout.strip()

def stop() -> None:
    global recorder, log
    if recorder is not None:
        shell('pkill -2 screenrecord')
        try:
            recorder.wait(timeout=8)
        except subprocess.TimeoutExpired:
            recorder.terminate()
            recorder.wait(timeout=5)
        recorder = None
    if log is not None:
        log.close()
        log = None

def terminate(*_):
    global running
    running = False

signal.signal(signal.SIGTERM, terminate)
signal.signal(signal.SIGINT, terminate)
shell(f'mkdir -p {root} /sdcard/Download/one-move-lab')
try:
    while running:
        request = shell(f'cat {root}/request 2>/dev/null')
        match = re.fullmatch(r'(start|stop)-(0[1-9]|1[0-2])', request)
        if match and request != last:
            action, level = match.groups()
            try:
                stop()
                if action == 'start':
                    log = (output / f'level-{level}.log').open('w')
                    recorder = subprocess.Popen(['adb', 'shell', 'screenrecord', '--size', '720x1280', '--bit-rate', '2500000', '--time-limit', '60', f'/sdcard/Download/one-move-lab/level-{level}.mp4'], stdout=log, stderr=subprocess.STDOUT)
                    time.sleep(.8)
                    if recorder.poll() is not None:
                        raise RuntimeError('screenrecord exited before gameplay')
                else:
                    size = shell(f'stat -c %s /sdcard/Download/one-move-lab/level-{level}.mp4')
                    if not size.isdigit() or int(size) < 1000:
                        raise RuntimeError('recording file missing or empty')
                shell(f'echo {request} > {root}/ack')
                last = request
            except Exception as exc:
                (output / 'error.txt').write_text(repr(exc))
                shell(f'echo error-{request} > {root}/ack')
                raise
        time.sleep(.12)
finally:
    stop()
