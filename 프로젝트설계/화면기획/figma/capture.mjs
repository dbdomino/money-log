import { chromium } from 'playwright';
import { mkdir } from 'fs/promises';
import path from 'path';
import { fileURLToPath, pathToFileURL } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const protoDir = path.resolve(__dirname, '../proto');
const outDir = path.resolve(__dirname, 'png');

const shots = [
  { file: 'auth-login.html', name: '01-auth-login' },
  { file: 'auth-signup.html', name: '02-auth-signup' },
  { file: 'auth-find-id.html', name: '02b-auth-find-id' },
  { file: 'auth-find-password.html', name: '02c-auth-find-password' },
  { file: 'auth-reset-password.html', name: '02d-auth-reset-password' },
  { file: 'error-forbidden.html', name: '03-error-forbidden' },
  { file: 'payment-list.html', name: '04-payment-list' },
  { file: 'payment-list.html', name: '04b-payment-create-modal', open: '[data-modal-open="modal-payment-create"]' },
  { file: 'expend-group-list.html', name: '05-expend-group-list' },
  { file: 'ledger-monthly.html', name: '06-ledger-monthly' },
  { file: 'ledger-monthly.html', name: '06b-ledger-expense-modal', query: '?m=expense-create' },
  { file: 'ledger-excel.html', name: '07-ledger-excel' },
  { file: 'fixed-expense-list.html', name: '08-fixed-expense-list' },
  { file: 'member-admin-list.html', name: '09-member-admin-list' },
  { file: 'member-profile.html', name: '10-member-profile' },
  { file: 'target-default.html', name: '11-target-default' },
  { file: 'statistics-monthly.html', name: '12-statistics-monthly' },
];

await mkdir(outDir, { recursive: true });
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });

for (const s of shots) {
  const fileUrl = pathToFileURL(path.join(protoDir, s.file)).href + (s.query || '');
  await page.goto(fileUrl, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(400);
  if (s.open) {
    const btn = page.locator(s.open).first();
    if (await btn.count()) {
      await btn.click();
      await page.waitForTimeout(400);
    }
  }
  await page.screenshot({ path: path.join(outDir, s.name + '.png'), fullPage: true });
  console.log('wrote', s.name);
}

await browser.close();
console.log('done');
