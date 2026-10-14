#!/usr/bin/env python3
"""Generates every JSON/PNG file of the Storage Plus add-on.

scripts/main.js is hand-written; everything else under packs/ comes from here.
Run:  python3 tools/generate.py
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BP = ROOT / "packs" / "StoragePlus_BP"
RP = ROOT / "packs" / "StoragePlus_RP"

VERSION = [1, 0, 0]
MIN_ENGINE = [1, 21, 90]
SCRIPT_API = "2.0.0"

UUID = {
    "bp_header": "5b1e9c2a-6d3f-4a8e-9f1b-2c7d8e4a6b01",
    "bp_data": "5b1e9c2a-6d3f-4a8e-9f1b-2c7d8e4a6b02",
    "bp_script": "5b1e9c2a-6d3f-4a8e-9f1b-2c7d8e4a6b03",
    "rp_header": "8c4f2e7b-1a9d-4c6e-b3f5-7e2a9d1c4f01",
    "rp_res": "8c4f2e7b-1a9d-4c6e-b3f5-7e2a9d1c4f02",
}

# ---------------------------------------------------------------- data tables
# Keep pages / inventory size in sync with scripts/main.js.
CHESTS = {
    "iron": {"pages": 2, "base": (205, 205, 212), "dark": (110, 110, 122), "accent": (70, 70, 80),
             "en": "Iron Chest", "th": "หีบเหล็ก"},
    "gold": {"pages": 3, "base": (245, 205, 70), "dark": (175, 120, 25), "accent": (120, 70, 10),
             "en": "Gold Chest", "th": "หีบทอง"},
    "diamond": {"pages": 5, "base": (100, 225, 215), "dark": (30, 140, 150), "accent": (15, 80, 95),
                "en": "Diamond Chest", "th": "หีบเพชร"},
    "netherite": {"pages": 8, "base": (78, 68, 74), "dark": (38, 33, 36), "accent": (150, 110, 90),
                  "en": "Netherite Chest", "th": "หีบเนเธอไรต์"},
}
PAGE_SIZE = 27
VAULT_SIZE = PAGE_SIZE * max(c["pages"] for c in CHESTS.values())

DRAWERS = {
    1: {"stacks": 32, "en": "Drawer (1 slot)", "th": "ลิ้นชัก (1 ช่อง)"},
    2: {"stacks": 16, "en": "Drawer (2 slots)", "th": "ลิ้นชัก (2 ช่อง)"},
    4: {"stacks": 8, "en": "Drawer (4 slots)", "th": "ลิ้นชัก (4 ช่อง)"},
}
# index = upgrade level stored in block state sp:upgrade
UPGRADES = [
    {"id": None, "mult": 1, "trim": (92, 60, 30), "en": "No upgrade", "th": "ไม่มีอัปเกรด"},
    {"id": "iron", "mult": 4, "trim": (215, 215, 222), "material": "minecraft:iron_ingot",
     "en": "Iron", "th": "เหล็ก"},
    {"id": "gold", "mult": 16, "trim": (250, 210, 60), "material": "minecraft:gold_ingot",
     "en": "Gold", "th": "ทอง"},
    {"id": "diamond", "mult": 64, "trim": (90, 230, 220), "material": "minecraft:diamond",
     "en": "Diamond", "th": "เพชร"},
    {"id": "netherite", "mult": 256, "trim": (28, 22, 26), "en": "Netherite", "th": "เนเธอไรต์"},
]
FACINGS = ["north", "south", "east", "west"]


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def write_text(path: Path, text: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------- pixel art
def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def noisy(c, rng, amt=0.08):
    return shade(c, 1 + rng.uniform(-amt, amt))


def fill_rect(img, x0, y0, w, h, color, rng=None, amt=0.08):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), (*(noisy(color, rng, amt) if rng else color), 255))


def outline(img, x0, y0, w, h, color):
    for x in range(x0, x0 + w):
        img.putpixel((x, y0), (*color, 255))
        img.putpixel((x, y0 + h - 1), (*color, 255))
    for y in range(y0, y0 + h):
        img.putpixel((x0, y), (*color, 255))
        img.putpixel((x0 + w - 1, y), (*color, 255))


def paint_box(img, u, v, w, h, d, base, dark, rng):
    """Paints every face of a Bedrock box-UV cube."""
    faces = [
        (u + d, v, w, d), (u + d + w, v, w, d),            # top, bottom
        (u, v + d, d, h), (u + d, v + d, w, h),            # east, north
        (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h),  # west, south
    ]
    for x, y, fw, fh in faces:
        fill_rect(img, x, y, fw, fh, base, rng)
        outline(img, x, y, fw, fh, dark)


def chest_entity_texture(c, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    paint_box(img, 0, 19, 14, 10, 14, c["base"], c["dark"], rng)  # base
    paint_box(img, 0, 0, 14, 5, 14, c["base"], c["dark"], rng)    # lid
    paint_box(img, 0, 0, 2, 4, 1, c["accent"], shade(c["accent"], 0.6), rng)  # latch
    return img


def chest_item_icon(c, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    fill_rect(img, 1, 2, 14, 5, shade(c["base"], 1.08), rng)   # lid
    fill_rect(img, 1, 7, 14, 8, c["base"], rng)                # body
    outline(img, 1, 2, 14, 13, c["dark"])
    for x in range(1, 15):
        img.putpixel((x, 7), (*c["dark"], 255))
    fill_rect(img, 7, 5, 2, 4, c["accent"])                    # latch
    return img


def upgrade_icon(trim, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dark = shade(trim, 0.55)
    fill_rect(img, 2, 2, 12, 12, trim, rng, 0.06)
    outline(img, 2, 2, 12, 12, dark)
    arrow = [(7, 4), (8, 4), (6, 5), (7, 5), (8, 5), (9, 5), (5, 6), (6, 6), (7, 6), (8, 6), (9, 6), (10, 6)]
    arrow += [(x, y) for y in range(7, 12) for x in (7, 8)]
    arrow_color = (235, 235, 235) if sum(trim) < 200 else shade(dark, 0.7)
    for p in arrow:
        img.putpixel(p, (*arrow_color, 255))
    return img


WOOD = (162, 112, 62)
WOOD_DARK = (104, 68, 36)
PANEL = (128, 86, 46)


def drawer_side(seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    fill_rect(img, 0, 0, 16, 16, WOOD, rng)
    for y in (4, 8, 12):  # plank seams
        for x in range(16):
            img.putpixel((x, y), (*shade(WOOD, 0.8), 255))
    outline(img, 0, 0, 16, 16, WOOD_DARK)
    return img


def drawer_front(slots, trim, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    fill_rect(img, 0, 0, 16, 16, WOOD, rng)
    outline(img, 0, 0, 16, 16, trim)
    if slots == 1:
        panels = [(2, 2, 12, 12)]
    elif slots == 2:
        panels = [(2, 2, 12, 5), (2, 9, 12, 5)]
    else:
        panels = [(2, 2, 5, 5), (9, 2, 5, 5), (2, 9, 5, 5), (9, 9, 5, 5)]
    for x, y, w, h in panels:
        fill_rect(img, x, y, w, h, PANEL, rng, 0.05)
        outline(img, x, y, w, h, WOOD_DARK)
        hx = x + w // 2 - 1
        hy = y + 1 if h > 5 else y + 1
        img.putpixel((hx, hy), (*trim, 255))
        img.putpixel((hx + 1, hy), (*trim, 255))
    # trim dividers so the upgrade colour is visible
    if slots >= 2:
        for x in range(1, 15):
            img.putpixel((x, 8), (*trim, 255))
    if slots == 4:
        for y in range(1, 15):
            img.putpixel((8, y), (*trim, 255))
    return img


def save_png(img, path: Path, scale=1):
    path.parent.mkdir(parents=True, exist_ok=True)
    if scale != 1:
        img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
    img.save(path)


def pack_icon():
    img = Image.new("RGBA", (16, 16), (40, 40, 48, 255))
    chest = chest_item_icon(CHESTS["diamond"], 1)
    img.alpha_composite(chest)
    return img


# ---------------------------------------------------------------- behaviour pack
def gen_bp():
    write_json(BP / "manifest.json", {
        "format_version": 2,
        "header": {
            "name": "Storage Plus BP",
            "description": "Iron/Gold/Diamond/Netherite chests + Drawers with upgrades",
            "uuid": UUID["bp_header"], "version": VERSION, "min_engine_version": MIN_ENGINE,
        },
        "modules": [
            {"type": "data", "uuid": UUID["bp_data"], "version": VERSION},
            {"type": "script", "language": "javascript", "uuid": UUID["bp_script"],
             "entry": "scripts/main.js", "version": VERSION},
        ],
        "dependencies": [
            {"uuid": UUID["rp_header"], "version": VERSION},
            {"module_name": "@minecraft/server", "version": SCRIPT_API},
        ],
    })

    common = {
        "minecraft:health": {"value": 20, "max": 20},
        "minecraft:damage_sensor": {"triggers": {"cause": "all", "deals_damage": False}},
        "minecraft:physics": {"has_gravity": False, "has_collision": True},
        "minecraft:pushable": {"is_pushable": False, "is_pushable_by_piston": False},
        "minecraft:knockback_resistance": {"value": 1.0},
        "minecraft:fire_immune": True,
    }

    for tier, c in CHESTS.items():
        write_json(BP / "entities" / f"{tier}_chest.json", {
            "format_version": "1.16.0",
            "minecraft:entity": {
                "description": {"identifier": f"sp:{tier}_chest_entity", "is_spawnable": False,
                                "is_summonable": True, "is_experimental": False},
                "components": {
                    "minecraft:type_family": {"family": ["sp_chest", "inanimate"]},
                    "minecraft:inventory": {"container_type": "container",
                                            "inventory_size": PAGE_SIZE, "private": False},
                    "minecraft:collision_box": {"width": 0.875, "height": 0.875},
                    "minecraft:nameable": {"always_show": False, "allow_name_tag_renaming": False},
                    **common,
                },
            },
        })
        write_json(BP / "items" / f"{tier}_chest.json", item_json(f"{tier}_chest"))

    # Invisible entity that holds the pages that are not currently shown.
    write_json(BP / "entities" / "chest_vault.json", {
        "format_version": "1.16.0",
        "minecraft:entity": {
            "description": {"identifier": "sp:chest_vault", "is_spawnable": False,
                            "is_summonable": True, "is_experimental": False},
            "components": {
                "minecraft:type_family": {"family": ["sp_vault", "inanimate"]},
                "minecraft:inventory": {"container_type": "container",
                                        "inventory_size": VAULT_SIZE, "private": False},
                "minecraft:collision_box": {"width": 0.1, "height": 0.1},
                **common,
            },
        },
    })

    for up in UPGRADES[1:]:
        write_json(BP / "items" / f"upgrade_{up['id']}.json", item_json(f"upgrade_{up['id']}"))

    for slots in DRAWERS:
        write_json(BP / "blocks" / f"drawer_{slots}.json", drawer_block(slots))

    gen_recipes()


def item_json(name):
    return {
        "format_version": "1.21.40",
        "minecraft:item": {
            "description": {"identifier": f"sp:{name}", "menu_category": {"category": "items"}},
            "components": {
                "minecraft:icon": {"textures": {"default": f"sp_{name}"}},
                "minecraft:display_name": {"value": f"item.sp.{name}.name"},
                "minecraft:max_stack_size": 64,
            },
        },
    }


def materials(slots, upgrade, facing):
    side = {"texture": "sp_drawer_side", "render_method": "opaque"}
    front = {"texture": f"sp_drawer_{slots}_front_{upgrade}", "render_method": "opaque"}
    return {"*": side, facing: front}


def drawer_block(slots):
    perms = []
    for facing in FACINGS:
        for lvl in range(len(UPGRADES)):
            perms.append({
                "condition": f"q.block_state('minecraft:cardinal_direction') == '{facing}' && "
                             f"q.block_state('sp:upgrade') == {lvl}",
                "components": {"minecraft:material_instances": materials(slots, lvl, facing)},
            })
    return {
        "format_version": "1.21.40",
        "minecraft:block": {
            "description": {
                "identifier": f"sp:drawer_{slots}",
                "menu_category": {"category": "items"},
                "states": {"sp:upgrade": list(range(len(UPGRADES)))},
                "traits": {"minecraft:placement_direction": {
                    "enabled_states": ["minecraft:cardinal_direction"], "y_rotation_offset": 180}},
            },
            "components": {
                "minecraft:destructible_by_mining": {"seconds_to_destroy": 1.5},
                "minecraft:destructible_by_explosion": {"explosion_resistance": 1200},
                "minecraft:map_color": "#8b5a2b",
                "minecraft:material_instances": materials(slots, 0, "north"),
            },
            "permutations": perms,
        },
    }


def shaped(name, pattern, key, result, count=1):
    write_json(BP / "recipes" / f"{name}.json", {
        "format_version": "1.20.10",
        "minecraft:recipe_shaped": {
            "description": {"identifier": f"sp:{name}"},
            "tags": ["crafting_table"],
            "pattern": pattern,
            "key": key,
            "result": {"item": result, "count": count},
        },
    })


def shapeless(name, ingredients, result):
    write_json(BP / "recipes" / f"{name}.json", {
        "format_version": "1.20.10",
        "minecraft:recipe_shapeless": {
            "description": {"identifier": f"sp:{name}"},
            "tags": ["crafting_table"],
            "ingredients": [{"item": i} for i in ingredients],
            "result": {"item": result},
        },
    })


def gen_recipes():
    ring = ["MMM", "MCM", "MMM"]
    shaped("iron_chest", ring, {"M": {"item": "minecraft:iron_ingot"}, "C": {"item": "minecraft:chest"}},
           "sp:iron_chest")
    shaped("gold_chest", ring, {"M": {"item": "minecraft:gold_ingot"}, "C": {"item": "sp:iron_chest"}},
           "sp:gold_chest")
    shaped("diamond_chest", ring, {"M": {"item": "minecraft:diamond"}, "C": {"item": "sp:gold_chest"}},
           "sp:diamond_chest")
    shapeless("netherite_chest", ["sp:diamond_chest", "minecraft:netherite_ingot"], "sp:netherite_chest")

    planks = {"tag": "minecraft:planks"}
    chest = {"item": "minecraft:chest"}
    shaped("drawer_1", ["PPP", "PCP", "PPP"], {"P": planks, "C": chest}, "sp:drawer_1")
    shaped("drawer_2", ["PCP", "PPP", "PCP"], {"P": planks, "C": chest}, "sp:drawer_2", 2)
    shaped("drawer_4", ["CPC", "PPP", "CPC"], {"P": planks, "C": chest}, "sp:drawer_4", 4)

    stick = {"item": "minecraft:stick"}
    for up in UPGRADES[1:4]:
        shaped(f"upgrade_{up['id']}", ["SMS", "MSM", "SMS"],
               {"S": stick, "M": {"item": up["material"]}}, f"sp:upgrade_{up['id']}")
    shaped("upgrade_netherite", ["SDS", "DND", "SDS"],
           {"S": stick, "D": {"item": "minecraft:diamond"}, "N": {"item": "minecraft:netherite_ingot"}},
           "sp:upgrade_netherite")


# ---------------------------------------------------------------- resource pack
def gen_rp():
    write_json(RP / "manifest.json", {
        "format_version": 2,
        "header": {
            "name": "Storage Plus RP",
            "description": "Textures & models for Storage Plus",
            "uuid": UUID["rp_header"], "version": VERSION, "min_engine_version": MIN_ENGINE,
        },
        "modules": [{"type": "resources", "uuid": UUID["rp_res"], "version": VERSION}],
        "dependencies": [{"uuid": UUID["bp_header"], "version": VERSION}],
    })

    write_json(RP / "models" / "entity" / "sp_chest.geo.json", {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {"identifier": "geometry.sp_chest", "texture_width": 64, "texture_height": 64,
                            "visible_bounds_width": 2, "visible_bounds_height": 2,
                            "visible_bounds_offset": [0, 0.5, 0]},
            "bones": [{"name": "root", "pivot": [0, 0, 0], "cubes": [
                {"origin": [-7, 0, -7], "size": [14, 10, 14], "uv": [0, 19]},
                {"origin": [-7, 9, -7], "size": [14, 5, 14], "uv": [0, 0]},
                {"origin": [-1, 7, -8], "size": [2, 4, 1], "uv": [0, 0]},
            ]}],
        }],
    })

    item_tex, terrain_tex = {}, {}
    for i, (tier, c) in enumerate(CHESTS.items()):
        write_json(RP / "entity" / f"{tier}_chest.entity.json", {
            "format_version": "1.10.0",
            "minecraft:client_entity": {"description": {
                "identifier": f"sp:{tier}_chest_entity",
                "materials": {"default": "entity_alphatest"},
                "textures": {"default": f"textures/entity/sp/{tier}_chest"},
                "geometry": {"default": "geometry.sp_chest"},
                "render_controllers": ["controller.render.default"],
            }},
        })
        save_png(chest_entity_texture(c, 10 + i), RP / "textures" / "entity" / "sp" / f"{tier}_chest.png")
        save_png(chest_item_icon(c, 20 + i), RP / "textures" / "items" / "sp" / f"{tier}_chest.png")
        item_tex[f"sp_{tier}_chest"] = {"textures": f"textures/items/sp/{tier}_chest"}

    write_json(RP / "entity" / "chest_vault.entity.json", {
        "format_version": "1.10.0",
        "minecraft:client_entity": {"description": {"identifier": "sp:chest_vault"}},
    })

    for i, up in enumerate(UPGRADES[1:], start=1):
        save_png(upgrade_icon(up["trim"], 30 + i), RP / "textures" / "items" / "sp" / f"upgrade_{up['id']}.png")
        item_tex[f"sp_upgrade_{up['id']}"] = {"textures": f"textures/items/sp/upgrade_{up['id']}"}

    save_png(drawer_side(40), RP / "textures" / "blocks" / "sp" / "drawer_side.png")
    terrain_tex["sp_drawer_side"] = {"textures": "textures/blocks/sp/drawer_side"}
    for slots in DRAWERS:
        for lvl, up in enumerate(UPGRADES):
            name = f"drawer_{slots}_front_{lvl}"
            save_png(drawer_front(slots, up["trim"], 50 + slots), RP / "textures" / "blocks" / "sp" / f"{name}.png")
            terrain_tex[f"sp_{name}"] = {"textures": f"textures/blocks/sp/{name}"}

    write_json(RP / "textures" / "item_texture.json", {
        "resource_pack_name": "storage_plus", "texture_name": "atlas.items", "texture_data": item_tex})
    write_json(RP / "textures" / "terrain_texture.json", {
        "resource_pack_name": "storage_plus", "texture_name": "atlas.terrain",
        "padding": 8, "num_mip_levels": 4, "texture_data": terrain_tex})
    write_json(RP / "blocks.json", {"format_version": [1, 1, 0],
                                    **{f"sp:drawer_{s}": {"sound": "wood"} for s in DRAWERS}})
    gen_lang()

    for pack in (BP, RP):
        save_png(pack_icon(), pack / "pack_icon.png", scale=8)


def gen_lang():
    texts = {"en": {}, "th": {}}

    def add(key, en, th):
        texts["en"][key] = en
        texts["th"][key] = th

    for tier, c in CHESTS.items():
        add(f"item.sp.{tier}_chest.name", c["en"], c["th"])
        add(f"entity.sp:{tier}_chest_entity.name", c["en"], c["th"])
    add("entity.sp:chest_vault.name", "Chest Storage", "ที่เก็บของหีบ")
    for slots, d in DRAWERS.items():
        add(f"tile.sp:drawer_{slots}.name", d["en"], d["th"])
    for lvl, up in enumerate(UPGRADES):
        add(f"sp.upgrade.{lvl}", up["en"] if lvl == 0 else f"{up['en']} Upgrade x{up['mult']}",
            up["th"] if lvl == 0 else f"อัปเกรด{up['th']} x{up['mult']}")
        if up["id"]:
            add(f"item.sp.upgrade_{up['id']}.name", f"{up['en']} Drawer Upgrade",
                f"อัปเกรดลิ้นชัก ({up['th']})")

    msgs = [
        ("sp.msg.cant_place", "§cCan't place the chest here", "§cวางหีบตรงนี้ไม่ได้"),
        ("sp.msg.page", "Page %s/%s", "หน้า %s/%s"),
        ("sp.msg.chest_hint", "§7Tap: open §8|§7 Hit: next page §8|§7 Sneak+Hit: previous §8|§7 Pickaxe x3: pick up",
         "§7แตะ: เปิด §8|§7 ตี: หน้าถัดไป §8|§7 ย่อ+ตี: ย้อนหน้า §8|§7 ตีด้วยอีเต้อ 3 ที: เก็บหีบ"),
        ("sp.msg.break_more", "§eHit %s more time(s) with a pickaxe to pick up the chest",
         "§eตีด้วยอีเต้ออีก %s ครั้งเพื่อเก็บหีบ"),
        ("sp.msg.unstackable", "§cUnstackable items can't go in a drawer",
         "§cไอเท็มที่ซ้อนไม่ได้ ใส่ลิ้นชักไม่ได้"),
        ("sp.msg.special", "§cItems with enchantments, names or damage can't go in a drawer",
         "§cไอเท็มที่มีเอนชานต์/ตั้งชื่อ/เสียหาย ใส่ลิ้นชักไม่ได้"),
        ("sp.msg.no_slot", "§cNo free slot for a new item type", "§cไม่มีช่องว่างสำหรับไอเท็มชนิดใหม่"),
        ("sp.msg.full", "§cThis slot is full", "§cช่องนี้เต็มแล้ว"),
        ("sp.msg.inv_full", "§cYour inventory is full", "§cกระเป๋าเต็ม"),
        ("sp.msg.empty_drawer", "§7This drawer is empty", "§7ลิ้นชักว่างเปล่า"),
        ("sp.msg.upgrade_lower", "§eThis drawer already has an equal or better upgrade",
         "§eลิ้นชักนี้มีอัปเกรดเท่ากันหรือดีกว่าอยู่แล้ว"),
        ("sp.msg.upgrade_done", "§aUpgrade installed!", "§aติดตั้งอัปเกรดแล้ว!"),
        ("sp.msg.too_full", "§cThis drawer holds %s stacks. Take items out until it has %s stacks or less before breaking it.",
         "§cลิ้นชักนี้มีของ %s สแต็ก ต้องเอาออกให้เหลือไม่เกิน %s สแต็กก่อนจึงจะทุบได้"),
        ("sp.msg.added", "§a+%s", "§a+%s"),
        ("sp.msg.taken", "§e-%s", "§e-%s"),
        ("sp.msg.empty_slot", "§8(empty, max %s stacks)", "§8(ว่าง, สูงสุด %s สแต็ก)"),
        ("sp.msg.drawer_hint", "§7Tap with item: put §8|§7 Double-tap: put all §8|§7 Empty hand: take stack §8|§7 Sneak: take 1",
         "§7แตะถือของ: ใส่ §8|§7 แตะ 2 ที: ใส่ทั้งหมด §8|§7 มือเปล่า: หยิบ 1 สแต็ก §8|§7 ย่อ: หยิบ 1 ชิ้น"),
    ]
    for k, en, th in msgs:
        add(k, en, th)

    for code, lang in (("en_US", "en"), ("th_TH", "th")):
        write_text(RP / "texts" / f"{code}.lang",
                   "".join(f"{k}={v}\t#\n" for k, v in texts[lang].items()))
    write_json(RP / "texts" / "languages.json", ["en_US", "th_TH"])


if __name__ == "__main__":
    gen_bp()
    gen_rp()
    print("generated", BP.relative_to(ROOT), "and", RP.relative_to(ROOT))
