import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const sourceRoot = path.resolve(process.cwd(), 'src');
const blockedPatterns = [
  { label: 'WeChat cloud runtime', pattern: /\bwx\.cloud\b/ },
  { label: 'Cloud Hosting request helper', pattern: /\bcallContainer\b/ },
  { label: 'Cloud Hosting gateway header', pattern: /X-WX-/ },
  { label: 'WeChat Cloud Storage URL', pattern: /cloud:\/\// },
  { label: 'Cloud Hosting login route', pattern: /\/cloud-login\b/ },
  { label: 'CloudBase configuration import', pattern: /config\/cloud(?:\.js)?/ },
];

async function listSourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nestedFiles = await Promise.all(entries.map(async (entry) => {
    const absolutePath = path.join(directory, entry.name);
    return entry.isDirectory() ? listSourceFiles(absolutePath) : [absolutePath];
  }));
  return nestedFiles.flat();
}

const violations = [];
for (const filePath of await listSourceFiles(sourceRoot)) {
  const content = await readFile(filePath, 'utf8');
  const relativePath = path.relative(process.cwd(), filePath);
  const lines = content.split(/\r?\n/);

  lines.forEach((line, index) => {
    blockedPatterns.forEach(({ label, pattern }) => {
      if (pattern.test(line)) {
        violations.push(`${relativePath}:${index + 1} ${label}`);
      }
    });
  });
}

if (violations.length > 0) {
  console.error('CloudBase runtime references remain:');
  violations.forEach((violation) => console.error(`- ${violation}`));
  process.exit(1);
}

console.log('No CloudBase runtime references found in src.');
