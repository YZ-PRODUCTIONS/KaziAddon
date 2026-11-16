// Hand-designed pixel materials for the eight independent Dragon Strike summons.
// One shared UV layout keeps all editable Blockbench models and runtime meshes aligned.
// These are clustered texture strokes, not photographs or randomized noise fields.
'use strict';

const materials = Object.freeze([
    'scales', 'belly', 'horn', 'armor', 'crest', 'eye', 'teeth', 'mouth',
    'tongue', 'joint', 'plume', 'vein', 'iris', 'pupil', 'glow', 'trim'
]);
const SIZE = 256;
const TILE = 64;
const LOGICAL = 32;

const rgb = hex => hex.match(/../g).map(v => parseInt(v, 16));
const hex = values => values.map(v => Math.round(v).toString(16).padStart(2, '0')).join('');
const blend = (a, b, t) => a.map((v, i) => Math.round(v + (b[i] - v) * t));
const mixed = (a, b, t) => hex(blend(rgb(a), rgb(b), t));
const clamp = (n, a = 0, b = 1) => Math.max(a, Math.min(b, n));
const step = (n, levels = 5) => Math.round(clamp(n) * levels) / levels;

function palette(c) {
    const values = {
        scales: c.scales,
        belly: c.belly,
        horn: c.horn,
        armor: c.armor,
        crest: c.crest,
        // Eyes are deliberately blank sclera; separate mesh pieces carry iris/pupil.
        eye: c.eye,
        teeth: c.teeth || ['6f6a76', 'dedbcf', 'fff8e7'],
        mouth: c.mouth,
        tongue: c.tongue || ['4a2437', '905064', 'c17b85'],
        joint: [c.scales[0], mixed(c.scales[0], c.scales[1], .34), mixed(c.scales[0], c.scales[1], .68)],
        plume: c.plume,
        vein: c.vein,
        iris: c.iris,
        pupil: c.pupil || ['070a14', '101321', '252738'],
        glow: c.glow,
        trim: c.trim
    };
    return Object.freeze(Object.fromEntries(materials.map(name => [name, Object.freeze(values[name].slice())])));
}

const palettes = Object.freeze({
    first: palette({
        scales: ['7f7b99', 'cac7d8', 'f5f1ed'],
        belly: ['353146', '655d7b', 'a49cb4'],
        horn: ['75687e', 'c8c2d3', 'fff7e9'],
        armor: ['79728f', 'c9c5d9', 'faf3ed'],
        crest: ['65627e', 'b6b8d0', 'f5f1ec'],
        eye: ['b5a4a1', 'f2e7cc', 'fffce2'],
        mouth: ['100e20', '21172e', '35213b'],
        plume: ['9c93a9', 'd7d0df', 'fff8e9'],
        vein: ['7666b4', 'b7a0eb', 'e5d8ff'],
        iris: ['9b5a22', 'e4b35a', 'fff3a1'],
        glow: ['b5a1ea', 'ded0ff', 'fff4e8'],
        trim: ['7e7195', 'b1a8cd', 'e9ddeb']
    }),
    blind: palette({
        scales: ['151b2b', '364156', '73849b'],
        belly: ['171829', '38374f', '6d6986'],
        horn: ['27334a', '637c96', 'adbed0'],
        armor: ['101827', '29384c', '71849d'],
        crest: ['1d243a', '465479', '9197c5'],
        eye: ['4c4770', '9891bf', 'dad4ee'],
        mouth: ['090b13', '111323', '201c36'],
        tongue: ['1b162b', '483052', '806580'],
        teeth: ['616c82', 'b4c1d0', 'e4e9e3'],
        plume: ['242b42', '536184', '9ba9c3'],
        vein: ['353875', '797bd0', 'b6c9ff'],
        iris: ['4c4685', 'a298db', 'e4dcff'],
        glow: ['414c9d', '8b9bea', 'd4e5ff'],
        trim: ['313b59', '687798', 'aebad0']
    }),
    cobra: palette({
        scales: ['2c5669', '60a1ac', 'b5dedb'],
        belly: ['717e9c', 'b2c4d1', 'e8ecdf'],
        horn: ['607c93', 'a5cbd4', 'e1f4e9'],
        armor: ['25475e', '528a9d', 'acd4d4'],
        crest: ['476d82', '91bfc7', 'd0eee4'],
        eye: ['acc6bd', 'e4ead5', 'fff8de'],
        mouth: ['101c2a', '24273c', '3e344b'],
        tongue: ['62263f', 'b04a67', 'e18b99'],
        plume: ['438594', '8dc4c6', 'd4eee4'],
        vein: ['275267', '4aaec4', 'b4f0ee'],
        iris: ['ab552b', 'e9a84e', 'fff1a2'],
        glow: ['42b3c4', '89e1e6', 'e1ffed'],
        trim: ['5e293e', 'b45168', 'e39aa1']
    }),
    four_eyed: palette({
        scales: ['455b30', '8aa14f', 'd1de87'],
        belly: ['827342', 'beb575', 'ebe3ab'],
        horn: ['6b7050', 'bac391', 'f0ebba'],
        armor: ['40572e', '8b9d50', 'ccd78b'],
        crest: ['66733b', 'b4bf66', 'e9e99e'],
        eye: ['b6b085', 'eee7bd', 'fffadc'],
        mouth: ['171b15', '2d3020', '4b4427'],
        tongue: ['4c342b', '8a6752', 'ba9675'],
        plume: ['7b844a', 'c3c77b', 'efecba'],
        vein: ['6c7f37', 'b4d062', 'e7f7a6'],
        iris: ['7b4229', 'd2893d', 'ffdd82'],
        glow: ['b4bd4f', 'e5e981', 'ffffc8'],
        trim: ['6c6339', 'b6a866', 'ede0a2']
    }),
    corpse_flame: palette({
        scales: ['6d263b', 'c54c43', 'f58b56'],
        belly: ['4e1e31', '963044', 'd76250'],
        horn: ['803443', 'df7252', 'ffd48c'],
        armor: ['5b2032', 'ab3e43', 'ec7955'],
        crest: ['a03236', 'ef743e', 'ffd97b'],
        eye: ['de8247', 'ffd372', 'fff1ad'],
        mouth: ['210f1c', '3b1425', '5d2030'],
        tongue: ['81213d', 'c74552', 'f8937a'],
        teeth: ['92513e', 'e3bd86', 'fff1bb'],
        plume: ['af3433', 'ed7a3f', 'ffde87'],
        vein: ['b83b35', 'ff963e', 'ffe99a'],
        iris: ['d56a2f', 'ffcd55', 'fffbd2'],
        pupil: ['140b15', '2b1021', '4e152b'],
        glow: ['ee6f36', 'ffc653', 'fff4bc'],
        trim: ['8e3041', 'd75f43', 'ffb960']
    }),
    ice_crystal: palette({
        scales: ['5f8da8', 'b4dce4', 'edf9f5'],
        belly: ['3a638b', '77a8c7', 'c6e5e9'],
        horn: ['688db2', 'bddae9', 'f5fbf1'],
        armor: ['527b9e', 'aacddc', 'eff9f2'],
        crest: ['739bbd', 'c8e4ee', 'fbfff5'],
        eye: ['accbdb', 'dff1ec', 'faffec'],
        mouth: ['14253e', '283853', '46516e'],
        tongue: ['4a5574', '829ab3', 'b3cbd3'],
        teeth: ['8eaaba', 'd5e7e7', 'fffdf1'],
        plume: ['799db9', 'bddbe5', 'f1fbf3'],
        vein: ['559ac2', 'a1e0f3', 'e5ffff'],
        iris: ['397ba5', '71c1dc', 'd5f7fa'],
        glow: ['70c1e4', 'b6ecf6', 'f6fff9'],
        trim: ['658cae', 'b6d6e4', 'f1faf5']
    }),
    spearhead: palette({
        scales: ['101924', '2b3e50', '617a8c'],
        belly: ['1a2332', '405168', '8996ae'],
        horn: ['405570', '91a9bd', 'd9e4e8'],
        armor: ['111923', '2b3b4c', '8396aa'],
        crest: ['233347', '536d86', 'a8bfce'],
        eye: ['b2a862', 'ece2a3', 'fff8cb'],
        mouth: ['080d16', '111b27', '243346'],
        tongue: ['28354a', '4a5e78', '8492aa'],
        teeth: ['688498', 'b6cbd5', 'e7eee6'],
        plume: ['293f56', '607e98', 'adc5d3'],
        vein: ['44849d', '78c7db', 'c5f5fc'],
        iris: ['ad8626', 'e9d34f', 'ffffa3'],
        glow: ['58abd3', 'a0e4f6', 'efffff'],
        trim: ['50667b', '93adbe', 'd6e3e6']
    }),
    angel: palette({
        scales: ['a89b7e', 'e0d7b8', 'fff8de'],
        belly: ['a49981', 'ded5bc', 'fff9e7'],
        horn: ['968151', 'dac087', 'fff1c0'],
        armor: ['a89771', 'e3d3aa', 'fff5d3'],
        crest: ['baaa7c', 'eddfb5', 'fffbe2'],
        eye: ['c6af71', 'f2df9a', 'fff8d3'],
        mouth: ['2b2330', '443342', '66505a'],
        tongue: ['714d55', 'b38184', 'dfafa1'],
        teeth: ['aaa186', 'ebe3cb', 'fff9e6'],
        plume: ['a9a08d', 'e6dfca', 'fff9e6'],
        vein: ['c4a45d', 'eed18a', 'fff0ba'],
        iris: ['937039', 'd9b35f', 'ffeda0'],
        glow: ['e4c579', 'ffe6ac', 'ffffe5'],
        trim: ['9f7c3e', 'd2ac61', 'ffdfa0']
    })
});

function painter(pixels, index) {
    const ox = index % 4 * TILE;
    const oy = Math.floor(index / 4) * TILE;
    function dot(x, y, color) {
        x = Math.round(x);
        y = Math.round(y);
        if (x < 0 || y < 0 || x >= LOGICAL || y >= LOGICAL) return;
        const c = Array.isArray(color) ? color : rgb(color);
        for (let dy = 0; dy < 2; dy++) for (let dx = 0; dx < 2; dx++) {
            const offset = ((oy + y * 2 + dy) * SIZE + ox + x * 2 + dx) * 4;
            pixels[offset] = c[0];
            pixels[offset + 1] = c[1];
            pixels[offset + 2] = c[2];
            pixels[offset + 3] = 255;
        }
    }
    function rect(x, y, w, h, c) {
        for (let yy = y; yy < y + h; yy++) for (let xx = x; xx < x + w; xx++) dot(xx, yy, c);
    }
    function line(x, y, xx, yy, c, width = 1) {
        const length = Math.max(Math.abs(xx - x), Math.abs(yy - y), 1);
        for (let i = 0; i <= length; i++) rect(Math.round(x + (xx - x) * i / length), Math.round(y + (yy - y) * i / length), width, width, c);
    }
    function path(points, c, width = 1) {
        for (let i = 1; i < points.length; i++) line(...points[i - 1], ...points[i], c, width);
    }
    function poly(points, c) {
        for (let y = 0; y < LOGICAL; y++) for (let x = 0; x < LOGICAL; x++) {
            let inside = false;
            for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
                const a = points[i], b = points[j];
                if ((a[1] > y + .5) !== (b[1] > y + .5)
                    && x + .5 < (b[0] - a[0]) * (y + .5 - a[1]) / (b[1] - a[1]) + a[0]) inside = !inside;
            }
            if (inside) dot(x, y, c);
        }
    }
    return { dot, rect, line, path, poly };
}

function bodyScales(p, id, colors) {
    const [lo, mid, hi] = colors;
    const seam = blend(lo, mid, .34);
    const lip = blend(mid, hi, .47);
    // Broad overlapping hexagonal plates. Their lower seams are darker than the
    // upper shoulders; staggered rows continue cleanly across the tile borders.
    for (let row = -1; row < 5; row++) {
        const y = row * 8;
        for (let col = -1; col < 5; col++) {
            const x = col * 8 + ((row & 1) ? 4 : 0);
            const plate = [[x - 4, y - 1], [x, y - 3], [x + 4, y - 1], [x + 4, y + 3], [x, y + 6], [x - 4, y + 3]];
            p.poly(plate, blend(mid, hi, (row + col) % 3 === 0 ? .13 : .03));
            p.poly([[x - 3, y], [x, y - 2], [x + 2, y], [x + 1, y + 3], [x - 1, y + 4], [x - 3, y + 2]], blend(mid, hi, .24));
            p.path([[x - 4, y], [x - 4, y + 3], [x, y + 6], [x + 4, y + 3], [x + 4, y]], seam);
            p.path([[x - 3, y], [x, y - 2], [x + 2, y - 1]], lip);
            p.path([[x + 2, y + 3], [x, y + 4]], blend(mid, lo, .13));
            if (id === 'ice_crystal') {
                p.path([[x, y - 1], [x - 1, y + 1], [x, y + 3]], blend(mid, hi, .62));
            } else if (id === 'corpse_flame') {
                p.path([[x - 4, y + 2], [x, y + 5], [x + 2, y + 4]], blend(mid, hi, .32));
            }
        }
    }
}

function bellyPlates(p, colors) {
    const [lo, mid, hi] = colors;
    // Continuous ventral scutes: a gently convex plate, not a checkerboard.
    for (let row = -1; row < 5; row++) {
        const y = row * 8;
        p.poly([[0, y], [8, y + 1], [16, y + 2], [24, y + 1], [32, y], [32, y + 8], [24, y + 9], [16, y + 10], [8, y + 9], [0, y + 8]], mid);
        p.poly([[3, y + 2], [12, y + 3], [22, y + 3], [29, y + 2], [28, y + 5], [16, y + 7], [4, y + 5]], blend(mid, hi, .24));
        p.path([[0, y + 7], [8, y + 8], [16, y + 9], [24, y + 8], [31, y + 7]], blend(lo, mid, .28));
        p.path([[1, y + 1], [9, y + 2], [16, y + 3], [23, y + 2], [30, y + 1]], blend(mid, hi, .58));
        p.line(2, y + 3, 2, y + 5, blend(mid, lo, .20));
        p.line(29, y + 3, 29, y + 5, blend(mid, lo, .28));
    }
}

function hornGrain(p, colors, crystal = false) {
    const [lo, mid, hi] = colors;
    // Longitudinal keratin strokes follow the horn's mesh instead of horizontal
    // stripes that would make the cones look like stacked cubes.
    for (const [x, length, shift] of [[3, 26, 2], [9, 21, 1], [17, 28, -2], [24, 18, -1], [29, 25, -2]]) {
        p.path([[x, 0], [x, 8], [x + shift, length]], blend(lo, mid, crystal ? .48 : .68));
        p.path([[x + 1, 1], [x + 1, 8], [x + 1 + shift, length - 4]], blend(mid, hi, crystal ? .72 : .49));
    }
    if (crystal) {
        p.poly([[10, 0], [14, 0], [10, 31], [7, 31]], blend(mid, hi, .72));
        p.poly([[24, 0], [27, 0], [20, 31], [17, 31]], blend(mid, lo, .32));
    } else {
        p.path([[0, 27], [9, 29], [20, 28], [31, 26]], blend(mid, lo, .23));
        p.path([[1, 26], [9, 28], [20, 27], [30, 25]], blend(mid, hi, .18));
    }
}

function armorPlates(p, colors, id) {
    const [lo, mid, hi] = colors;
    // Chamfer highlights define one large sculpted plate when mapped onto heads.
    p.poly([[1, 4], [7, 1], [24, 1], [31, 6], [29, 24], [22, 31], [8, 29], [1, 23]], blend(mid, hi, .08));
    p.poly([[4, 5], [10, 2], [22, 3], [26, 6], [22, 11], [8, 13], [3, 11]], blend(mid, hi, .28));
    p.poly([[26, 5], [30, 7], [28, 24], [22, 30], [19, 27], [24, 20]], blend(mid, lo, .38));
    p.path([[1, 5], [7, 1], [24, 1], [30, 6]], blend(mid, hi, .58));
    p.path([[1, 22], [8, 28], [22, 30], [28, 24]], blend(lo, mid, .35));
    p.path([[3, 22], [9, 26], [21, 28], [26, 23]], blend(mid, hi, .20));
    if (id === 'spearhead') {
        p.path([[3, 7], [12, 6], [19, 11], [27, 9]], blend(mid, hi, .58));
        p.path([[11, 7], [17, 13], [17, 23]], blend(lo, mid, .34));
        p.path([[13, 7], [19, 13], [19, 21]], blend(mid, hi, .22));
    } else if (id === 'first' || id === 'ice_crystal') {
        p.path([[14, 4], [18, 10], [15, 18], [19, 24]], blend(mid, lo, .24));
        p.path([[19, 10], [26, 15], [25, 20]], blend(mid, lo, .21));
        p.line(14, 4, 17, 9, blend(mid, hi, .46));
    } else if (id === 'cobra') {
        p.path([[5, 20], [9, 16], [10, 8]], blend(mid, lo, .23));
        p.path([[26, 20], [22, 16], [21, 8]], blend(mid, lo, .23));
    }
}

function crestStroke(p, colors, id) {
    const [lo, mid, hi] = colors;
    if (id === 'corpse_flame') {
        p.poly([[2, 31], [4, 18], [9, 22], [11, 10], [14, 15], [17, 0], [21, 12], [23, 9], [25, 22], [30, 17], [31, 31]], blend(mid, hi, .39));
        p.poly([[8, 31], [11, 22], [14, 26], [17, 10], [21, 24], [24, 20], [27, 31]], blend(mid, hi, .85));
        p.path([[4, 29], [6, 23], [8, 25], [12, 15]], blend(mid, lo, .20));
        p.path([[22, 28], [21, 23], [19, 24], [17, 17]], hi);
    } else {
        for (const x of [1, 9, 17, 25]) {
            p.path([[x + 2, 31], [x + 2, 22], [x + 3, 13], [x + 6, 0]], blend(lo, mid, .54));
            p.path([[x + 3, 31], [x + 3, 22], [x + 4, 13], [x + 7, 0]], blend(mid, hi, .60));
            p.path([[x + 5, 29], [x + 5, 22], [x + 6, 17]], blend(mid, hi, .20));
        }
    }
}

function featherBarbs(p, colors) {
    const [lo, mid, hi] = colors;
    // A readable central rachis and staggered paired barbs. The very subtle
    // dark seams stay ivory at a distance, rather than turning feather fans gray.
    p.poly([[14, 0], [17, 0], [17, 32], [15, 32]], blend(mid, hi, .63));
    p.line(18, 0, 18, 31, blend(mid, lo, .22));
    for (let row = -1; row < 8; row++) {
        const y = row * 5;
        p.path([[14, y + 6], [9, y + 3], [2, y - 1]], blend(mid, lo, .29));
        p.path([[14, y + 4], [9, y + 1], [2, y - 3]], blend(mid, hi, .40));
        p.path([[19, y + 6], [24, y + 3], [31, y - 1]], blend(mid, lo, .32));
        p.path([[19, y + 4], [24, y + 1], [31, y - 3]], blend(mid, hi, .30));
    }
    p.path([[1, 0], [3, 10], [1, 19], [3, 30]], blend(mid, lo, .18));
    p.path([[30, 0], [28, 9], [30, 19], [28, 31]], blend(mid, lo, .20));
}

function emissiveVeins(p, colors, id) {
    const [lo, mid, hi] = colors;
    p.rect(0, 0, 32, 32, blend(lo, mid, .12));
    const tracks = id === 'corpse_flame'
        ? [[[5, 31], [7, 23], [4, 17], [9, 10], [8, 0]], [[21, 31], [18, 22], [23, 14], [20, 7], [23, 0]]]
        : [[[4, 0], [9, 7], [8, 14], [14, 20], [12, 31]], [[25, 0], [21, 10], [24, 17], [21, 25], [27, 31]]];
    for (const points of tracks) {
        p.path(points, blend(mid, lo, .20), 3);
        p.path(points, mid, 2);
        p.path(points.map(([x, y]) => [x + 1, y]), blend(mid, hi, .68));
    }
    p.path([[9, 11], [15, 8], [17, 2]], blend(mid, hi, .36));
    p.path([[23, 18], [29, 16], [31, 10]], blend(mid, hi, .31));
}

function irisRays(p, colors) {
    const [lo, mid, hi] = colors;
    // No baked pupil or white glint: the model's separately inset pupil controls
    // the eye silhouette. This tile remains a colored iris even on narrow eyes.
    for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
        const dx = x - 15.5, dy = y - 15.5;
        const a = Math.atan2(dy, dx), radius = Math.hypot(dx, dy) / 22;
        const spoke = step((Math.cos(a * 18 + radius * 2) + 1) / 2, 3);
        let c = blend(mid, lo, step(radius * .38 + spoke * .18));
        c = blend(c, hi, step((1 - radius) * .31 + (1 - spoke) * .16));
        p.dot(x, y, c);
    }
}

function trimStroke(p, colors, id) {
    const [lo, mid, hi] = colors;
    p.line(1, 0, 1, 31, blend(lo, mid, .34));
    p.line(3, 0, 3, 31, blend(mid, hi, .56));
    p.line(28, 0, 28, 31, blend(mid, hi, .24));
    p.line(30, 0, 30, 31, blend(lo, mid, .37));
    if (id === 'angel') {
        for (let y = -4; y < 32; y += 12) {
            p.path([[8, y], [16, y + 5], [24, y], [16, y + 10], [8, y]], blend(mid, lo, .28));
            p.path([[9, y], [16, y + 4], [23, y]], blend(mid, hi, .61));
        }
    } else if (id === 'cobra') {
        for (let y = 4; y < 32; y += 12) {
            p.path([[5, y], [15, y + 3], [25, y]], blend(mid, hi, .36));
            p.path([[5, y + 1], [15, y + 4], [25, y + 1]], blend(mid, lo, .21));
        }
    } else {
        for (const y of [8, 23]) {
            p.path([[6, y], [13, y + 2], [23, y]], blend(mid, lo, .19));
            p.line(7, y - 1, 12, y, blend(mid, hi, .26));
        }
    }
}

async function atlas(id) {
    if (!Object.prototype.hasOwnProperty.call(palettes, id)) throw new RangeError('Unknown Dragon Strike material palette: ' + id);
    const sharp = require(process.env.SHARP_MODULE || 'C:/Users/th3wyz/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
    const pixels = Buffer.alloc(SIZE * SIZE * 4);
    for (let index = 0; index < materials.length; index++) {
        const name = materials[index], p = painter(pixels, index);
        const colors = palettes[id][name].map(rgb), [lo, mid, hi] = colors;
        for (let y = 0; y < LOGICAL; y++) for (let x = 0; x < LOGICAL; x++) {
            // Broad stepped brush patches create form without adding UV-baked
            // strong lighting; all eight creatures remain readable in daylight.
            const bevel = clamp((Math.abs(x - 14.5) - 9) / 8) * .32;
            const dark = bevel + clamp((y - 21) / 13) * .14;
            const shine = clamp(1 - Math.abs(x - 11) / 13) * clamp(1 - Math.abs(y - 9) / 26) * .31;
            let color = blend(blend(mid, lo, step(dark)), hi, step(shine));
            if (name === 'mouth' || name === 'pupil') color = blend(mid, lo, step(clamp((y - 10) / 22) * .22));
            if (name === 'eye') color = blend(mid, hi, step(clamp(1 - Math.hypot(x - 11, y - 10) / 31) * .38));
            if (name === 'glow') color = blend(mid, hi, step(clamp(1 - Math.hypot(x - 14, y - 12) / 26) * .56));
            p.dot(x, y, color);
        }
        if (name === 'scales') bodyScales(p, id, colors);
        if (name === 'belly') bellyPlates(p, colors);
        if (name === 'horn') hornGrain(p, colors, id === 'ice_crystal');
        if (name === 'armor') armorPlates(p, colors, id);
        if (name === 'crest') crestStroke(p, colors, id);
        if (name === 'teeth') {
            p.line(6, 3, 8, 25, blend(mid, hi, .66));
            p.line(24, 5, 23, 29, blend(mid, lo, .25));
            p.line(12, 2, 13, 12, blend(mid, hi, .35));
        }
        if (name === 'tongue') {
            p.path([[15, 0], [15, 12], [17, 22], [17, 31]], blend(mid, lo, .20));
            p.path([[13, 0], [13, 12], [15, 22], [15, 31]], blend(mid, hi, .21));
        }
        if (name === 'joint') {
            for (let y = 0; y < 32; y += 8) {
                p.path([[0, y], [9, y + 2], [22, y + 2], [31, y]], blend(mid, lo, .33));
                p.path([[1, y + 2], [9, y + 4], [22, y + 4], [30, y + 2]], blend(mid, hi, .24));
            }
        }
        if (name === 'plume') featherBarbs(p, colors);
        if (name === 'vein') emissiveVeins(p, colors, id);
        if (name === 'iris') irisRays(p, colors);
        if (name === 'trim') trimStroke(p, colors, id);
    }
    return sharp(pixels, { raw: { width: SIZE, height: SIZE, channels: 4 } }).png({ compressionLevel: 9 }).toBuffer();
}

module.exports = { materials, palettes, atlas };
