import assert from 'node:assert/strict';
import { mkdir } from 'node:fs/promises';
import { chromium } from 'playwright';

const base = process.env.ACRA_UI_URL || 'http://127.0.0.1:8787/';
const output = process.env.ACRA_BROWSER_OUTPUT || 'build/browser-smoke';
await mkdir(output, { recursive: true });

const browser = await chromium.launch({ headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(base, { waitUntil: 'domcontentloaded' });
  await page.locator('#health-label').filter({ hasText: 'Local service healthy' }).waitFor();

  await page.locator('#new-project-button').click();
  await page.locator('#project-form input[name="name"]').fill('Browser acceptance lab');
  await page.locator('#project-form textarea[name="description"]').fill('Authorized offline import');
  await page.locator('#project-form button[type="submit"]').click();
  await page.locator('#targets-view.active').waitFor();

  await page.locator('#target-form input[name="displayName"]').fill('Rejected target');
  await page.locator('#target-form input[name="hostInput"]').fill('127.0.0.1@outside.invalid');
  await page.locator('#target-form input[name="authorizationReference"]').fill('CI-APPROVED-LAB');
  await page.locator('#target-form button[type="submit"]').click();
  await page.locator('#target-form-message').filter({ hasText: 'Enter a full HTTP(S) base URL' }).waitFor();
  assert.equal(await page.locator('#target-count').innerText(), '0');

  await page.locator('#target-form input[name="displayName"]').fill('Loopback API');
  await page.locator('#target-form select[name="hostScheme"]').selectOption('http');
  await page.locator('#target-form input[name="hostInput"]').fill('127.0.0.1:8081');
  await page.locator('#target-form input[name="hostBasePath"]').fill('/api/v1/');
  await page.locator('#target-form input[name="authorizationReference"]').fill('CI-APPROVED-LAB');
  await page.locator('#target-form button[type="submit"]').click();
  await page.getByText('Authorized target registered. No scan was started.').waitFor();
  await page.locator('#target-list').filter({ hasText: '127.0.0.1:8081/api/v1/' }).waitFor();
  assert.match(await page.locator('#target-list').innerText(), /127\.0\.0\.1:8081\/api\/v1\//);
  await page.screenshot({ path: `${output}/target.png`, fullPage: true });

  await page.locator('[data-view="inventory"]').click();
  await page.locator('#import-content').fill(JSON.stringify({
    openapi: '3.0.3',
    info: { title: 'Browser lab', version: '1' },
    paths: { '/users/{userId}': { get: { responses: { '200': { description: 'ok' } } } } },
  }));
  await page.locator('#import-form button[type="submit"]').click();
  await page.getByText(/OPENAPI import complete: 1 observation/).waitFor();
  await page.locator('#inventory-summary-endpoints').filter({ hasText: '1' }).waitFor();
  assert.match(await page.locator('#inventory-table-body').innerText(), /users/);
  await page.screenshot({ path: `${output}/inventory.png`, fullPage: true });

  await page.locator('[data-view="coverage"]').click();
  await page.locator('#coverage-view.active').waitFor();
  assert.equal(await page.locator('#coverage-tested').innerText(), '0');

  await page.locator('[data-view="reports"]').click();
  await page.locator('#report-form button[type="submit"]').click();
  await page.locator('#report-preview').filter({ hasText: /"projectId"/ }).waitFor();
  assert.match(await page.locator('#report-preview').innerText(), /\n  "projectId":/);
  assert.match(await page.locator('#report-sha').innerText(), /^[a-f0-9]{64}$/);
  await page.screenshot({ path: `${output}/report.png`, fullPage: true });

  await page.locator('[data-view="targets"]').click();
  await page.locator('#target-form input[name="displayName"]').fill('Authorized staging IPv4');
  await page.locator('#target-form select[name="hostScheme"]').selectOption('http');
  await page.locator('#target-form input[name="hostInput"]').fill('10.42.0.7:8081');
  await page.locator('#target-form input[name="hostBasePath"]').fill('/api/v1/');
  await page.locator('#target-form select[name="environment"]').selectOption('STAGING');
  await page.locator('#target-form select[name="testingMode"]').selectOption('SAFE_ACTIVE');
  await page.locator('#target-form input[name="authorizationReference"]').fill('CI-STAGING-SCOPE');
  await page.locator('#target-form button[type="submit"]').click();
  await page.locator('#target-list').filter({ hasText: '10.42.0.7:8081/api/v1/' }).waitFor();
  await page.locator('[data-view="active"]').click();
  await page.locator('#active-target option').filter({ hasText: 'Authorized staging IPv4' }).waitFor({ state: 'attached' });
  assert.match(await page.locator('#active-expectation option').innerText(), /Import a read-only endpoint/);
  await page.screenshot({ path: `${output}/external-target.png`, fullPage: true });

  await page.reload({ waitUntil: 'domcontentloaded' });
  await page.locator('#health-label').filter({ hasText: 'Local service healthy' }).waitFor();
  await page.locator('#metric-projects').filter({ hasText: '1' }).waitFor();
  await page.locator('#metric-targets').filter({ hasText: '2' }).waitFor();
  assert.equal(await page.locator('#metric-projects').innerText(), '1');
  assert.equal(await page.locator('#metric-targets').innerText(), '2');
  console.log('STANDALONE_BROWSER_SMOKE PASS project/invalid-target-guard/target/import/coverage/report/external-target/restart');
} finally {
  await browser.close();
}
