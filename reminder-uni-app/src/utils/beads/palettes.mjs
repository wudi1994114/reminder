import mard from './palette-data/mard-221.json' with { type: 'json' };
import perler from './palette-data/perler-midi.json' with { type: 'json' };
import artkal from './palette-data/artkal-s.json' with { type: 'json' };
import hama from './palette-data/hama-midi.json' with { type: 'json' };

/** Vendored data only: no requests or third-party runtime code. Show credit next to the palette selector. */
export const BEAD_PALETTES = [mard, perler, artkal, hama].map(data => ({
  id: data.id,
  name: `${data.brand} · ${data.colors.length} 色`,
  colors: data.colors.map(({code,name,hex,rgb}) => ({code,name,hex,rgb:[...rgb]})),
  credit: `Color data: craft-color-codes by MakeBead (https://makebead.com) · ${data.id === 'mard-221' ? 'CC BY 4.0' : 'maxcleme/beadcolors · MIT'}`,
}));
