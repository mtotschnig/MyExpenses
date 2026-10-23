#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import { existsSync, mkdtempSync, readFileSync, rmSync, statSync } from 'node:fs'
import { homedir, tmpdir } from 'node:os'
import { dirname, isAbsolute, join, resolve } from 'node:path'

/** The npm package that installs the `pdd-kit` command. */
const PACKAGE = 'pdd-toolkit'

/** Read `file` as JSON, or `undefined` when it is absent or unreadable. */
function readJson(file) {
  try {
    return JSON.parse(readFileSync(file, 'utf8'))
  } catch {
    return undefined
  }
}

/**
 * Pin the CLI to the version these skills were installed from.
 *
 * `pdd-toolkit@latest` is wrong here: the skills and the CLI are one release,
 * and a skill installed from version X must lint with version X. Resolving
 * `latest` at run time means a model authored against a newer format gets
 * linted by an older published CLI, which reports the new frontmatter keys as
 * unknown — a confusing failure with no obvious cause.
 *
 * `pdd-kit install` writes `.pdd-kit-install.json` beside the skill folders.
 * A Claude plugin installs the whole package instead, so the skill folder sits
 * at `<package>/skills/<name>/` and the package's own manifest names the
 * release.
 */
function installedPackageSpec() {
  const installation = readJson(new URL('../../.pdd-kit-install.json', import.meta.url))
  if (installation?.package === PACKAGE && typeof installation.version === 'string') {
    return `${PACKAGE}@${installation.version}`
  }
  const packageRoot = readJson(new URL('../../../package.json', import.meta.url))
  if (packageRoot?.name === PACKAGE && typeof packageRoot.version === 'string') {
    return `${PACKAGE}@${packageRoot.version}`
  }
  return `${PACKAGE}@latest`
}

/**
 * Run npm without a shell. Windows ships npm as a `.cmd` shim that Node
 * refuses to start without one, so run the npm bundled beside Node directly,
 * and fall back to the shim through `cmd.exe` with every argument quoted.
 */
function npmInvocation(npmArgs) {
  if (process.platform !== 'win32') return { file: 'npm', args: npmArgs, shell: false }
  const bundled = join(dirname(process.execPath), 'node_modules', 'npm', 'bin', 'npm-cli.js')
  if (existsSync(bundled)) {
    return { file: process.execPath, args: [bundled, ...npmArgs], shell: false }
  }
  return {
    file: 'npm.cmd',
    args: npmArgs.map(arg => `"${arg.replaceAll('"', '""')}"`),
    shell: true
  }
}

const NPM_MISSING = 'npm was not found on PATH. Install Node.js with npm ' +
  '(https://nodejs.org), or put npm on PATH, then run the lint again.'

/**
 * Maintainers can explicitly activate one local PDD worktree through
 * `~/.local/bin/pdd-kit-dev`. Resolve only that fixed development command, outside the
 * untrusted target repository; ordinary installations retain the pinned npm
 * fallback above.
 */
function activeDevelopmentCli(runnerRoot, env) {
  const binDirectory = resolve(env.PDD_KIT_DEV_BIN_DIR || join(homedir(), '.local', 'bin'))
  try {
    const cli = execFileSync(join(binDirectory, 'pdd-kit-dev'), ['--dev-cli'], {
      cwd: runnerRoot,
      env,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore']
    }).trim()
    if (!isAbsolute(cli) || !statSync(cli).isFile()) return undefined
    const packageRoot = dirname(dirname(cli))
    const manifest = JSON.parse(readFileSync(join(packageRoot, 'package.json'), 'utf8'))
    return manifest?.name === PACKAGE ? cli : undefined
  } catch {
    return undefined
  }
}

function fail(message) {
  process.stderr.write(`${message}\n`)
  process.exit(2)
}

const args = process.argv.slice(2)
const rootIndex = args.indexOf('--root')
const requestedRoot = rootIndex >= 0 ? args[rootIndex + 1] : undefined
if (!requestedRoot) {
  fail('Usage: run-pdd-kit.mjs --root <repository> lint [--json]')
}

const commandArgs = args.filter(
  (_, index) => index !== rootIndex && index !== rootIndex + 1
)
if (commandArgs[0] !== 'lint') {
  fail('The isolated pdd-kit runner supports only lint.')
}

let root
try {
  root = resolve(execFileSync(
    'git',
    ['-C', resolve(requestedRoot), 'rev-parse', '--show-toplevel'],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }
  ).trim())
} catch {
  fail('The pdd-kit runner must target a Git repository.')
}

const runnerRoot = mkdtempSync(join(tmpdir(), 'pdd-kit-cli-'))
const env = { ...process.env }

try {
  const developmentCli = activeDevelopmentCli(runnerRoot, env)
  if (developmentCli) {
    execFileSync(process.execPath, [developmentCli, '--cwd', root, ...commandArgs], {
      cwd: runnerRoot,
      env,
      stdio: 'inherit'
    })
  } else {
    const npm = npmInvocation([
      'exec',
      '--yes',
      '--ignore-scripts',
      `--package=${installedPackageSpec()}`,
      '--',
      'pdd-kit',
      '--cwd',
      root,
      ...commandArgs
    ])
    execFileSync(npm.file, npm.args, {
      cwd: runnerRoot,
      env,
      shell: npm.shell,
      stdio: 'inherit'
    })
  }
} catch (error) {
  // cmd.exe reports an unknown command as exit status 9009.
  if (error?.code === 'ENOENT' || (process.platform === 'win32' && error?.status === 9009)) {
    process.stderr.write(`${NPM_MISSING}\n`)
    process.exitCode = 127
  } else {
    process.exitCode = typeof error.status === 'number' ? error.status : 1
  }
} finally {
  rmSync(runnerRoot, { recursive: true, force: true })
}
