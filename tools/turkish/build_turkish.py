# -*- coding: utf-8 -*-
"""
Türk yemeklerinin 100 g protein / lif / su değerlerini standart tariflerden hesaplar ve
app/src/main/java/com/proteinlifsu/domain/TurkishDishes.kt dosyasını üretir.

Girdi: USDA SR Legacy CSV klasörü (önceden https://fdc.nal.usda.gov'dan indirilmiş, yerel).
Çalıştırma (izole, ağ yok):
  py -I tools/turkish/build_turkish.py --sr <klasör> --out app/src/main/java/com/proteinlifsu/domain/TurkishDishes.kt
"""
import argparse
import csv
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from recipes import DISHES, ING  # noqa: E402

NUT = {"1003": "p", "1079": "f", "1051": "w"}

# Kategori → (porsiyon profili, sıvı mı)
PROFILES = {
    "SOUP": ({"SMALL_BOWL": 200, "MEDIUM_BOWL": 250, "LARGE_BOWL": 350}, True),
    "DRINK": ({}, True),
    "STEW": ({"SMALL_BOWL": 200, "MEDIUM_BOWL": 250, "LARGE_BOWL": 350, "MEDIUM_PLATE": 250, "LARGE_PLATE": 350}, False),
    "OLIVE": ({"SMALL_BOWL": 150, "MEDIUM_PLATE": 200, "LARGE_PLATE": 300}, False),
    "RICE": ({"SMALL_BOWL": 150, "MEDIUM_PLATE": 200, "LARGE_PLATE": 300}, False),
    "GRILL": ({"MEDIUM_PLATE": 200, "LARGE_PLATE": 300}, False),
    "PASTRY": ({}, False),
    "MEZE": ({"SMALL_BOWL": 100, "MEDIUM_BOWL": 150}, False),
    "SALAD": ({"SMALL_BOWL": 150, "MEDIUM_BOWL": 250, "MEDIUM_PLATE": 200}, False),
    "DESSERT": ({}, False),
}

PIECE_UNITS = {
    "ONE_SERVING": "SERVING", "PIECE": "PIECE", "SLICE": "SLICE", "GLASS": "GLASS",
    "GLASS_TEA": "TEA_GLASS", "CUP_COFFEE": "COFFEE_CUP", "MUG": "MUG", "BOWL_SMALL": "BOWL",
    "PIDE": "PIDE", "MEATBALL_SERVING": "MEATBALL_SERVING",
}


def load_nutrients(folder):
    wanted = {str(v) for v in ING.values()}
    data = {}
    with open(os.path.join(folder, "food_nutrient.csv"), encoding="utf-8") as fh:
        for r in csv.DictReader(fh):
            if r["fdc_id"] in wanted and r["nutrient_id"] in NUT and r["amount"] != "":
                data.setdefault(int(r["fdc_id"]), {})[NUT[r["nutrient_id"]]] = float(r["amount"])
    return data


def compute(dish, nut):
    did, tr, en, cat, yld, ings, piece_g, piece_unit = dish
    raw = p = f = dm = 0.0
    fiber_known = True
    for key, g in ings:
        fdc = ING[key]
        n = nut.get(fdc)
        if n is None or "p" not in n or "w" not in n:
            raise SystemExit(f"{did}: {key} ({fdc}) için USDA verisi eksik")
        if "f" not in n:
            fiber_known = False
        raw += g
        p += n["p"] * g / 100
        f += n.get("f", 0.0) * g / 100
        dm += (100 - n["w"]) * g / 100
    cooked = raw * yld
    if cooked < dm:
        raise SystemExit(f"{did}: pişmiş ağırlık ({cooked:.0f}) kuru maddeden ({dm:.0f}) küçük — verim hatalı")
    water = cooked - dm
    res = {
        "p": p / cooked * 100, "f": f / cooked * 100, "w": water / cooked * 100,
        "raw": raw, "cooked": cooked, "fiber_known": fiber_known,
    }
    if not (0 <= res["w"] <= 99.9) or res["p"] + res["f"] > 100:
        raise SystemExit(f"{did}: sınır dışı sonuç {res}")
    return res


def kstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def fnum(v):
    return ("%.2f" % v).rstrip("0").rstrip(".") + "f"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sr", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()
    nut = load_nutrients(args.sr)
    ids = set()
    rows = []
    print("%-34s %6s %6s %6s %7s" % ("yemek", "P/100", "L/100", "Su%", "pişmiş"))
    for d in DISHES:
        did, tr, en, cat, yld, ings, piece_g, piece_unit = d
        if did in ids:
            raise SystemExit("yinelenen kimlik " + did)
        ids.add(did)
        r = compute(d, nut)
        print("%-34s %6.2f %6.2f %6.1f %7.0f" % (tr[:34], r["p"], r["f"], r["w"], r["cooked"]))
        portions, liquid = PROFILES[cat]
        portions = dict(portions)
        unit = "SERVING"
        if piece_g:
            portions["PIECE"] = piece_g
            unit = PIECE_UNITS[piece_unit]
        if not portions and cat == "DESSERT":
            portions = {"SMALL_BOWL": 150}  # helva vb. kaseyle servis edilir
        if not portions:
            raise SystemExit(did + ": porsiyon yok")
        rows.append((did, tr, en, cat, r, portions, unit, liquid, ings))

    lines = []
    lines.append("package com.proteinlifsu.domain")
    lines.append("")
    lines.append("// BU DOSYA OTOMATİK ÜRETİLDİ: tools/turkish/build_turkish.py (elle düzenleme).")
    lines.append("// Değerler standart tariflerden, USDA SR Legacy malzeme verisiyle hesaplanmıştır (yaklaşık).")
    lines.append("")
    lines.append("object TurkishDishes {")
    lines.append("    private const val NOTE_TR = \"Standart tarifle hesaplandı (malzemeler: USDA)\"")
    lines.append("    private const val NOTE_EN = \"Calculated from a standard recipe (USDA ingredients)\"")
    lines.append("")
    lines.append("    val foods: List<Food> = listOf(")
    for did, tr, en, cat, r, portions, unit, liquid, ings in rows:
        pm = ", ".join("PortionType.%s to %d" % (k, v) for k, v in portions.items())
        lines.append("        Food(")
        lines.append("            id = %s, nameTr = %s, nameEn = %s," % (kstr(did), kstr(tr), kstr(en)))
        lines.append("            proteinPer100g = %s, fiberPer100g = %s, waterPer100g = %s, liquid = %s," % (
            fnum(r["p"]), fnum(r["f"]), fnum(r["w"]), "true" if liquid else "false"))
        lines.append("            portions = mapOf(%s), piece = PieceUnit.%s," % (pm, unit))
        lines.append("            noteTr = NOTE_TR, noteEn = NOTE_EN, fiberKnown = %s," % ("true" if r["fiber_known"] else "false"))
        lines.append("        ),")
    lines.append("    )")
    lines.append("}")
    lines.append("")
    with open(args.out, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines))
    print("yazilan", len(rows), "yemek ->", args.out)


if __name__ == "__main__":
    main()
