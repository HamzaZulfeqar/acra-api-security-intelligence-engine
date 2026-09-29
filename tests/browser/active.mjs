import assert from 'node:assert/strict';
import { mkdir } from 'node:fs/promises';
import { chromium } from 'playwright';

const base = process.env.ACRA_UI_URL || 'http://127.0.0.1:8787/';
const host = process.env.ACRA_FIXTURE_HOST;
const port = process.env.ACRA_FIXTURE_PORT || '18081';
if (!host || !/^(?:\d{1,3}\.){3}\d{1,3}$/.test(host)) throw new Error('Explicit fixture IPv4 required');
const fixture = `http://${host}:${port}`;
const output = process.env.ACRA_BROWSER_OUTPUT || 'build/browser-active';
await mkdir(output, { recursive: true });

const browser = await chromium.launch({ headless: true });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(base, { waitUntil: 'domcontentloaded' });
  await page.locator('#health-label').filter({ hasText: 'Local service healthy' }).waitFor();
  await page.locator('#new-project-button').click();
  await page.locator('#project-form input[name="name"]').fill('Authorized GUI execution');
  await page.locator('#project-form textarea[name="description"]').fill('Isolated CI staging fixture');
  await page.locator('#project-form button[type="submit"]').click();
  await page.locator('#targets-view.active').waitFor();

  await page.locator('#target-form input[name="displayName"]').fill('Isolated staging fixture');
  await page.locator('#target-form select[name="hostScheme"]').selectOption('http');
  await page.locator('#target-form input[name="hostInput"]').fill(`${host}:${port}`);
  await page.locator('#target-form input[name="hostBasePath"]').fill('/api/v1');
  await page.locator('#target-form select[name="environment"]').selectOption('STAGING');
  await page.locator('#target-form select[name="testingMode"]').selectOption('SAFE_ACTIVE');
  await page.locator('#target-form input[name="authorizationReference"]').fill('CI-ROE-ISOLATED-FIXTURE');
  await page.locator('#target-form button[type="submit"]').click();
  await page.getByText('Authorized target registered. No scan was started.').waitFor();

  await page.locator('[data-view="inventory"]').click();
  await page.locator('#import-target option').filter({ hasText: 'Isolated staging fixture' }).waitFor({ state: 'attached' });
  await page.locator('#import-content').fill(JSON.stringify({
    openapi: '3.0.3', info: { title: 'Isolated CI fixture', version: '1' },
    paths: { '/demo': { get: { responses: { '200': { description: 'ok' } } } } },
  }));
  await page.locator('#import-form button[type="submit"]').click();
  await page.getByText(/OPENAPI import complete: 1 observation/).waitFor();

  await page.locator('[data-view="context"]').click();
  await page.locator('#principal-form input[name="principalId"]').fill('limited');
  await page.locator('#principal-form input[name="displayName"]').fill('Limited user');
  await page.locator('#principal-form button[type="submit"]').click();
  await page.locator('#context-principal-count').filter({ hasText: '1' }).waitFor();
  await page.locator('#role-form input[name="roleId"]').fill('viewer');
  await page.locator('#role-form input[name="name"]').fill('Viewer');
  await page.locator('#role-form button[type="submit"]').click();
  await page.locator('#context-role-count').filter({ hasText: '1' }).waitFor();
  await page.locator('#tenant-form input[name="tenantId"]').fill('tenant');
  await page.locator('#tenant-form input[name="name"]').fill('Tenant');
  await page.locator('#tenant-form button[type="submit"]').click();
  await page.locator('#context-tenant-count').filter({ hasText: '1' }).waitFor();
  await page.locator('#resource-form input[name="resourceId"]').fill('resource');
  await page.locator('#resource-form input[name="resourceType"]').fill('demo');
  await page.locator('#resource-owner').selectOption('limited');
  await page.locator('#resource-tenant').selectOption('tenant');
  await page.locator('#resource-form input[name="state"]').fill('ACTIVE');
  await page.locator('#resource-form button[type="submit"]').click();
  await page.locator('#context-resource-count').filter({ hasText: '1' }).waitFor();
  await page.locator('#expectation-target option').filter({ hasText: 'Isolated staging fixture' }).waitFor({ state: 'attached' });
  await page.locator('#expectation-endpoint').selectOption('/api/v1/demo');
  await page.locator('#expectation-principal').selectOption('limited');
  await page.locator('#expectation-role').selectOption('viewer');
  await page.locator('#expectation-tenant').selectOption('tenant');
  await page.locator('#expectation-resource').selectOption('resource');
  await page.locator('#expectation-form textarea[name="rationale"]').fill('Limited user denied at baseline');
  await page.locator('#expectation-form button[type="submit"]').click();
  await page.locator('#context-expectation-count').filter({ hasText: '1' }).waitFor();

  await page.locator('[data-view="active"]').click();
  await page.locator('#active-target option').filter({ hasText: 'Isolated staging fixture' }).waitFor({ state: 'attached' });
  await page.locator('#active-expectation option').filter({ hasText: 'limited' }).waitFor({ state: 'attached' });
  await page.locator('#active-path').fill('/api/v1/demo');
  await page.locator('#active-tested-auth').fill('Bearer limited-user');
  await page.locator('#active-positive-auth').fill('Bearer approved-control');
  await page.locator('#active-form input[name="confirmed"]').check();
  await page.locator('#active-form button[type="submit"]').click();
  await page.locator('#active-message').filter({ hasText: 'Controlled execution complete: expected DENY, observed ALLOW, differential UNEXPECTED_CHANGE.' }).waitFor();
  await page.locator('#active-execution-count').filter({ hasText: '1' }).waitFor();
  assert.match(await page.locator('#active-table-body').innerText(), /COMPLETED/);
  assert.equal(await page.locator('#active-tested-auth').inputValue(), '');
  assert.equal(await page.locator('#active-positive-auth').inputValue(), '');
  await page.screenshot({ path: `${output}/authorized-execution.png`, fullPage: true });

  const stats = await fetch(`${fixture}/__fixture_stats`).then(response => response.json());
  assert.deepEqual(stats.requests, [
    { path: '/api/v1/demo', identity: 'tested' },
    { path: '/api/v1/demo', identity: 'positive' },
    { path: '/api/v1/demo', identity: 'anonymous' },
    { path: '/api/v1/demo/', identity: 'tested' },
  ]);
  await page.reload({ waitUntil: 'domcontentloaded' });
  await page.locator('#health-label').filter({ hasText: 'Local service healthy' }).waitFor();
  await page.locator('#project-selector').selectOption({ label: 'Authorized GUI execution' });
  await page.locator('[data-view="active"]').click();
  await page.locator('#active-execution-count').filter({ hasText: '1' }).waitFor();
  assert.doesNotMatch(await page.locator('body').innerText(), /Bearer limited-user|Bearer approved-control/);
  console.log('STANDALONE_BROWSER_ACTIVE PASS GUI-to-authorized-staging/4-requests/result/reload/no-secret-render');
} finally {
  await browser.close();
}
