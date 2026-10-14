#!/usr/bin/env python3
"""Generates the JSON data/assets and the procedural textures for the mod.

Run from the repository root:  python3 tools/generate_resources.py
"""
import json
import random
from pathlib import Path

from PIL import Image

MOD = "ironchests"
ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
ASSETS = ROOT / "assets" / MOD
DATA = ROOT / "data"

# name -> (display name EN, display name TH, particle texture)
TIERS = {
    "copper": ("Copper Chest", "หีบทองแดง", "minecraft:block/copper_block"),
    "iron": ("Iron Chest", "หีบเหล็ก", "minecraft:block/iron_block"),
    "gold": ("Gold Chest", "หีบทอง", "minecraft:block/gold_block"),
    "diamond": ("Diamond Chest", "หีบเพชร", "minecraft:block/diamond_block"),
    "crystal": ("Crystal Chest", "หีบคริสตัล", "minecraft:block/glass"),
    "obsidian": ("Obsidian Chest", "หีบออบซิเดียน", "minecraft:block/obsidian"),
    "dirt": ("DirtChest 9000!", "หีบดิน 9000!", "minecraft:block/dirt"),
}

UPGRADES = [
    ("wood", "copper"),
    ("wood", "iron"),
    ("copper", "iron"),
    ("iron", "gold"),
    ("gold", "diamond"),
    ("diamond", "crystal"),
    ("diamond", "obsidian"),
]

MATERIAL_EN = {"wood": "Wood", "copper": "Copper", "iron": "Iron", "gold": "Gold",
               "diamond": "Diamond", "crystal": "Crystal", "obsidian": "Obsidian"}
MATERIAL_TH = {"wood": "ไม้", "copper": "ทองแดง", "iron": "เหล็ก", "gold": "ทอง",
               "diamond": "เพชร", "crystal": "คริสตัล", "obsidian": "ออบซิเดียน"}

# Texture palettes: (body, body dark/rim, lock)
PALETTES = {
    "copper": ((196, 112, 74), (122, 62, 40), (226, 226, 226)),
    "iron": ((200, 200, 200), (110, 110, 110), (240, 200, 60)),
    "gold": ((250, 210, 60), (170, 120, 20), (230, 230, 230)),
    "diamond": ((110, 230, 220), (40, 140, 150), (250, 210, 60)),
    "crystal": ((215, 245, 255), (150, 210, 230), (250, 250, 250)),
    "obsidian": ((40, 26, 60), (16, 10, 26), (130, 80, 190)),
    "dirt": ((134, 96, 67), (90, 62, 42), (110, 110, 110)),
    "wood": ((160, 120, 70), (100, 72, 40), (200, 200, 200)),
}


def write_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# --------------------------------------------------------------------------- assets

def gen_assets():
    for name, (_, _, particle) in TIERS.items():
        block = f"{name}_chest"
        write_json(ASSETS / "blockstates" / f"{block}.json",
                   {"variants": {"": {"model": f"{MOD}:block/{block}"}}})
        write_json(ASSETS / "models" / "block" / f"{block}.json",
                   {"textures": {"particle": particle}})
        write_json(ASSETS / "models" / "item" / f"{block}.json",
                   {"parent": f"{MOD}:item/chest_template", "textures": {"particle": particle}})

    # Chest items are drawn by a BlockEntityWithoutLevelRenderer ("builtin/entity").
    write_json(ASSETS / "models" / "item" / "chest_template.json", {
        "parent": "builtin/entity",
        "display": {
            "gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "head": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 315, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 315, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
        },
    })

    for src, dst in UPGRADES:
        item = f"{src}_to_{dst}_chest_upgrade"
        write_json(ASSETS / "models" / "item" / f"{item}.json",
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})

    en = {f"itemGroup.{MOD}": "Iron Chests"}
    th = {f"itemGroup.{MOD}": "หีบเหล็ก (Iron Chests)"}
    for name, (en_name, th_name, _) in TIERS.items():
        en[f"block.{MOD}.{name}_chest"] = en_name
        th[f"block.{MOD}.{name}_chest"] = th_name
    for src, dst in UPGRADES:
        item = f"item.{MOD}.{src}_to_{dst}_chest_upgrade"
        en[item] = f"{MATERIAL_EN[src]} to {MATERIAL_EN[dst]} Chest Upgrade"
        th[item] = f"ชุดอัปเกรดหีบ{MATERIAL_TH[src]}เป็น{MATERIAL_TH[dst]}"
    en.update({
        f"tooltip.{MOD}.upgrade.from_to": "Upgrades %s into %s",
        f"tooltip.{MOD}.upgrade.how": "Sneak + right click a chest to use",
        f"message.{MOD}.upgrade.in_use": "Someone is using this chest!",
        f"message.{MOD}.upgrade.double_chest": "Double chests can't be upgraded",
    })
    th.update({
        f"tooltip.{MOD}.upgrade.from_to": "อัปเกรด %s เป็น %s",
        f"tooltip.{MOD}.upgrade.how": "ย่อตัว + คลิกขวาที่หีบเพื่อใช้",
        f"message.{MOD}.upgrade.in_use": "มีคนกำลังเปิดหีบนี้อยู่!",
        f"message.{MOD}.upgrade.double_chest": "หีบคู่ไม่สามารถอัปเกรดได้",
    })
    write_json(ASSETS / "lang" / "en_us.json", en)
    write_json(ASSETS / "lang" / "th_th.json", th)


# --------------------------------------------------------------------------- data

def ingredient(key: str):
    if key.startswith("#"):
        return {"tag": key[1:]}
    return {"item": key}


def shaped(name, pattern, keys, result):
    write_json(DATA / MOD / "recipes" / f"{name}.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": {k: ingredient(v) for k, v in keys.items()},
        "result": {"item": result, "count": 1},
    })


def gen_data():
    # Loot tables: drop the chest itself, keeping its custom name.
    for name in TIERS:
        block = f"{MOD}:{name}_chest"
        write_json(DATA / MOD / "loot_tables" / "blocks" / f"{name}_chest.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{
                    "type": "minecraft:item",
                    "name": block,
                    "functions": [{"function": "minecraft:copy_name", "source": "block_entity"}],
                }],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })

    metal = [f"{MOD}:{n}_chest" for n in TIERS if n != "dirt"]
    write_json(DATA / "minecraft" / "tags" / "blocks" / "mineable" / "pickaxe.json",
               {"replace": False, "values": metal})
    write_json(DATA / "minecraft" / "tags" / "blocks" / "mineable" / "shovel.json",
               {"replace": False, "values": [f"{MOD}:dirt_chest"]})
    all_chests = [f"{MOD}:{n}_chest" for n in TIERS]
    write_json(DATA / "forge" / "tags" / "blocks" / "chests.json", {"replace": False, "values": all_chests})
    write_json(DATA / "forge" / "tags" / "items" / "chests.json", {"replace": False, "values": all_chests})

    wood_chest = "#forge:chests/wooden"
    c = lambda n: f"{MOD}:{n}_chest"

    # Chests
    shaped("copper_chest", ["CCC", "CWC", "CCC"], {"C": "#forge:ingots/copper", "W": wood_chest}, c("copper"))
    shaped("iron_chest", ["III", "IWI", "III"], {"I": "#forge:ingots/iron", "W": wood_chest}, c("iron"))
    shaped("iron_chest_from_copper_chest", ["IGI", "GCG", "IGI"],
           {"I": "#forge:ingots/iron", "G": "#forge:glass", "C": c("copper")}, c("iron"))
    shaped("gold_chest", ["GGG", "GCG", "GGG"], {"G": "#forge:ingots/gold", "C": c("iron")}, c("gold"))
    shaped("diamond_chest", ["GGG", "DCD", "GGG"],
           {"G": "#forge:glass", "D": "#forge:gems/diamond", "C": c("gold")}, c("diamond"))
    shaped("crystal_chest", ["GGG", "GCG", "GGG"], {"G": "#forge:glass", "C": c("diamond")}, c("crystal"))
    shaped("obsidian_chest", ["OOO", "OCO", "OOO"], {"O": "#forge:obsidian", "C": c("diamond")}, c("obsidian"))
    shaped("dirt_chest", ["DDD", "DCD", "DDD"], {"D": "minecraft:dirt", "C": wood_chest}, c("dirt"))

    # Upgrade kits
    u = lambda a, b: f"{MOD}:{a}_to_{b}_chest_upgrade"
    shaped("wood_to_copper_chest_upgrade", ["CCC", "CPC", "CCC"],
           {"C": "#forge:ingots/copper", "P": "#minecraft:planks"}, u("wood", "copper"))
    shaped("wood_to_iron_chest_upgrade", ["III", "IPI", "III"],
           {"I": "#forge:ingots/iron", "P": "#minecraft:planks"}, u("wood", "iron"))
    shaped("copper_to_iron_chest_upgrade", ["IGI", "GCG", "IGI"],
           {"I": "#forge:ingots/iron", "G": "#forge:glass", "C": "#forge:ingots/copper"}, u("copper", "iron"))
    shaped("iron_to_gold_chest_upgrade", ["GGG", "GIG", "GGG"],
           {"G": "#forge:ingots/gold", "I": "#forge:ingots/iron"}, u("iron", "gold"))
    shaped("gold_to_diamond_chest_upgrade", ["GGG", "DID", "GGG"],
           {"G": "#forge:glass", "D": "#forge:gems/diamond", "I": "#forge:ingots/gold"}, u("gold", "diamond"))
    shaped("diamond_to_crystal_chest_upgrade", ["GGG", "GOG", "GGG"],
           {"G": "#forge:glass", "O": "#forge:obsidian"}, u("diamond", "crystal"))
    shaped("diamond_to_obsidian_chest_upgrade", ["OOO", "OGO", "OOO"],
           {"O": "#forge:obsidian", "G": "#forge:glass"}, u("diamond", "obsidian"))


# --------------------------------------------------------------------------- textures

def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def box_faces(u, v, w, h, d):
    """Face rectangles (x0, y0, x1, y1) of a model box in Minecraft's box UV layout."""
    return [
        (u + d, v, u + d + w, v + d),                    # top
        (u + d + w, v, u + d + 2 * w, v + d),            # bottom
        (u, v + d, u + d, v + d + h),                    # side
        (u + d, v + d, u + d + w, v + d + h),            # front
        (u + d + w, v + d, u + 2 * d + w, v + d + h),    # side
        (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),  # back
    ]


def paint_face(img, rect, body, rim, rng, transparent=False):
    x0, y0, x1, y1 = rect
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            edge = x in (x0, x1 - 1) or y in (y0, y1 - 1)
            if edge:
                px[x, y] = rim + (255,)
            elif transparent:
                # Mostly clear glass with a few bright glints
                px[x, y] = (255, 255, 255, 255) if (x - y) % 7 == 0 and rng.random() < 0.5 else (0, 0, 0, 0)
            else:
                n = rng.uniform(0.9, 1.08)
                if (y - y0) % 4 == 0:
                    n *= 0.92  # subtle plates
                px[x, y] = shade(body, n) + (255,)


def gen_chest_texture(name):
    body, rim, lock = PALETTES[name]
    rng = random.Random(name)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    transparent = name == "crystal"
    for face in box_faces(0, 0, 14, 5, 14):      # lid
        paint_face(img, face, body, rim, rng, transparent)
    for face in box_faces(0, 19, 14, 10, 14):    # bottom
        paint_face(img, face, body, rim, rng, transparent)
    for face in box_faces(0, 0, 2, 4, 1):        # lock
        x0, y0, x1, y1 = face
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), shade(lock, rng.uniform(0.85, 1.0)) + (255,))
    out = ASSETS / "textures" / "entity" / "chest" / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)


def gen_upgrade_texture(src, dst):
    """16x16 icon: a frame in the source material with the target material inside and an arrow."""
    s_body, s_rim, _ = PALETTES[src]
    d_body, d_rim, _ = PALETTES[dst]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(1, 15):
        for x in range(1, 15):
            outer = x in (1, 14) or y in (1, 14)
            frame = x in (2, 13) or y in (2, 13)
            if outer:
                px[x, y] = s_rim + (255,)
            elif frame:
                px[x, y] = s_body + (255,)
            else:
                px[x, y] = shade(d_body, 1.0 - 0.03 * ((x + y) % 3)) + (255,)
    # Upward arrow in the middle
    arrow = ["...##...", "..####..", ".######.", "...##...", "...##...", "...##..."]
    for dy, row in enumerate(arrow):
        for dx, ch in enumerate(row):
            if ch == "#":
                px[4 + dx, 5 + dy] = d_rim + (255,)
    out = ASSETS / "textures" / "item" / f"{src}_to_{dst}_chest_upgrade.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)


def gen_textures():
    for name in TIERS:
        gen_chest_texture(name)
    for src, dst in UPGRADES:
        gen_upgrade_texture(src, dst)


if __name__ == "__main__":
    gen_assets()
    gen_data()
    gen_textures()
    print("Resources generated in", ROOT)
