#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import { extname, resolve } from 'node:path'

function fail(message) {
  process.stderr.write(`${message}\n`)
  process.exit(1)
}

function git(root, ...args) {
  return execFileSync('git', args, {
    cwd: root,
    encoding: 'utf8',
    // A large repository lists more paths than the default 1 MiB buffer holds.
    maxBuffer: 512 * 1024 * 1024,
    stdio: ['ignore', 'pipe', 'pipe']
  })
}

function matches(files, pattern, limit = 80) {
  const matched = files.filter(file => pattern.test(file)).sort()
  return { files: matched.slice(0, limit), omitted: Math.max(0, matched.length - limit) }
}

const args = process.argv.slice(2)
const rootIndex = args.indexOf('--root')
const requestedRoot = rootIndex >= 0 ? args[rootIndex + 1] : process.cwd()
if (!requestedRoot) fail('--root requires a directory')
if (args.some(arg => arg !== '--root' && arg !== requestedRoot)) {
  fail('Usage: inventory-repository.mjs --root <repository>')
}

let root
try {
  root = resolve(git(resolve(requestedRoot), 'rev-parse', '--show-toplevel').trim())
} catch {
  fail('pdd-kit inventory must run inside a Git repository.')
}

const files = git(root, 'ls-files', '-z').split('\0').filter(Boolean)
const extensions = {}
const topLevel = {}
for (const file of files) {
  const extension = extname(file).toLowerCase() || '[none]'
  extensions[extension] = (extensions[extension] || 0) + 1
  const area = file.includes('/') ? file.split('/')[0] : '[root]'
  topLevel[area] = (topLevel[area] || 0) + 1
}

const inventory = {
  repositoryRoot: root,
  trackedFileCount: files.length,
  extensions: Object.fromEntries(Object.entries(extensions).sort((left, right) => right[1] - left[1])),
  topLevelAreas: Object.fromEntries(Object.entries(topLevel).sort((left, right) => right[1] - left[1])),
  candidates: {
    instructions: matches(files, /(^|\/)(AGENTS|CLAUDE|CONTRIBUTING|README|SECURITY)(\.|$)/i),
    documentation: matches(files, /(^|\/)(docs?|documentation)\//i),
    entryPoints: matches(files, /(routes?|router|controllers?|handlers?|commands?|cmd|api|graphql|http|ssh|web)\b/i),
    processes: matches(files, /(queues?|workers?|jobs?|tasks?|schedulers?|cron|webhooks?|indexers?|services?|daemons?)\b/i),
    persistence: matches(files, /(models?|entities|schemas?|migrations?|database|storage|repositories)\b/i),
    observability: matches(files, /(metrics?|telemetry|tracing|logging|audit|monitoring|prometheus|opentelemetry)\b/i),
    configuration: matches(files, /(^|\/)(config|configs|configuration|settings|\.env|docker|helm|charts?)\b/i),
    tests: matches(files, /(^|\/)(tests?|specs?|e2e|fixtures?)\//i)
  }
}

process.stdout.write(`${JSON.stringify(inventory, null, 2)}\n`)
