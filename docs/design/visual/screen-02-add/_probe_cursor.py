#!/usr/bin/env python3
"""用 Chrome 实测 design.html 里 NAME 白框中 `▮` 光标的渲染尺寸，作为 Compose 光标宽度的依据。"""
import json
import re
import subprocess
from pathlib import Path
import html as _html

HERE = Path("/Users/cch/Desktop/Time/docs/design/visual/screen-02-add")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

PROBE = """
<pre id="__probe" style="display:none"></pre>
<script>
(function(){
  var out={};
  var boxes=document.querySelectorAll('.form-scroll .px > div, .form-scroll .px4 > div');
  var cursor=null, cbox=null;
  Array.prototype.forEach.call(boxes, function(d){
    if (cursor) return;
    var s=d.querySelector('span[style*="blink"]');
    if (s) { cursor=s; cbox=d; }
  });
  if (cursor) {
    var r=cursor.getBoundingClientRect(), p=cbox.getBoundingClientRect();
    var cs=getComputedStyle(cursor);
    out.cursor={w:+r.width.toFixed(2), h:+r.height.toFixed(2),
                x:+r.x.toFixed(2), y:+r.y.toFixed(2),
                fontSize:cs.fontSize, lineHeight:cs.lineHeight,
                fontFamily:cs.fontFamily, display:cs.display};
    out.cursorBox={x:+p.x.toFixed(2), y:+p.y.toFixed(2), w:+p.width.toFixed(2), h:+p.height.toFixed(2),
                   padding:getComputedStyle(cbox).padding, fontSize:getComputedStyle(cbox).fontSize,
                   lineHeight:getComputedStyle(cbox).lineHeight, fontFamily:getComputedStyle(cbox).fontFamily};
    out.prevText = cursor.previousSibling ? cursor.previousSibling.textContent : null;
  } else {
    out.error = "blink span not found";
  }
  // 用 canvas 取 `▮` 在 14px mono 下的真实墨迹范围（actualBoundingBox*）
  var cv=document.createElement('canvas'), cx=cv.getContext('2d');
  cx.font='14px ui-monospace, SFMono-Regular, Menlo, monospace';
  var m=cx.measureText('\\u25AE');
  out.glyph={advance:+m.width.toFixed(2),
             inkW:+(m.actualBoundingBoxLeft+m.actualBoundingBoxRight).toFixed(2),
             inkH:+(m.actualBoundingBoxAscent+m.actualBoundingBoxDescent).toFixed(2),
             ascent:+m.actualBoundingBoxAscent.toFixed(2),
             descent:+m.actualBoundingBoxDescent.toFixed(2)};
  document.getElementById('__probe').textContent=JSON.stringify(out);
})();
</script>
"""


def probe(mode: str) -> None:
    src = HERE / f"_design_standalone_{mode}.html"
    text = src.read_text(encoding="utf-8")
    f = HERE / f"_probe_cursor_{mode}.html"
    f.write_text(text.replace("</body>", PROBE + "</body>"), encoding="utf-8")
    dump = subprocess.run(
        [CHROME, "--headless=new", "--disable-gpu", "--window-size=314,648",
         "--virtual-time-budget=3000", "--dump-dom", f.as_uri()],
        capture_output=True, text=True, check=True).stdout
    m = re.search(r'<pre id="__probe"[^>]*>(.*?)</pre>', dump, re.S)
    if not m:
        print(f"{mode}: __probe 未出现")
        return
    print(f"===== {mode} =====")
    print(json.dumps(json.loads(_html.unescape(m.group(1))), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    probe("future")
    probe("past")