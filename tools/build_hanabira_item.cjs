const fs = require('fs');
const path = require('path');
const sharp = require('C:/Users/User/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');

async function build(root = path.resolve(__dirname, '..')) {
  const source = path.join(__dirname, 'assets/hanabira-fruit.png');
  const output = path.join(root, 'src/main/resources/assets/kazimod/textures/items/petal_petal_no_mi.png');
  fs.mkdirSync(path.dirname(output), { recursive: true });
  await sharp(source)
    .resize(64, 64, { fit: 'contain', kernel: 'nearest', background: '#00000000' })
    .png()
    .toFile(output);
  return output;
}

module.exports = { build };
if (require.main === module) {
  build(process.argv[2]).then(console.log).catch(error => {
    console.error(error);
    process.exitCode = 1;
  });
}
