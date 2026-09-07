import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const dir = dirname(fileURLToPath(import.meta.url));
const browser = await chromium.launch({ headless: true, args: ['--force-device-scale-factor=1'] });
const page = await browser.newPage({ viewport: { width: 306, height: 140 }, deviceScaleFactor: 1 });
for (const name of ['original', 'component']) {
  await page.goto(`file://${join(dir, `${name}-card.html`)}`);
  await page.screenshot({ path: join(dir, `${name}.png`) });
}
await browser.close();
