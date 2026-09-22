#!/usr/bin/env python3
"""Native screenrecord supervisor; uses only the dedicated emulator and this debug app's files."""
from pathlib import Path
import re
import signal
import subprocess
import time

package = 'com.example.onemove'
output = Path('lab-evidence/recorder')
output.mkdir(parents=True, exist_ok=True)
running = True
recorder = None
log = None
last = ''

def shell(command: str) -> str:
    return subprocess.run(['adb', 'shell', command], capture_output=True, text=True, timeout=10).stdout.strip()

def ack(message: str) -> None:
    assert re.fullmatch(r'(?:error-)?(?:start|stop)-(?:0[1-9]|1[0-2])', message)
    shell(f"run-as {package} sh -c 'echo {message} > files/lab/record-ack'")

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
shell('mkdir -p /sdcard/Download/one-move-lab')
try:
    while running:
        request = shell(f'run-as {package} cat files/lab/record-request 2>/dev/null')
        match = re.fullmatch(r'(start|stop)-(0[1-9]|1[0-2])', request)
        if match and request != last:
            action, level = match.groups()
            with (output / 'events.log').open('a') as events:
                events.write(f'{time.time():.3f} {request}\n')
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
                ack(request)
                last = request
            except Exception as exc:
                (output / 'error.txt').write_text(repr(exc))
                ack(f'error-{request}')
                raise
        time.sleep(.12)
finally:
    stop()
