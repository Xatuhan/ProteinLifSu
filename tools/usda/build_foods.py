# -*- coding: utf-8 -*-
"""
USDA FoodData Central verisinden uygulamanın çevrimdışı besin dosyasını üretir.

Girdi (önceden https://fdc.nal.usda.gov adresinden indirilmiş, yerel dosyalar):
  --sr      SR Legacy CSV klasörü (food.csv, food_nutrient.csv, food_portion.csv, measure_unit.csv)
  --fndds   FNDDS (Survey) JSON dosyası
  --found   Foundation Foods JSON dosyası
Çıktı:
  --out     app/src/main/assets/usda_foods.tsv

Satır biçimi (sekmeyle ayrılmış, UTF-8):
  fdcId  kaynak(S/F/N)  adEN  adTR  protein100g  lif100g(boş=veri yok)  ölçüler
Ölçüler: "gram~en~tr" öğeleri ';' ile ayrılır (en fazla 4).

Çalıştırma (izole mod, ağ erişimi yok):
  py -I tools/usda/build_foods.py --sr ... --fndds ... --found ... --out ...
"""
import argparse
import csv
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from glossary import PHRASES, WORDS  # noqa: E402

PROTEIN_NUMBERS = {"203"}
FIBER_NUMBERS = {"291"}
SR_PROTEIN_ID = "1003"
SR_FIBER_ID = "1079"
SR_WATER_ID = "1051"
WATER_NUMBERS = {"255"}

# Sıvı (çorba/içecek) tespiti: ilk virgüllü parça bu sözcüklerle başlıyorsa ve kuru/konsantre değilse.
LIQUID_HEADS = ("soup", "beverages", "milk", "buttermilk", "kefir", "tea", "coffee", "juice", "lemonade", "smoothie")
NOT_LIQUID = ("dry", "powder", "mix", "concentrate", "condensed", "dehydrated", "freeze-dried", "cube", "granules", "instant, dry")


def is_liquid(name):
    low = name.lower()
    head = low.split(",")[0].strip()
    if not (head.startswith(LIQUID_HEADS) or " juice" in head or head.endswith(" soup")):
        return False
    if "prepared with" in low or "prepared from" in low or "ready-to-serve" in low or "ready to serve" in low:
        return True
    return not any(re.search(r"b" + re.escape(x) + r"b", low) for x in NOT_LIQUID)

# Uygulamadaki elle seçilmiş 43 besinin USDA kimlikleri (çift kayıt olmasın).
CURATED_IDS = {
    171477, 172388, 173118, 174032, 175168, 173709, 171287, 171284, 171304, 171265, 170904,
    173420, 170899, 171248, 173177, 173904, 172421, 173757, 175203, 170287, 168878, 169737,
    168910, 168917, 174289, 174924, 172688, 169247, 170457, 168409, 169967, 168463, 171688,
    173944, 169118, 173941, 170567, 170187, 170581, 173806, 172470, 170554,
}

MAX_PORTIONS = 4
# Büyük harfle yazılsa da marka olmayan kısaltmalar.
ACRONYMS = {"nfs", "ns"}
TOKEN = re.compile(r"[A-Za-z]+(?:'[a-z]+)?|[^A-Za-z]+")
PHRASE_KEYS = sorted(PHRASES.keys(), key=len, reverse=True)

# Parça başı kalıpları: "with X" → "X ile" vb. (Türkçe sözdizimine yaklaşmak için).
PREFIX_RULES = [
    ("made with ", "{} ile yapılmış"),
    ("made from ", "{} ile yapılmış"),
    ("prepared with ", "{} ile hazırlanmış"),
    ("cooked with ", "{} ile pişmiş"),
    ("cooked in ", "{} içinde pişmiş"),
    ("ns as to ", "{} belirtilmemiş"),
    ("without ", "{} olmadan"),
    ("with ", "{} ile"),
    ("from ", "{} kaynaklı"),
    ("in ", "{} içinde"),
    ("on ", "{} üzerinde"),
    ("no ", "{} yok"),
    ("not ", "{} değil"),
]

TR_UPPER = {"i": "İ", "ı": "I", "ç": "Ç", "ğ": "Ğ", "ö": "Ö", "ş": "Ş", "ü": "Ü"}


def tr_capitalize(s):
    if not s:
        return s
    c = s[0]
    return TR_UPPER.get(c, c.upper()) + s[1:]


def translate_words(text):
    out = []
    for tok in TOKEN.findall(text):
        if tok[0].isalpha():
            low = tok.lower()
            # Tamamı büyük harf (≥3) ise marka/kısaltma: olduğu gibi bırak.
            if len(tok) >= 3 and tok.isupper() and low not in ACRONYMS:
                out.append(tok)
                continue
            tr = WORDS.get(low)
            if tr is None and low.endswith("s") and low[:-1] in WORDS:
                tr = WORDS[low[:-1]]
            if tr is None:
                out.append(tok)
            elif tr == "-":
                continue
            else:
                out.append(tr)
        else:
            out.append(tok)
    s = "".join(out)
    s = re.sub(r"\s+", " ", s)
    s = re.sub(r"\(\s+", "(", s)
    s = re.sub(r"\s+\)", ")", s)
    return s


def translate_segment(seg):
    raw = seg.strip()
    low = raw.lower()
    if not low:
        return ""
    if low in PHRASES:
        return PHRASES[low]
    for prefix, pattern in PREFIX_RULES:
        if low.startswith(prefix) and len(low) > len(prefix):
            inner = translate_segment(raw[len(prefix):])
            if inner:
                return pattern.format(inner)
    # Parça içindeki bilinen çok kelimeli kalıplar (en uzun önce).
    work = raw
    placeholders = []
    lw = work.lower()
    for key in PHRASE_KEYS:
        if " " not in key and "-" not in key:
            continue
        idx = lw.find(key)
        if idx >= 0:
            ph = "\u0001%d\u0001" % len(placeholders)
            placeholders.append(PHRASES[key])
            work = work[:idx] + ph + work[idx + len(key):]
            lw = work.lower()
    parts = re.split(r"(\u0001\d+\u0001)", work)
    out = []
    for p in parts:
        m = re.fullmatch(r"\u0001(\d+)\u0001", p)
        out.append(placeholders[int(m.group(1))] if m else translate_words(p))
    return re.sub(r"\s+", " ", "".join(out)).strip()


def translate_name(name):
    segs = [translate_segment(s) for s in name.split(",")]
    text = ", ".join(s for s in segs if s)
    return tr_capitalize(text)


def translate_portion(desc):
    return re.sub(r"\s+", " ", translate_segment(desc)).strip()


def clean(s):
    # Sekme/satır sonu ve ayırıcı karakterler dosya biçimini bozmasın.
    return re.sub(r"[\t\r\n;~]", " ", s).strip()


def fmt_num(v):
    if v is None:
        return ""
    v = round(float(v), 2)
    return ("%.2f" % v).rstrip("0").rstrip(".")


def nutrient_from_json(food):
    p = f = w = None
    for n in food.get("foodNutrients", []):
        nut = n.get("nutrient", {})
        num = str(nut.get("number", ""))
        amt = n.get("amount")
        if amt is None:
            continue
        if num in PROTEIN_NUMBERS:
            p = amt
        elif num in FIBER_NUMBERS:
            f = amt
        elif num in WATER_NUMBERS:
            w = amt
    return p, f, w


def portions_from_json(food):
    out = []
    for por in sorted(food.get("foodPortions", []), key=lambda x: x.get("sequenceNumber", 99)):
        g = por.get("gramWeight") or 0
        if g <= 0:
            continue
        desc = clean(por.get("portionDescription") or "")
        # FNDDS'te "Quantity not specified" ve yalnız sayısal kod olan ölçüler anlamsız: atla.
        if not desc or desc.lower().startswith("quantity not specified") or not re.search(r"[A-Za-z]", desc):
            continue
        out.append((g, desc))
        if len(out) >= MAX_PORTIONS:
            break
    return out


def load_sr(folder):
    foods = {}
    with open(os.path.join(folder, "food.csv"), encoding="utf-8") as fh:
        for r in csv.DictReader(fh):
            foods[r["fdc_id"]] = {"name": r["description"], "p": None, "f": None, "w": None, "portions": []}
    with open(os.path.join(folder, "food_nutrient.csv"), encoding="utf-8") as fh:
        for r in csv.DictReader(fh):
            food = foods.get(r["fdc_id"])
            if not food or r["amount"] == "":
                continue
            if r["nutrient_id"] == SR_PROTEIN_ID:
                food["p"] = float(r["amount"])
            elif r["nutrient_id"] == SR_FIBER_ID:
                food["f"] = float(r["amount"])
            elif r["nutrient_id"] == SR_WATER_ID:
                food["w"] = float(r["amount"])
    units = {}
    with open(os.path.join(folder, "measure_unit.csv"), encoding="utf-8") as fh:
        for r in csv.DictReader(fh):
            units[r["id"]] = r["name"]
    with open(os.path.join(folder, "food_portion.csv"), encoding="utf-8") as fh:
        rows = sorted(csv.DictReader(fh), key=lambda r: (r["fdc_id"], int(r["seq_num"] or 99)))
    for r in rows:
        food = foods.get(r["fdc_id"])
        if not food or len(food["portions"]) >= MAX_PORTIONS:
            continue
        try:
            g = float(r["gram_weight"])
        except ValueError:
            continue
        if g <= 0:
            continue
        unit = units.get(r["measure_unit_id"], "")
        if unit == "undetermined":
            unit = ""
        desc = " ".join(x for x in [fmt_num(r["amount"]) if r["amount"] else "", unit, r["portion_description"], r["modifier"]] if x)
        desc = clean(desc)
        if desc:
            food["portions"].append((g, desc))
    return [(int(k), "S", v["name"], v["p"], v["f"], v["w"], v["portions"]) for k, v in foods.items()]


def load_json(path, key, src):
    with open(path, encoding="utf-8") as fh:
        data = json.load(fh)[key]
    out = []
    for food in data:
        p, f, w = nutrient_from_json(food)
        out.append((int(food["fdcId"]), src, food["description"], p, f, w, portions_from_json(food)))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sr", required=True)
    ap.add_argument("--fndds", required=True)
    ap.add_argument("--found", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    rows = load_sr(args.sr) + load_json(args.fndds, "SurveyFoods", "N") + load_json(args.found, "FoundationFoods", "F")
    seen_ids = set()
    seen_names = set()
    kept = 0
    skipped = {"curated": 0, "no_protein": 0, "dup": 0, "bad": 0}
    lines = []
    liquid_count = 0
    for fdc, src, name, p, f, w, portions in rows:
        if fdc in CURATED_IDS:
            skipped["curated"] += 1
            continue
        if p is None:
            skipped["no_protein"] += 1
            continue
        key = name.strip().lower()
        if fdc in seen_ids or key in seen_names:
            skipped["dup"] += 1
            continue
        if not (0 <= p <= 100) or (f is not None and not (0 <= f <= 100)) or p + (f or 0) > 100:
            skipped["bad"] += 1
            continue
        seen_ids.add(fdc)
        seen_names.add(key)
        name_en = clean(name)
        name_tr = clean(translate_name(name))
        por = ";".join("%s~%s~%s" % (fmt_num(g), d, clean(translate_portion(d))) for g, d in portions[:MAX_PORTIONS])
        if w is not None and not (0 <= w <= 100):
            w = None
        liquid = w is not None and is_liquid(name)
        liquid_count += liquid
        lines.append("\t".join([str(fdc), src, name_en, name_tr, fmt_num(p), fmt_num(f), por, fmt_num(w), "1" if liquid else "0"]))
        kept += 1
    lines.sort(key=lambda l: l.split("\t")[2].lower())
    with open(args.out, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("# USDA FoodData Central (SR Legacy 2018-04, FNDDS 2021-2023, Foundation 2026-10). Türkçe adlar otomatik çeviri.\n")
        fh.write("\n".join(lines))
        fh.write("\n")
    print("yazilan", kept, "sivi", liquid_count, "atlanan", skipped, "boyut", os.path.getsize(args.out))


if __name__ == "__main__":
    main()
