// Run from this directory. Renderer is a documentation tool, not an app dependency.
const fs = require('node:fs');
const path = require('node:path');
const { instance } = require('@viz-js/viz');
(async () => {
  const viz = await instance();
  const root = __dirname;
  fs.mkdirSync(path.join(root, 'svg'), { recursive: true });
  for (const name of fs.readdirSync(path.join(root, 'fontes')).filter(n => n.endsWith('.dot'))) {
    const source = fs.readFileSync(path.join(root, 'fontes', name), 'utf8');
    const result = viz.render(source, { engine: 'dot', format: 'svg' });
    if (result.status !== 'success') throw new Error(`${name}: ${JSON.stringify(result.errors)}`);
    fs.writeFileSync(path.join(root, 'svg', name.replace(/\.dot$/, '.svg')), result.output);
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
