// Storage Plus — tiered big chests + drawers with stack-limit upgrades.
// Script API: @minecraft/server 2.0.0 (Minecraft Bedrock 1.21.90+)
import { world, system, ItemStack } from "@minecraft/server";

// ============================================================ config
// Keep in sync with tools/generate.py
const PAGE_SIZE = 27;
const VAULT_ID = "sp:chest_vault";
const CHEST_FAMILY = "sp_chest";
const CHESTS = {
  "sp:iron_chest_entity": { item: "sp:iron_chest", pages: 2, label: "Iron Chest" },
  "sp:gold_chest_entity": { item: "sp:gold_chest", pages: 3, label: "Gold Chest" },
  "sp:diamond_chest_entity": { item: "sp:diamond_chest", pages: 5, label: "Diamond Chest" },
  "sp:netherite_chest_entity": { item: "sp:netherite_chest", pages: 8, label: "Netherite Chest" },
};
const CHEST_BY_ITEM = Object.fromEntries(Object.entries(CHESTS).map(([id, c]) => [c.item, id]));
const BREAK_HITS = 3;

// stacks = how many full stacks each slot holds before upgrades
const DRAWERS = {
  "sp:drawer_1": { slots: 1, stacks: 32 },
  "sp:drawer_2": { slots: 2, stacks: 16 },
  "sp:drawer_4": { slots: 4, stacks: 8 },
};
const UPGRADES = [
  { item: undefined, mult: 1 },
  { item: "sp:upgrade_iron", mult: 4 },
  { item: "sp:upgrade_gold", mult: 16 },
  { item: "sp:upgrade_diamond", mult: 64 },
  { item: "sp:upgrade_netherite", mult: 256 },
];
const UPGRADE_BY_ITEM = Object.fromEntries(UPGRADES.map((u, lvl) => [u.item, lvl]).filter(([id]) => id));
// A drawer holding more than this many stacks must be emptied before it can be broken,
// so breaking it never spawns thousands of item entities.
const MAX_BREAK_STACKS = 64;
const DOUBLE_TAP_TICKS = 10;

// ============================================================ helpers
const lower = (v) => String(v ?? "").toLowerCase();
const isCreative = (p) => lower(p.getGameMode()) === "creative";
const isValid = (e) => !!e && (typeof e.isValid === "function" ? e.isValid() : e.isValid);
const invOf = (e) => e.getComponent("minecraft:inventory")?.container;
const fmt = (n) => String(Math.floor(n)).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
const tr = (key, ...args) => (args.length ? { translate: key, with: args.map(String) } : { translate: key });

const quietUntil = new Map(); // player.id -> tick; pauses the look-at info after feedback
function feedback(player, ...parts) {
  try {
    player.onScreenDisplay.setActionBar({ rawtext: parts });
    quietUntil.set(player.id, system.currentTick + 40);
  } catch {}
}
function sound(dim, id, location) {
  try {
    dim.playSound(id, location);
  } catch {}
}

const cooldowns = new Map();
function cooldown(key, ticks) {
  const now = system.currentTick;
  if ((cooldowns.get(key) ?? -1000) + ticks > now) return false;
  cooldowns.set(key, now);
  return true;
}

const itemInfoCache = new Map();
function itemInfo(id) {
  let info = itemInfoCache.get(id);
  if (info) return info;
  try {
    const proto = new ItemStack(id, 1);
    info = { max: proto.maxAmount, key: proto.localizationKey, proto };
    itemInfoCache.set(id, info);
  } catch {
    info = { max: 64, key: undefined, proto: undefined }; // not cached: may work later
  }
  return info;
}
function itemName(id) {
  const key = itemInfo(id).key;
  return key ? { translate: key } : { text: id.replace(/^minecraft:/, "").replace(/_/g, " ") };
}

function consumeHeld(player, typeId) {
  if (isCreative(player)) return;
  const inv = invOf(player);
  const slot = player.selectedSlotIndex;
  const item = inv?.getItem(slot);
  if (!item || item.typeId !== typeId) return;
  if (item.amount > 1) {
    item.amount -= 1;
    inv.setItem(slot, item);
  } else {
    inv.setItem(slot);
  }
}

function give(player, stack) {
  const left = invOf(player)?.addItem(stack);
  if (left) player.dimension.spawnItem(left, player.location);
}

// ============================================================ big chests
// The chest is an entity whose 27-slot inventory is the page you see.
// The other pages live in an invisible "vault" entity at the same spot and
// are swapped in/out with Container.swapItems, so enchantments, names,
// shulker contents etc. are always kept.

const pageOf = (chest) => Number(chest.getDynamicProperty("sp:page") ?? 0);

function refreshChestName(chest) {
  const cfg = CHESTS[chest.typeId];
  chest.nameTag = `${cfg.label} ${pageOf(chest) + 1}/${cfg.pages}`;
}

function findVault(chest, create) {
  const [vault] = chest.dimension.getEntities({ type: VAULT_ID, location: chest.location, maxDistance: 0.6 });
  if (vault || !create) return vault;
  return chest.dimension.spawnEntity(VAULT_ID, chest.location);
}

function switchPage(chest, player, delta) {
  const cfg = CHESTS[chest.typeId];
  const display = invOf(chest);
  const vault = invOf(findVault(chest, true));
  if (!cfg || !display || !vault) return;

  const cur = pageOf(chest) % cfg.pages;
  const next = (cur + delta + cfg.pages) % cfg.pages;
  for (let i = 0; i < PAGE_SIZE; i++) display.swapItems(i, cur * PAGE_SIZE + i, vault); // store current
  for (let i = 0; i < PAGE_SIZE; i++) display.swapItems(i, next * PAGE_SIZE + i, vault); // load next
  chest.setDynamicProperty("sp:page", next);
  refreshChestName(chest);

  sound(chest.dimension, "item.book.page_turn", chest.location);
  feedback(player, { translate: `entity.${chest.typeId}.name` }, { text: " §8| §f" }, tr("sp.msg.page", next + 1, cfg.pages));
}

function dropContainer(container, dim, at) {
  if (!container) return;
  for (let i = 0; i < container.size; i++) {
    const item = container.getItem(i);
    if (!item) continue;
    container.setItem(i);
    dim.spawnItem(item, at);
  }
}

function breakChest(chest) {
  const cfg = CHESTS[chest.typeId];
  const dim = chest.dimension;
  const at = { x: chest.location.x, y: chest.location.y + 0.5, z: chest.location.z };
  dropContainer(invOf(chest), dim, at);
  const vault = findVault(chest, false);
  if (vault) {
    dropContainer(invOf(vault), dim, at);
    vault.remove();
  }
  dim.spawnItem(new ItemStack(cfg.item, 1), at);
  sound(dim, "random.break", at);
  chest.remove();
}

const FACE_OFFSET = {
  up: [0, 1, 0], down: [0, -1, 0], north: [0, 0, -1],
  south: [0, 0, 1], east: [1, 0, 0], west: [-1, 0, 0],
};
const REPLACEABLE = new Set([
  "minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern",
  "minecraft:snow_layer", "minecraft:deadbush", "minecraft:seagrass",
]);
const canReplace = (b) => !!b && (b.isAir || b.isLiquid || REPLACEABLE.has(b.typeId));

function placeChest(player, dim, clickedLoc, face, chestId) {
  const clicked = dim.getBlock(clickedLoc);
  let pos = clickedLoc;
  if (!clicked || !REPLACEABLE.has(clicked.typeId)) {
    const o = FACE_OFFSET[lower(face)] ?? FACE_OFFSET.up;
    pos = { x: clickedLoc.x + o[0], y: clickedLoc.y + o[1], z: clickedLoc.z + o[2] };
  }
  const target = dim.getBlock(pos);
  const center = { x: pos.x + 0.5, y: pos.y, z: pos.z + 0.5 };
  const occupied =
    dim.getEntities({ location: center, maxDistance: 0.7, families: [CHEST_FAMILY] }).length > 0 ||
    dim.getPlayers({ location: { x: center.x, y: center.y + 0.5, z: center.z }, maxDistance: 0.9 }).length > 0;
  if (!canReplace(target) || occupied) {
    feedback(player, tr("sp.msg.cant_place"));
    return;
  }
  if (!target.isAir && !target.isLiquid) target.setType("minecraft:air");

  const chest = dim.spawnEntity(chestId, center);
  try {
    chest.setRotation({ x: 0, y: player.getRotation().y + 180 });
  } catch {}
  chest.setDynamicProperty("sp:page", 0);
  refreshChestName(chest);
  consumeHeld(player, CHESTS[chestId].item);
  sound(dim, "random.chestclosed", center);
}

// ============================================================ drawers
// Each drawer's data lives in a world dynamic property keyed by its position:
// { u: upgradeLevel, s: [ {i: itemId, n: count} | null, ... ] }
// Only "plain" stackable items are accepted, so storing just id+count loses nothing.

const drawerCache = new Map();
const drawerKey = (dim, l) => `sp:d|${dim.id}|${l.x}|${l.y}|${l.z}`;

function loadDrawer(dim, loc, slots) {
  const key = drawerKey(dim, loc);
  let d = drawerCache.get(key);
  if (!d) {
    try {
      const raw = world.getDynamicProperty(key);
      if (typeof raw === "string") d = JSON.parse(raw);
    } catch {}
    d = d && Array.isArray(d.s) ? d : { u: 0, s: [] };
    drawerCache.set(key, d);
  }
  while (d.s.length < slots) d.s.push(null);
  return d;
}

function saveDrawer(dim, loc, d) {
  const key = drawerKey(dim, loc);
  if (d.u === 0 && d.s.every((e) => !e)) {
    world.setDynamicProperty(key, undefined);
    drawerCache.delete(key);
  } else {
    world.setDynamicProperty(key, JSON.stringify(d));
    drawerCache.set(key, d);
  }
}

const slotStacks = (def, d) => def.stacks * UPGRADES[d.u].mult;
const capacity = (def, d, id) => slotStacks(def, d) * itemInfo(id).max;

/** Which slot of the front face was tapped, or -1 if unknown. */
function hitSlot(def, facing, face, rel) {
  if (def.slots === 1) return 0;
  if (!rel || face !== facing) return -1;
  let u; // 0 = viewer's left, 1 = viewer's right
  switch (facing) {
    case "north": u = 1 - rel.x; break;
    case "south": u = rel.x; break;
    case "west": u = rel.z; break;
    case "east": u = 1 - rel.z; break;
    default: return -1;
  }
  const top = rel.y >= 0.5;
  if (def.slots === 2) return top ? 0 : 1;
  return (top ? 0 : 2) + (u >= 0.5 ? 1 : 0);
}

/** faceLocation is documented as block-relative; accept absolute too. */
function relFace(loc, fl) {
  if (!fl) return undefined;
  const r = { x: fl.x - loc.x, y: fl.y - loc.y, z: fl.z - loc.z };
  const unit = (v) => v >= -0.01 && v <= 1.01;
  return unit(r.x) && unit(r.y) && unit(r.z) ? r : { x: fl.x, y: fl.y, z: fl.z };
}

function rejectReason(stack) {
  const info = itemInfo(stack.typeId);
  if (!info.proto || info.max <= 1) return "sp.msg.unstackable";
  try {
    if (!stack.isStackableWith(info.proto)) return "sp.msg.special";
  } catch {
    return "sp.msg.special";
  }
  return undefined;
}

function pickSlot(d, preferred, id) {
  if (preferred >= 0) {
    const e = d.s[preferred];
    if (!e || e.i === id) return preferred;
  }
  const same = d.s.findIndex((e) => e && e.i === id);
  return same >= 0 ? same : d.s.findIndex((e) => !e);
}

/** Adds up to `amount` items to slot idx, returns how many were accepted. */
function insert(def, d, idx, id, amount) {
  const e = d.s[idx] ?? { i: id, n: 0 };
  const take = Math.max(0, Math.min(capacity(def, d, id) - e.n, amount));
  e.n += take;
  d.s[idx] = e.n > 0 ? e : null;
  return take;
}

function depositHeld(player, inv, def, d, slot) {
  const sel = player.selectedSlotIndex;
  const held = inv.getItem(sel);
  const reason = rejectReason(held);
  if (reason) return feedback(player, tr(reason));
  const idx = pickSlot(d, slot, held.typeId);
  if (idx < 0) return feedback(player, tr("sp.msg.no_slot"));
  const took = insert(def, d, idx, held.typeId, held.amount);
  if (!took) return feedback(player, tr("sp.msg.full"));
  if (held.amount > took) {
    held.amount -= took;
    inv.setItem(sel, held);
  } else {
    inv.setItem(sel);
  }
  feedback(player, tr("sp.msg.added", fmt(took)), { text: " " }, itemName(held.typeId));
}

function depositAll(player, inv, def, d, slot, id) {
  const proto = itemInfo(id).proto;
  if (!proto) return;
  const idx = pickSlot(d, slot, id);
  if (idx < 0) return feedback(player, tr("sp.msg.no_slot"));
  let total = 0;
  for (let i = 0; i < inv.size; i++) {
    const it = inv.getItem(i);
    if (!it || it.typeId !== id || !it.isStackableWith(proto)) continue;
    const took = insert(def, d, idx, id, it.amount);
    if (!took) break;
    total += took;
    if (it.amount > took) {
      it.amount -= took;
      inv.setItem(i, it);
    } else {
      inv.setItem(i);
    }
  }
  if (total) feedback(player, tr("sp.msg.added", fmt(total)), { text: " " }, itemName(id));
  else feedback(player, tr("sp.msg.full"));
}

/** mode: "one" | "stack" | "fill" (as much as fits in the inventory). */
function takeOut(player, inv, d, slot, mode) {
  const idx = slot >= 0 && d.s[slot] ? slot : d.s.findIndex((e) => e && e.n > 0);
  if (idx < 0) return feedback(player, tr("sp.msg.empty_drawer"));
  const e = d.s[idx];
  const max = itemInfo(e.i).max;
  let total = 0;
  for (let round = mode === "fill" ? 64 : 1; round > 0 && e.n > 0; round--) {
    const want = mode === "one" ? 1 : Math.min(max, e.n);
    const left = inv.addItem(new ItemStack(e.i, want));
    const given = want - (left?.amount ?? 0);
    e.n -= given;
    total += given;
    if (given < want) break; // inventory full
  }
  if (e.n <= 0) d.s[idx] = null;
  if (total) feedback(player, tr("sp.msg.taken", fmt(total)), { text: " " }, itemName(e.i));
  else feedback(player, tr("sp.msg.inv_full"));
}

function applyUpgrade(player, block, d, lvl) {
  if (lvl <= d.u) return feedback(player, tr("sp.msg.upgrade_lower"));
  const old = d.u;
  d.u = lvl;
  consumeHeld(player, UPGRADES[lvl].item);
  if (old > 0 && !isCreative(player)) give(player, new ItemStack(UPGRADES[old].item, 1));
  block.setPermutation(block.permutation.withState("sp:upgrade", lvl));
  sound(block.dimension, "random.anvil_use", block.center());
  feedback(player, tr("sp.msg.upgrade_done"), { text: " §f" }, tr(`sp.upgrade.${lvl}`));
}

const lastTap = new Map(); // player.id -> { key, tick, mode, id, slot }

function onDrawerInteract(player, dim, loc, face, faceLocation) {
  const block = dim.getBlock(loc);
  const def = block && DRAWERS[block.typeId];
  const inv = invOf(player);
  if (!def || !inv) return;

  const d = loadDrawer(dim, loc, def.slots);
  const facing = lower(block.permutation.getState("minecraft:cardinal_direction") ?? "north");
  const slot = hitSlot(def, facing, lower(face), relFace(loc, faceLocation));
  const held = inv.getItem(player.selectedSlotIndex);

  const key = drawerKey(dim, loc);
  const now = system.currentTick;
  const prev = lastTap.get(player.id);
  const quick = prev && prev.key === key && now - prev.tick <= DOUBLE_TAP_TICKS;
  lastTap.set(player.id, { key, tick: now, mode: "", id: held?.typeId, slot });
  const remember = (mode, id) => Object.assign(lastTap.get(player.id), { mode, id });

  if (player.isSneaking) {
    takeOut(player, inv, d, slot, "one");
  } else if (held && UPGRADE_BY_ITEM[held.typeId] !== undefined) {
    applyUpgrade(player, block, d, UPGRADE_BY_ITEM[held.typeId]);
  } else if (quick && prev.mode === "put" && (!held || held.typeId === prev.id)) {
    depositAll(player, inv, def, d, prev.slot, prev.id);
    lastTap.delete(player.id);
  } else if (quick && prev.mode === "take") {
    // the first tap may have put the stack into the selected slot, so ignore `held`
    takeOut(player, inv, d, prev.slot, "fill");
    lastTap.delete(player.id);
  } else if (held) {
    depositHeld(player, inv, def, d, slot);
    remember("put", held.typeId);
  } else {
    takeOut(player, inv, d, slot, "stack");
    remember("take");
  }
  saveDrawer(dim, loc, d);
}

function dropDrawerContents(dim, loc, d) {
  const at = { x: loc.x + 0.5, y: loc.y + 0.5, z: loc.z + 0.5 };
  for (const e of d.s) {
    if (!e) continue;
    const max = itemInfo(e.i).max;
    for (let n = e.n; n > 0; n -= max) dim.spawnItem(new ItemStack(e.i, Math.min(max, n)), at);
  }
  if (d.u > 0) dim.spawnItem(new ItemStack(UPGRADES[d.u].item, 1), at);
}

// ============================================================ events
world.beforeEvents.playerInteractWithBlock.subscribe((ev) => {
  const { player, block } = ev;
  const isDrawer = !!DRAWERS[block.typeId];
  const chestId = !isDrawer && ev.itemStack ? CHEST_BY_ITEM[ev.itemStack.typeId] : undefined;
  if (!isDrawer && !chestId) return;

  ev.cancel = true; // no block placing / item use on drawers; we place the chest ourselves
  if (ev.isFirstEvent === false) return;
  if (!cooldown(`${player.id}:block`, 3)) return;

  const dim = block.dimension;
  const loc = block.location;
  const face = ev.blockFace;
  const faceLocation = ev.faceLocation;
  system.run(() => {
    if (!isValid(player)) return;
    if (isDrawer) onDrawerInteract(player, dim, loc, face, faceLocation);
    else placeChest(player, dim, loc, face, chestId);
  });
});

world.beforeEvents.playerInteractWithEntity.subscribe((ev) => {
  const { player, target } = ev;
  if (target.typeId === VAULT_ID) {
    ev.cancel = true;
    return;
  }
  if (!CHESTS[target.typeId]) return;
  if (player.isSneaking) {
    // Sneak + interact = next page (normal interact opens the chest)
    ev.cancel = true;
    if (cooldown(`${player.id}:page`, 4)) system.run(() => isValid(target) && switchPage(target, player, 1));
  } else {
    system.run(() => isValid(target) && sound(target.dimension, "random.chestopen", target.location));
  }
});

const chestHits = new Map(); // chest.id -> { n, tick }
world.afterEvents.entityHitEntity.subscribe((ev) => {
  const player = ev.damagingEntity;
  const chest = ev.hitEntity;
  if (player?.typeId !== "minecraft:player" || !isValid(chest) || !CHESTS[chest.typeId]) return;
  if (!cooldown(`${player.id}:hit`, 3)) return;

  const held = invOf(player)?.getItem(player.selectedSlotIndex);
  if (!held?.typeId.endsWith("_pickaxe")) {
    switchPage(chest, player, player.isSneaking ? -1 : 1);
    return;
  }
  if (isCreative(player)) return breakChest(chest);
  const now = system.currentTick;
  const prev = chestHits.get(chest.id);
  const n = prev && now - prev.tick < 40 ? prev.n + 1 : 1;
  if (n >= BREAK_HITS) {
    chestHits.delete(chest.id);
    breakChest(chest);
  } else {
    chestHits.set(chest.id, { n, tick: now });
    sound(chest.dimension, "dig.wood", chest.location);
    feedback(player, tr("sp.msg.break_more", BREAK_HITS - n));
  }
});

// Chest killed some other way (e.g. /kill): don't lose the hidden pages.
world.afterEvents.entityDie.subscribe(
  (ev) => {
    try {
      const chest = ev.deadEntity;
      const vault = findVault(chest, false);
      if (!vault) return;
      const at = { x: chest.location.x, y: chest.location.y + 0.5, z: chest.location.z };
      dropContainer(invOf(vault), chest.dimension, at);
      vault.remove();
    } catch {}
  },
  { entityTypes: Object.keys(CHESTS) }
);

world.beforeEvents.playerBreakBlock.subscribe((ev) => {
  const def = DRAWERS[ev.block.typeId];
  if (!def) return;
  const d = loadDrawer(ev.block.dimension, ev.block.location, def.slots);
  const stacks = d.s.reduce((sum, e) => sum + (e ? Math.ceil(e.n / itemInfo(e.i).max) : 0), 0);
  if (stacks > MAX_BREAK_STACKS) {
    ev.cancel = true;
    const player = ev.player;
    system.run(() => feedback(player, tr("sp.msg.too_full", fmt(stacks), MAX_BREAK_STACKS)));
  }
});

world.afterEvents.playerBreakBlock.subscribe((ev) => {
  const def = DRAWERS[ev.brokenBlockPermutation.type.id];
  if (!def) return;
  const dim = ev.block.dimension;
  const loc = ev.block.location;
  const d = loadDrawer(dim, loc, def.slots);
  dropDrawerContents(dim, loc, d);
  saveDrawer(dim, loc, { u: 0, s: [] });
});

// A freshly placed drawer must never inherit stale data (e.g. left by /setblock).
world.afterEvents.playerPlaceBlock.subscribe((ev) => {
  if (!DRAWERS[ev.block.typeId]) return;
  saveDrawer(ev.block.dimension, ev.block.location, { u: 0, s: [] });
});

// ============================================================ look-at info (action bar)
function drawerInfo(block, def) {
  const d = loadDrawer(block.dimension, block.location, def.slots);
  const parts = [{ translate: `tile.${block.typeId}.name` }, { text: " §8| §b" }, tr(`sp.upgrade.${d.u}`)];
  d.s.forEach((e, i) => {
    parts.push({ text: def.slots > 1 ? `\n§7[${i + 1}] §f` : "\n§f" });
    if (e) parts.push(itemName(e.i), { text: `§7: §e${fmt(e.n)} §7/ ${fmt(capacity(def, d, e.i))}` });
    else parts.push(tr("sp.msg.empty_slot", fmt(slotStacks(def, d))));
  });
  parts.push({ text: "\n" }, tr("sp.msg.drawer_hint"));
  return parts;
}

function chestInfo(chest) {
  const cfg = CHESTS[chest.typeId];
  return [
    { translate: `entity.${chest.typeId}.name` },
    { text: " §8| §f" },
    tr("sp.msg.page", pageOf(chest) + 1, cfg.pages),
    { text: "\n" },
    tr("sp.msg.chest_hint"),
  ];
}

function showLookInfo(player) {
  const head = player.getHeadLocation();
  const dist = (l) => Math.hypot(l.x - head.x, l.y - head.y, l.z - head.z);

  const blockHit = player.getBlockFromViewDirection({ maxDistance: 6 });
  const chestHit = player
    .getEntitiesFromViewDirection({ maxDistance: 6 })
    .find((h) => CHESTS[h.entity.typeId]);

  const block = blockHit?.block;
  const def = block && DRAWERS[block.typeId];
  const blockDist = block ? dist(block.center()) : Infinity;

  if (chestHit && chestHit.distance <= blockDist) {
    player.onScreenDisplay.setActionBar({ rawtext: chestInfo(chestHit.entity) });
  } else if (def) {
    player.onScreenDisplay.setActionBar({ rawtext: drawerInfo(block, def) });
  }
}

system.runInterval(() => {
  const now = system.currentTick;
  for (const player of world.getAllPlayers()) {
    if ((quietUntil.get(player.id) ?? 0) > now) continue;
    try {
      showLookInfo(player);
    } catch {}
  }
  if (now % 1200 === 0) {
    for (const [k, v] of chestHits) if (now - v.tick > 100) chestHits.delete(k);
  }
}, 5);
