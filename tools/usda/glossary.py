# -*- coding: utf-8 -*-
"""
USDA besin adları için İngilizce → Türkçe sözlük (otomatik çeviri).

Yöntem: USDA adları virgülle ayrılmış parçalardan oluşur ("Chicken, breast, meat only,
roasted"). Her parça önce PHRASES'te (çok kelimeli kalıplar) aranır, yoksa kelime kelime
WORDS ile çevrilir. Sözlükte olmayan kelimeler (özel adlar, markalar, yöresel yemekler)
olduğu gibi kalır. Uygulama Türkçe adın altında her zaman özgün İngilizce adı da gösterir.
"""

# Çok kelimeli kalıplar (küçük harf, tam parça eşleşmesi ya da parça içinde geçiş).
PHRASES_RAW = """
separable lean and fat|yağlı ve yağsız kısım
separable lean only|yalnız yağsız kısım
separable fat|ayrılabilir yağ
trimmed to 0" fat|yağı tamamen alınmış
trimmed to 1/8" fat|yağı 3 mm bırakılmış
trimmed to 1/4" fat|yağı 6 mm bırakılmış
all grades|tüm sınıflar
all classes|tüm sınıflar
all types|tüm türler
all varieties|tüm çeşitler
without salt added|tuz eklenmemiş
without added salt|tuz eklenmemiş
without salt|tuzsuz
with salt added|tuz eklenmiş
with salt|tuzlu
no salt added|tuz eklenmemiş
salt added in processing|işlemde tuz eklenmiş
salt not added in processing|işlemde tuz eklenmemiş
lightly salted|az tuzlu
new zealand|Yeni Zelanda
new england|New England
no added fat|yağ eklenmeden
fat added|yağ eklenmiş
fat free|yağsız
nonfat or fat free|yağsız
low fat|az yağlı
reduced fat|yağı azaltılmış
reduced fat (2%)|yağı azaltılmış (%2)
reduced sodium|sodyumu azaltılmış
low sodium|düşük sodyumlu
reduced sugar|şekeri azaltılmış
sugar free|şekersiz
no sugar added|şeker eklenmemiş
without added sugar|şeker eklenmemiş
with added sugar|şeker eklenmiş
low calorie|düşük kalorili
reduced calorie|kalorisi azaltılmış
infant formula|bebek maması (formül)
toddler formula|çocuk devam sütü
baby toddler cereal|bebek/çocuk tahıl maması
baby toddler juice|bebek/çocuk meyve suyu
meat only|yalnız et
meat and skin|et ve deri
meat and skin with breading|et ve deri, panelenmiş
meat and skin and giblets and neck|et, deri, sakatat ve boyun
skin only|yalnız deri
with skin|derili
without skin|derisiz
includes skin|deri dahil
flesh and skin|et ve deri
skin eaten|derisi yenmiş
skin not eaten|derisi yenmemiş
skin / coating eaten|deri/kaplama yenmiş
skin / coating not eaten|deri/kaplama yenmemiş
skin and breading removed|deri ve pane çıkarılmış
peel eaten|kabuğu yenmiş
peel not eaten|kabuğu yenmemiş
cooked in skin|kabuğuyla pişmiş
mature seeds|olgun tane
immature seeds|taze tane
dry mix|kuru karışım
from dry mix|kuru karışımdan
broilers or fryers|piliç
broiler or fryers|piliç
solids and liquids|katı ve sıvı
drained solids|süzülmüş katı
drained and rinsed|süzülüp yıkanmış
cereals ready-to-eat|hazır kahvaltılık gevrek
ready-to-eat|yemeye hazır
ready to eat|yemeye hazır
ready to serve|servise hazır
ready to drink|içmeye hazır
ready to feed|beslemeye hazır
ready-to-serve|servise hazır
variety meats and by-products|sakatat ve yan ürünler
fast foods|fast food
fast food|fast food
from fast food|fast food'dan
from fast food / restaurant|fast food / restoran
fast food / restaurant|fast food / restoran
from restaurant|restorandan
from restaurant or fast food|restoran veya fast food
whole grain|tam tahıllı
whole grain white|tam tahıllı beyaz
whole wheat|tam buğday
wheat flour|buğday unu
corn flour|mısır unu
egg omelet or scrambled egg|omlet veya çırpılmış yumurta
egg white omelet|yumurta akı omleti
egg white|yumurta akı
egg salad|yumurta salatası
egg substitute|yumurta ikamesi
ns as to fat|yağ belirtilmemiş
ns as to fat type|yağ türü belirtilmemiş
ns as to fat eaten|yağın yenip yenmediği belirtilmemiş
ns as to form|biçim belirtilmemiş
ns as to part|parça belirtilmemiş
ns as to type|tür belirtilmemiş
ns as to type of meat|et türü belirtilmemiş
ns as to type of milk|süt türü belirtilmemiş
ns as to cooking method|pişirme yöntemi belirtilmemiş
ns as to skin eaten|derinin yenip yenmediği belirtilmemiş
ns as to major flour|ana un belirtilmemiş
dry heat|kuru ısıda
moist heat|nemli ısıda
mixed species|karışık türler
alcoholic beverage|alkollü içecek
salad dressing|salata sosu
with added solution|çözelti eklenmiş
game meat|av eti
commercially prepared|hazır (ticari)
puerto rican style|Porto Riko usulü
baked or broiled|fırında veya ızgarada
or fried|veya kızartılmış
or roasted|veya kavrulmuş
and vegetables including carrots|ve havuç dahil sebzeler
and vegetables excluding carrots|ve havuç hariç sebzeler
composite of trimmed retail cuts|yağı alınmış perakende parçaların karışımı
composite of cuts|parçaların karışımı
retail parts|perakende parçalar
short loin|kısa bel
short ribs|kısa kaburga
prepared with water|suyla hazırlanmış
prepared from recipe|tarifle hazırlanmış
prepared with equal volume water|eşit hacim suyla hazırlanmış
prepared with equal volume milk|eşit hacim sütle hazırlanmış
prepared with whole milk|tam yağlı sütle hazırlanmış
prepared with 2% milk|%2 sütle hazırlanmış
prepared with calcium sulfate|kalsiyum sülfatla hazırlanmış
made with water|suyla yapılmış
made with milk|sütle yapılmış
made with butter|tereyağıyla yapılmış
made with margarine|margarinle yapılmış
made with oil|yağla yapılmış
made with mayonnaise|mayonezle yapılmış
made with light mayonnaise|light mayonezle yapılmış
made with non-dairy milk|bitkisel sütle yapılmış
made with low fat (2%) milk|az yağlı (%2) sütle yapılmış
made with tap water|musluk suyuyla yapılmış
made with bottled water|şişe suyuyla yapılmış
made with baby water|bebek suyuyla yapılmış
made with italian dressing|İtalyan sosuyla yapılmış
made with creamy dressing|kremalı sosla yapılmış
made with light creamy dressing|light kremalı sosla yapılmış
made with light italian dressing|light İtalyan sosuyla yapılmış
made with any type of fat free dressing|yağsız sosla yapılmış
made with mayonnaise-type salad dressing|mayonez tipi sosla yapılmış
made with light mayonnaise-type salad dressing|light mayonez tipi sosla yapılmış
made from dried potatoes|kuru patatesten yapılmış
chicken or turkey|tavuk veya hindi
chicken and/or turkey|tavuk ve/veya hindi
chicken or turkey salad|tavuk veya hindi salatası
potato chips|patates cipsi
tortilla chips|tortilla cipsi
not reconstituted|sulandırılmamış
and added vegetables|ve eklenmiş sebzeler
home recipe|ev tarifi
home style|ev usulü
with meat|etli
no meat|etsiz
dark meat|koyu et
light meat|beyaz et
light or dark meat|beyaz veya koyu et
peanut butter|fıstık ezmesi
peanut butter sandwich|fıstık ezmeli sandviç
peanut butter and jelly sandwich|fıstık ezmeli ve reçelli sandviç
from fresh|tazeden
from frozen|donmuştan
from canned|konserveden
from dried|kurudan
from raw|çiğden
from concentrate|konsantreden
from pre-cooked|önceden pişmişten
fried chicken|kızarmış tavuk
cooked with oil|yağla pişmiş
cooked with butter or margarine|tereyağı veya margarinle pişmiş
cooked with water|suyla pişmiş
bottom round|alt but
top round|üst but
top round steak|üst but biftek
top round roast|üst but rosto
eye of round|but göbeği
eye of round roast|but göbeği rosto
eye of round steak|but göbeği biftek
tip round|but ucu
outside round|dış but
with vegetables|sebzeli
with vegetables and gravy|sebzeli ve soslu
thin crust|ince hamur
thick crust|kalın hamur
medium crust|orta hamur
regular crust|normal hamur
pan crust|tava hamuru
stuffed crust|dolgulu hamur
rising crust|kabaran hamur
hand-tossed crust|elde açılmış hamur
pie crust|turta hamuru
chicken breast|tavuk göğsü
chicken thigh|tavuk but
chicken drumstick|tavuk baget
chicken wing|tavuk kanat
chicken leg|tavuk bacak
chicken fillet sandwich|tavuk fileto sandviç
chicken patty|tavuk köftesi
chicken nuggets|tavuk nugget
chicken tenders or strips|tavuk şerit
chicken noodle|tavuklu şehriye
chicken broth|tavuk suyu
beef broth|et suyu
with iron|demir eklenmiş
no sauce|sossuz
low fat milk|az yağlı süt
whole milk|tam yağlı süt
nonfat milk|yağsız süt
chocolate milk|kakaolu süt
strawberry milk|çilekli süt
almond milk|badem sütü
coconut milk|hindistan cevizi sütü
coconut cream|hindistan cevizi kreması
coconut meat|hindistan cevizi içi
sweet potato|tatlı patates
from school lunch|okul yemeğinden
school lunch|okul yemeği
with beans|fasulyeli
with beans and rice|fasulyeli ve pilavlı
with rice|pilavlı
with egg|yumurtalı
with ham|jambonlu
with sausage|sosisli
with nuts|kuruyemişli
with peanuts|yer fıstıklı
without peanuts|yer fıstıksız
with raisins|kuru üzümlü
with fruit|meyveli
with bananas|muzlu
with corn|mısırlı
with peas|bezelyeli
with peas and carrots|bezelye ve havuçlu
with carrots|havuçlu
with butter|tereyağlı
with gravy|soslu
with cheese|peynirli
with cheese and meat|peynirli ve etli
no cheese|peynirsiz
cheese only|yalnız peynir
with whipped cream|krem şantili
with cream sauce|krema soslu
with tomato sauce|domates soslu
with tomato-based sauce|domates bazlı soslu
tomato-based sauce|domates bazlı sos
soy-based sauce|soya bazlı sos
with soy-based sauce|soya soslu
cheese sauce|peynir sosu
white sauce|beyaz sos
or mushroom sauce|veya mantar sosu
on wheat bun|buğday ekmeği arasında
on white bun|beyaz ekmek arasında
on white bread|beyaz ekmek arasında
on wheat bread|buğday ekmeği arasında
no dressing|sossuz
ice creams|dondurmalar
ice cream|dondurma
light ice cream|light dondurma
ice cream cone|külah dondurma
ice cream bar|çubuk dondurma
ice cream sandwich|sandviç dondurma
soft serve|yumuşak dondurma
frozen novelties|dondurulmuş tatlılar
frozen yogurts|dondurulmuş yoğurtlar
cake or cupcake|kek veya cupcake
gluten free|glutensiz
top loin|üst bel
top loin steak|üst bel biftek
top loin (chops)|üst bel (pirzola)
top sirloin|üst sırt
top sirloin cap steak|üst sırt kapak biftek
bottom sirloin|alt sırt
green beans|taze fasulye
lima beans|lima fasulyesi
black beans|siyah fasulye
pinto beans|barbunya
kidney beans|kırmızı fasulye
white beans|kuru fasulye
mung beans|maş fasulyesi
refried beans|fasulye ezmesi
winged beans|kanatlı fasulye
soft drink|gazlı içecek
energy drink|enerji içeceği
dry roasted|kuru kavrulmuş
oil roasted|yağda kavrulmuş
honey roasted|ballı kavrulmuş
lip off|kenarı alınmış
flat half|düz yarım
point half|uç yarım
rib eye steak|antrikot biftek
rib eye roast|antrikot rosto
ribeye cap steak|antrikot kapak biftek
plate steak|döş biftek
french fries|patates kızartması
french fried|kızartılmış
french toast|yumurtalı ekmek (french toast)
french toast sticks|french toast çubukları
french dressing|Fransız sosu
italian dressing|İtalyan sosu
ranch dressing|ranch sos
thousand island dressing|thousand island sos
blue or roquefort cheese dressing|rokfor peynirli sos
and/or dark-green leafy; no potatoes|ve/veya koyu yeşil yapraklı; patatessiz
and dark-green leafy; no potatoes|ve koyu yeşil yapraklı; patatessiz
chocolate chip|damla çikolatalı
chocolate candy|çikolata şekerleme
chocolate coated|çikolata kaplı
chocolate covered|çikolata kaplı
milk chocolate|sütlü çikolata
baking chocolate|pasta çikolatası
hot chocolate / cocoa|sıcak çikolata / kakao
cocoa mix|kakao karışımı
ham with natural juices|kendi suyunda jambon
shoulder clod|kürek
hot dog|sosisli
hot dog sandwich|sosisli sandviç
hash brown|patates rösti
orange juice|portakal suyu
grapefruit juice|greyfurt suyu
apple juice|elma suyu
grape juice|üzüm suyu
pineapple juice|ananas suyu
tomato juice|domates suyu
lime juice|misket limonu suyu
cranberry juice cocktail|kızılcık suyu kokteyli
100% juice|%100 meyve suyu
fruit juice drink|meyve suyu içeceği
fruit flavored drink|meyve aromalı içecek
fruit flavored|meyve aromalı
fruit salad|meyve salatası
fruit cocktail|meyve kokteyli
fruit smoothie|meyveli smoothie
fruit juice smoothie|meyve sulu smoothie
fruit and vegetable smoothie|meyve ve sebze smoothie
regular pack|normal paket
juice pack|meyve suyunda
water pack|suda
syrup pack|şurupta
heavy syrup pack|yoğun şurupta
heavy syrup|yoğun şurup
light syrup pack|hafif şurupta
extra heavy syrup pack|çok yoğun şurupta
vacuum pack|vakumlu
as ingredient|malzeme olarak
refrigerated dough|soğutulmuş hamur
porterhouse steak|porterhouse biftek
t-bone steak|t-bone biftek
oat bran|yulaf kepeği
wheat bran|buğday kepeği
wheat germ|buğday ruşeymi
frozen meal|dondurulmuş hazır yemek
diet frozen meal|diyet dondurulmuş yemek
frozen entree|dondurulmuş ana yemek
canned entree|konserve ana yemek
green peas|bezelye
mixed nuts|karışık kuruyemiş
mock tender steak|kürek biftek
whole (arm and blade)|bütün (kol ve kürek)
with fruit|meyveli
canned or bottled|konserve veya şişe
canned in oil|yağda konserve
canned in water|suda konserve
luncheon meat|şarküteri eti
lunchmeat|şarküteri eti
stage 1|1. aşama
stage 2|2. aşama
arm pot roast|kol rosto
chuck eye roast|kürek göbeği rosto
chuck eye steak|kürek göbeği biftek
chuck for stew|güveçlik kürek
from whole bird|bütün kuştan
cream of wheat|irmik lapası
cream of chicken|tavuk kreması (çorba)
cream of mushroom|mantar kreması (çorba)
pasta with tomato-based sauce|domates soslu makarna
pasta with cream sauce|krema soslu makarna
granola bars|granola barlar
granola bar|granola bar
cereal or granola bar|tahıl veya granola bar
with added calcium|kalsiyum eklenmiş
with calcium added|kalsiyum eklenmiş
with high vitamin c|yüksek C vitaminli
with added vitamin c|C vitamini eklenmiş
with added vitamin d|D vitamini eklenmiş
with added vitamin a and vitamin d|A ve D vitamini eklenmiş
without added vitamin a and vitamin d|A ve D vitamini eklenmemiş
fortified with vitamin d|D vitaminiyle zenginleştirilmiş
vitamins a and d|A ve D vitaminleri
with added nutrients|besin öğesi eklenmiş
with added ascorbic acid|askorbik asit eklenmiş
without added ascorbic acid|askorbik asit eklenmemiş
without ascorbic acid|askorbik asitsiz
nutritional drink or shake|besin içeceği veya shake
nutritional powder mix|besin tozu karışımı
liquid concentrate|sıvı konsantre
frozen concentrate|dondurulmuş konsantre
flavors other than chocolate|çikolata dışı aromalar
flavors other than fruit|meyve dışı aromalar
mixed flavors|karışık aromalar
shank half|incik tarafı yarım
sirloin half|sırt tarafı yarım
whole (shank and sirloin)|bütün (incik ve sırt)
fast roasted|hızlı kızartılmış
fast fried|hızlı kızartılmış
slow roasted|yavaş pişirilmiş
formulated bar|formüle bar
wheat or cracked wheat|buğday veya kırık buğday
classic mixed vegetables|klasik karışık sebze
brown rice|esmer pirinç
spanish rice|İspanyol pilavı
brussels sprouts|Brüksel lahanası
family style|aile usulü
vegetable oil spread|bitkisel yağ sürülebilir
margarine-like spread|margarin benzeri sürülebilir
eye of round|but göbeği
tenderloin steak|bonfile biftek
tenderloin roast|bonfile rosto
tenderloin steak/roast|bonfile biftek/rosto
instant oatmeal|hazır yulaf lapası
summer squash|yaz kabağı
shelf stable|raf ömürlü
leg (ham)|but (jambon)
leg (top round)|but (üst but)
center loin (chops)|orta bel (pirzola)
center rib (chops)|orta kaburga (pirzola)
sirloin (chops)|sırt (pirzola)
blade (chops)|kürek (pirzola)
blade (steaks)|kürek (biftek)
country-style ribs|köy usulü kaburga
turnip greens|şalgam yaprağı
mustard greens|hardal yaprağı
beet greens|pancar yaprağı
dandelion greens|karahindiba yaprağı
under blade center steak|kürek altı biftek
denver cut|denver kesim
shoulder top blade steak|kürek üstü biftek
whole (ribs 6-12)|bütün (6-12. kaburga)
small end (ribs 10-12)|ince uç (10-12. kaburga)
large end (ribs 6-9)|kalın uç (6-9. kaburga)
tri-tip roast|tri-tip rosto
top blade|kürek üstü
white (industrial)|beyaz (endüstriyel)
potato salad|patates salatası
potato salad with egg|yumurtalı patates salatası
tuna salad|ton balığı salatası
macaroni or pasta salad|makarna salatası
peas and carrots|bezelye ve havuç
cheese topping|peynir sosu
pepperoni topping|pepperoni
sausage topping|sosis
pork and beef|domuz ve dana
beef and pork|dana ve domuz
beef or pork|dana veya domuz
inside skirt|iç döş
outside skirt|dış döş
back ribs|sırt kaburgası
coffee creamer|kahve kreması
cream substitute|krema ikamesi
grilled without sauce|sossuz ızgara
grilled with sauce|soslu ızgara
frozen coffee drink|buzlu kahve içeceği
frozen mocha coffee drink|buzlu mocha
iced latte|buzlu latte
cafe mocha|kafe mocha
iced cafe mocha|buzlu kafe mocha
wild caught|doğadan avlanmış
prepackaged or deli|paketli veya şarküteri
fish sandwich|balık sandviç
submarine sandwich|sandviç (sub)
cheese sandwich|peynirli sandviç
grilled cheese sandwich|kaşarlı tost
pizza chain|pizza zinciri
double cheeseburger|duble çizburger
double hamburger|duble hamburger
original seasoning|orijinal baharat
original recipe|orijinal tarif
extra crispy|ekstra çıtır
with creme filling|krema dolgulu
pre-sweetened with sugar|şekerle tatlandırılmış
pre-sweetened with low calorie sweetener|düşük kalorili tatlandırıcılı
regular or quick|normal veya hızlı pişen
regular and quick|normal ve hızlı pişen
yellow or green|sarı veya yeşil
with aspartame|aspartamlı
macaroni and cheese|peynirli makarna
macaroni or noodles with cheese|peynirli makarna veya erişte
breaded and fried|panelenmiş ve kızartılmış
meat and poultry|et ve kanatlı
tomato products|domates ürünleri
sugar substitute|şeker ikamesi
high protein|yüksek proteinli
pork sausage|domuz sosisi
danish pastry|Danimarka çöreği
chicken nuggets|tavuk nugget
corn grits|mısır irmiği
salad or cooking|salata veya yemeklik
sodium added|sodyum eklenmiş
rice cakes|pirinç patlağı
burrito bowl|burrito kasesi
pizza with pepperoni|pepperonili pizza
pizza with meat other than pepperoni|pepperoni dışı etli pizza
pizza with meat and vegetables|etli ve sebzeli pizza
white pizza|beyaz pizza
chickpeas (garbanzo beans|nohut (garbanzo
bengal gram)|bengal nohudu)
1% milkfat|%1 süt yağı
2% milkfat|%2 süt yağı
2% fat|%2 yağ
80% fat|%80 yağ
60% fat|%60 yağ
arm picnic|kol (picnic)
(boston butt)|(boston kürek)
garlic bread|sarımsaklı ekmek
as purchased|satın alındığı gibi
(corn and limas)|(mısır ve lima fasulyesi)
sunflower seed kernels|ay çekirdeği içi
blade roast|kürek rosto
clod roast|kürek rosto
onion rings|soğan halkası
cheese spread|sürülebilir peynir
pistachio nuts|antep fıstığı
cashew nuts|kaju
macadamia nuts|makadamya
sour cream|ekşi krema
clam chowder|istiridye çorbası (chowder)
chocolate sandwich|çikolatalı sandviç
plain or buttermilk|sade veya ayranlı
milk shake|milkshake
other vegetables|diğer sebzeler
with other vegetables|diğer sebzelerle
dark green vegetables|koyu yeşil sebzeler
with dark green vegetables|koyu yeşil sebzeli
and dark-green vegetables|ve koyu yeşil sebzeler
reduced calorie and/or high fiber|kalorisi azaltılmış ve/veya yüksek lifli
cheese and lettuce|peynir ve marul
from kid's menu|çocuk menüsünden
strawberry banana|çilekli muzlu
pan dulce|tatlı ekmek (pan dulce)
ice type|buzlu
crookneck and straightneck|eğri ve düz boyunlu
gelatin desserts|jöleli tatlılar
salisbury steak with gravy|soslu Salisbury köfte
coconut milk|hindistan cevizi sütü
smart soup|smart çorba
leavening agents|kabartma maddeleri
breakfast type|kahvaltılık
graham crackers|graham kraker
french or vienna|Fransız veya Viyana
lean and fat eaten|yağlı ve yağsız kısım yenmiş
lean only eaten|yalnız yağsız kısım yenmiş
no noodles|eriştesiz
corn tortilla|mısır tortilla
flour tortilla|un tortilla
turnover or hot pocket|turnover veya hot pocket
potato tots|patates topları
cream puff|krem şantili profiterol
custard or cream filled|muhallebi veya krema dolgulu
ready-to-bake or -fry|pişirmeye hazır
trail mix|kuruyemiş karışımı
table blends|sofralık karışımlar
free range|serbest gezen
dehydrated (low-moisture)|kurutulmuş (düşük nem)
center slice|orta dilim
(globe or french)|(enginar)
balsam-pear (bitter gourd)|acı kavun
leafy tips|yaprak uçları
kernels on cob|koçanda tane
kernels cut off cob|koçandan ayrılmış tane
whole kernel|bütün tane
cowpeas (blackeyes)|börülce
dried (desiccated)|kurutulmuş
sesame butter|tahin
full cut|tam kesim
shoulder steak|kürek biftek
sea lion|deniz aslanı
par fried|ön kızartılmış
under blade pot roast|kürek altı rosto
under blade pot roast or steak|kürek altı rosto veya biftek
shoulder pot roast or steak|kürek rosto veya biftek
shoulder pot roast|kürek rosto
under blade steak|kürek altı biftek
lasagna with meat|kıymalı lazanya
dry fortified|kuru, zenginleştirilmiş
half and half|yarım yağlı krema (half and half)
90% lean meat / 10% fat|%90 yağsız et / %10 yağ
80% lean meat / 20% fat|%80 yağsız et / %20 yağ
70% lean meat / 30% fat|%70 yağsız et / %30 yağ
95% lean meat / 5% fat|%95 yağsız et / %5 yağ
85% lean meat / 15% fat|%85 yağsız et / %15 yağ
75% lean meat / 25% fat|%75 yağsız et / %25 yağ
peanut butter filled|fıstık ezmesi dolgulu
with peanut butter filling|fıstık ezmesi dolgulu
with regular peanut butter|normal fıstık ezmeli
large patty; with condiments|büyük köfte; soslarla
tip side|uç tarafı
shoulder tender|kürek bonfilesi
shoulder top and center steaks|kürek üstü ve orta biftekler
tip center|uç ortası
fish oil|balık yağı
1" steak|2,5 cm biftek
skin (drumsticks and thighs)|deri (baget ve but)
shredded wheat|rendelenmiş buğday gevreği
top loin petite roast|üst bel küçük rosto
top loin filet|üst bel fileto
malted drink mix|maltlı içecek karışımı
great northern|great northern fasulye
waffle cone|waffle külah
cream cheese|krem peynir
from other sources|diğer kaynaklardan
excluding potatoes|patates hariç
excluding citrus fruits|turunçgiller hariç
1 small patty|1 küçük köfte
1 medium patty|1 orta köfte
1 large patty|1 büyük köfte
2 small patties|2 küçük köfte
2 medium patties|2 orta köfte
2 large patties|2 büyük köfte
no bake|pişirmesiz
stuffed shells|doldurulmuş deniz kabuğu makarna
lo mein|lo mein erişte
sushi roll|suşi rulo
cheese and/or cream based sauce|peynir ve/veya krema bazlı sos
with cheese and/or cream based sauce|peynir ve/veya krema soslu
pre-lightened and pre-sweetened with sugar|sütlü ve şekerli
pre-lightened and pre-sweetened with low calorie sweetener|sütlü ve tatlandırıcılı
baked snack crackers|fırınlanmış kraker
spaghetti with meat sauce|kıymalı spagetti
diluted with 3 volume water|3 hacim suyla seyreltilmiş
extra lean and regular|ekstra yağsız ve normal
frozen yogurts|dondurulmuş yoğurt
bamboo shoots|bambu filizi
almond butter|badem ezmesi
dry powder|kuru toz
rice and vermicelli mix|pirinç ve şehriye karışımı
chinese restaurant|Çin restoranı
corned beef|konserve dana (corned beef)
with applesauce and bananas|elma püreli ve muzlu
barbecue flavored|barbekü aromalı
real medleys|real medleys
external fat|dış yağ
seam fat|ara yağ
standard snack-type|standart atıştırmalık
milk based|süt bazlı
with ham|jambonlu
ice cream sandwich|sandviç dondurma
raw (alaska native)|çiğ (Alaska yerli)
dried (alaska native)|kurutulmuş (Alaska yerli)
liver (alaska native)|ciğer (Alaska yerli)
meat (alaska native)|et (Alaska yerli)
(includes foods for usda's food distribution program)|(USDA gıda dağıtım programı dahil)
includes foods for usda's food distribution program|USDA gıda dağıtım programı dahil
"""

# Tek kelimeler (küçük harf). Sözlükte olmayanlar olduğu gibi kalır.
WORDS_RAW = """
a|bir
fat|yağ
cooked|pişmiş
raw|çiğ
to|-
separable|ayrılabilir
beef|dana eti
or|veya
and|ve
trimmed|yağı alınmış
meat|et
meats|etler
only|yalnız
chicken|tavuk
added|eklenmiş
cheese|peynir
canned|konserve
sauce|sos
sauces|soslar
salt|tuz
salted|tuzlu
unsalted|tuzsuz
frozen|dondurulmuş
pork|domuz eti
from|-
boneless|kemiksiz
white|beyaz
roasted|kavrulmuş
roast|rosto
roasts|rostolar
vegetables|sebzeler
vegetable|sebze
steak|biftek
steaks|biftekler
dry|kuru
no|-
skin|deri
skinless|derisiz
fresh|taze
freshly|taze
loin|bel
egg|yumurta
eggs|yumurta
without|-
prepared|hazırlanmış
ready|hazır
boiled|haşlanmış
beans|fasulye
bean|fasulye
turkey|hindi
round|but
all|tüm
fried|kızartılmış
drained|süzülmüş
sandwich|sandviç
sandwiches|sandviçler
lamb|kuzu eti
as|olarak
bread|ekmek
water|su
made|yapılmış
on|üzerinde
imported|ithal
reduced|azaltılmış
beverages|içecekler
beverage|içecek
tomato|domates
tomatoes|domates
in|içinde
drink|içecek
drinks|içecekler
broiled|ızgara
seeds|tohum
seed|tohum
potato|patates
potatoes|patates
salad|salata
salads|salatalar
ns|belirtilmemiş
nfs|belirtilmemiş
carrots|havuç
carrot|havuç
oil|yağ
oils|yağlar
grades|sınıflar
grade|sınıf
braised|buğulama
babyfood|bebek maması
top|üst
based|bazlı
dressing|sos
eaten|yenmiş
grilled|ızgara
type|tür
types|türler
restaurant|restoran
of|-
ham|jambon
low|düşük
sugar|şeker
sugars|şekerler
broccoli|brokoli
plain|sade
baked|fırında
powder|toz
food|gıda
foods|gıdalar
bone|kemik
bones|kemikler
new|yeni
cereals|tahıllar
cereal|tahıl gevreği
shoulder|kürek
regular|normal
rib|kaburga
ribs|kaburga
solids|katılar
sodium|sodyum
formula|formül mama
dried|kurutulmuş
crust|hamur
pasta|makarna
infant|bebek
not|değil
chuck|kürek
flour|un
flours|unlar
coffee|kahve
leafy|yapraklı
blade|kürek
snacks|atıştırmalıklar
snack|atıştırmalık
mature|olgun
eat|yemek
heat|ısı
style|usulü
flavored|aromalı
flavor|aroma
flavors|aromalar
brown|esmer
includes|dahil
including|dahil
mixed|karışık
leg|but
legs|but
sausage|sosis
vitamin|vitamin
vitamins|vitaminler
instant|hazır
ground|kıyma
noodles|erişte
noodle|erişte
cake|kek
cakes|kekler
crackers|kraker
cracker|kraker
fryers|piliç
vanilla|vanilyalı
cured|tuzlanmış
soft|yumuşak
sirloin|sırt
broilers|piliç
broiler|piliç
veal|dana (buzağı)
toddler|çocuk
toddlers|çocuk
margarine|margarin
for|için
liquids|sıvılar
liquid|sıvı
cookie|kurabiye
cookies|kurabiyeler
gravy|et sosu
ice|buz
omelet|omlet
scrambled|çırpılmış
variety|çeşit
by|-
coated|kaplı
breast|göğüs
french|Fransız
other|diğer
home|ev
pack|paket
pan|tava
excluding|hariç
yellow|sarı
drumstick|baget
drumsticks|baget
unprepared|hazırlanmamış
syrup|şurup
syrups|şuruplar
enriched|zenginleştirilmiş
calorie|kalori
calories|kalori
peas|bezelye
pea|bezelye
tea|çay
sweetened|tatlandırılmış
unsweetened|şekersiz
pie|turta
pies|turtalar
filled|dolgulu
thigh|but
thighs|but
squash|kabak
nonfat|yağsız
toasted|kızarmış
serve|servis
pre|ön
bun|sandviç ekmeği
buns|sandviç ekmeği
decaffeinated|kafeinsiz
tenderloin|bonfile
granola|granola
retail|perakende
stewed|haşlama
apple|elma
apples|elma
chops|pirzola
chop|pirzola
strained|süzülmüş
black|siyah
patty|köfte
patties|köfte
alcoholic|alkollü
half|yarım
halves|yarımlar
lettuce|marul
bottled|şişelenmiş
composite|karışım
arm|kol
unenriched|zenginleştirilmemiş
shrimp|karides
bacon|domuz pastırması
orange|portakal
oranges|portakal
gluten|gluten
fortified|zenginleştirilmiş
concentrate|konsantre
shank|incik
species|türler
protein|protein
game|av
iced|buzlu
topping|üst malzeme
toppings|üst malzemeler
center|orta
salmon|somon
non|-sız
cuts|parçalar
cut|kesim
sour|ekşi
solution|çözelti
short|kısa
mushroom|mantar
mushrooms|mantar
macaroni|makarna
spread|sürülebilir
mayonnaise|mayonez
reconstituted|sulandırılmış
heated|ısıtılmış
unheated|ısıtılmamış
than|-
coconut|hindistan cevizi
end|uç
hard|sert
diet|diyet
spinach|ıspanak
wild|yabani
volume|hacim
small|küçük
bottom|alt
junior|küçük çocuk
dinner|akşam yemeği
high|yüksek
peppers|biber
pepper|biber
coating|kaplama
filling|dolgu
fillings|dolgular
calcium|kalsiyum
tuna|ton balığı
pineapple|ananas
strawberry|çilek
strawberries|çilek
wing|kanat
wings|kanat
large|büyük
dairy|süt ürünü
pretzels|pretzel
dessert|tatlı
desserts|tatlılar
poultry|kanatlı
banana|muz
bananas|muz
chinese|Çin usulü
hamburger|hamburger
pepperoni|pepperoni
condensed|yoğunlaştırılmış
tortilla|tortilla
tortillas|tortilla
puddings|pudingler
pudding|puding
wine|şarap
seafood|deniz ürünü
popcorn|patlamış mısır
peanuts|yer fıstığı
peanut|yer fıstığı
onion|soğan
onions|soğan
tip|uç
tips|uçlar
greek|Yunan
refrigerated|soğutulmuş
liver|ciğer
breaded|panelenmiş
breading|pane
iron|demir
energy|enerji
smoked|füme
peel|kabuk
heavy|yoğun
cabbage|lahana
brisket|döş
summer|yaz
chili|acı biber
table|sofralık
stew|güveç
tofu|tofu
kidney|böbrek
kidneys|böbrek
dog|sosis
thick|kalın
biscuit|bisküvi
biscuits|bisküvi
spices|baharatlar
spice|baharat
fluid|sıvı
rolls|rulo ekmek
lowfat|az yağlı
meatless|etsiz
natural|doğal
cranberry|kızılcık
cranberries|kızılcık
peach|şeftali
peaches|şeftali
raisins|kuru üzüm
raisin|kuru üzüm
mashed|püre
medium|orta
stuffed|dolgulu
spaghetti|spagetti
acid|asit
pumpkin|bal kabağı
carbonated|gazlı
pancakes|pankek
pancake|pankek
fries|kızartma
brewed|demlenmiş
ingredient|malzeme
ingredients|malzemeler
steamed|buharda pişmiş
substitute|ikame
loaf|somun
nutritional|besin
rotisserie|çevirme
kernels|tane
kernel|tane
cooking|yemeklik
back|sırt
dip|dip sos
icing|krema
shake|shake
barbecue|barbekü
blend|karışım
slice|dilim
slices|dilim
sesame|susam
pickled|turşu
leaves|yapraklar
leaf|yaprak
sprouted|filizlenmiş
immature|taze
creamy|kremalı
roll|rulo
soybean|soya fasulyesi
soybeans|soya fasulyesi
cone|külah
hash|rösti
sliced|dilimlenmiş
sweetener|tatlandırıcı
sweeteners|tatlandırıcılar
noodle|erişte
buttermilk|ayran
partially|kısmen
plate|tabak
whipped|çırpılmış
like|benzeri
cheddar|cheddar
under|altı
flat|düz
fillet|fileto
fillets|fileto
part|kısım
parts|parçalar
soymilk|soya sütü
waffle|waffle
pear|armut
pears|armut
blueberry|yaban mersini
blueberries|yaban mersini
simmered|kısık ateşte pişmiş
pasteurized|pastörize
mustard|hardal
cauliflower|karnabahar
oyster|istiridye
oysters|istiridye
stick|çubuk
sticks|çubuk
lunch|öğle yemeği
dough|hamur
chip|cips
chips|cips
pastry|hamur işi
pastries|hamur işleri
cottage|lor
jelly|jöle
product|ürün
pinto|barbunya
form|biçim
quick|hızlı
crab|yengeç
chopped|doğranmış
muffins|muffin
creams|kremalar
cream|krema
neck|boyun
broth|et suyu
juices|sular
juice|meyve suyu
sunflower|ayçiçeği
bars|bar
bar|bar
removed|çıkarılmış
mexican|Meksika usulü
almonds|badem
almond|badem
toast|kızarmış ekmek
covered|kaplı
bologna|bolonya salamı
cherries|kiraz
cherry|kiraz
applesauce|elma püresi
outside|dış
inside|iç
commercial|ticari
skim|yağsız
microwaved|mikrodalgada pişmiş
microwave|mikrodalga
bagel|simit (bagel)
bagels|simit (bagel)
duck|ördek
cucumber|salatalık
mozzarella|mozzarella
swiss|İsviçre peyniri
rye|çavdar
lasagna|lazanya
seasoning|baharat
seasoned|baharatlı
caffeine|kafein
custard|muhallebi
pink|pembe
prepackaged|paketli
dehydrated|kurutulmuş
garlic|sarımsak
heart|yürek
oven|fırın
beets|pancar
beet|pancar
fed|beslenmiş
beer|bira
long|uzun
vegetarian|vejetaryen
microwavable|mikrodalgaya uygun
deer|geyik
apricots|kayısı
apricot|kayısı
turnip|şalgam
turnips|şalgam
frosting|krema
frostings|kremalar
soda|soda
baking|pişirme
powdered|toz
root|kök
roots|kökler
sprouts|filiz
barley|arpa
cod|morina
firm|sert
casserole|güveç
seaweed|deniz yosunu
imitation|taklit
mackerel|uskumru
lentils|mercimek
lentil|mercimek
gelatin|jelatin
mild|hafif
cracked|kırık
chickpeas|nohut
chickpea|nohut
creamer|kahve kreması
plums|erik
plum|erik
trout|alabalık
nuggets|nugget
tongue|dil
rinsed|yıkanmış
pickles|turşu
pickle|turşu
chestnuts|kestane
fruits|meyveler
fruit|meyve
ripe|olgun
eggplant|patlıcan
celery|kereviz
batter|hamur
lime|misket limonu
kale|kara lahana
sauteed|sote
lightly|hafif
yolk|yumurta sarısı
yolks|yumurta sarısı
maple|akçaağaç
curry|köri
punch|punch
rack|kaburga
sushi|suşi
seedless|çekirdeksiz
starch|nişasta
catfish|yayın balığı
coleslaw|lahana salatası
zucchini|sakız kabağı
creamed|kremalı
meatballs|köfte
meatball|köfte
any|herhangi
peeled|soyulmuş
okra|bamya
grated|rendelenmiş
mixture|karışım
chowder|çorba (chowder)
split|kuru
bass|levrek
doughnut|donut
doughnuts|donut
fry|kızartma
popped|patlatılmış
sea|deniz
whey|peynir altı suyu
shredded|rendelenmiş
size|boy
young|genç
less|az
seeded|çekirdekli
cob|koçan
bulb|baş
shoots|filiz
common|yaygın
japanese|Japon usulü
herring|ringa
rings|halka
paste|ezme
goose|kaz
salsa|salsa
lobster|ıstakoz
gram|gram
grams|gram
cornbread|mısır ekmeği
farmed|çiftlik
caught|avlanmış
melon|kavun
melons|kavun
prunes|kuru erik
prune|kuru erik
tropical|tropik
buckwheat|karabuğday
brain|beyin
brains|beyin
curd|lor
pecans|pekan cevizi
pecan|pekan cevizi
pistachio|antep fıstığı
glazed|glazürlü
wings|kanat
topped|üstü kaplı
farm|çiftlik
millet|darı
raspberries|ahududu
raspberry|ahududu
papaya|papaya
oranges|portakal
quinoa|kinoa
ginger|zencefil
strip|şerit
vinegar|sirke
goat|keçi
perch|tatlı su levreği
breadsticks|grissini
assorted|karışık
strawberries|çilek
approximately|yaklaşık
radishes|turp
radish|turp
dill|dereotu
cashew|kaju
cashews|kaju
molasses|pekmez
spice|baharat
pita|pide (pita)
packaged|paketli
halibut|kalkan balığı
homemade|ev yapımı
pomegranate|nar
pomegranates|nar
pulp|posa
tart|tart
feet|paça
lungs|akciğer
spleen|dalak
amaranth|amarant
artichokes|enginar
artichoke|enginar
bitter|acı
semolina|irmik
olives|zeytin
olive|zeytin
purple|mor
walnuts|ceviz
walnut|ceviz
rolled|ezilmiş
evaporated|buharlaştırılmış
cubed|küp doğranmış
side|yan
figs|incir
fig|incir
grapes|üzüm
grape|üzüm
malted|maltlı
tostada|tostada
square|kare
pound|pound
uncoated|kaplamasız
indian|Hint usulü
asian|Asya usulü
tripe|işkembe
avocado|avokado
avocados|avokado
shellfish|kabuklu deniz ürünü
clams|istiridye
clam|istiridye
mussel|midye
mussels|midye
buffalo|manda
cultured|kültürlü
flaxseed|keten tohumu
rhubarb|ravent
watermelon|karpuz
puree|püre
chilled|soğutulmuş
skins|deri
marshmallows|marshmallow
pop|gazoz
mini|mini
leeks|pırasa
leek|pırasa
yam|yam
solid|katı
pickled|turşu
relish|turşu sosu
fava|bakla
macadamia|makadamya
parboiled|ön haşlanmış
cranberries|kızılcık
hazelnuts|fındık
hazelnut|fındık
filberts|fındık
pheasant|sülün
bulgur|bulgur
southern|güney
pigeon|güvercin
alcohol|alkol
kiwi|kivi
kiwifruit|kivi
old|eski
liqueur|likör
rainbow|gökkuşağı
crayfish|kerevit
brownie|brownie
brownies|brownie
scallops|tarak
scallop|tarak
octopus|ahtapot
cappuccino|kapuçino
jams|reçel
jam|reçel
preserves|reçel
tail|kuyruk
yogurts|yoğurtlar
yogurt|yoğurt
mandarin|mandalina
fennel|rezene
chard|pazı
parsnips|yaban havucu
pastrami|pastırma
stock|et suyu
horseradish|yaban turpu
hummus|humus
cheesecake|cheesecake
cube|küp
cubes|küp
pig|domuz
jerky|kurutulmuş et
squirrel|sincap
links|sosis
cold|soğuk
pitted|çekirdeği çıkarılmış
candied|şekerlenmiş
tamarind|demirhindi
ears|kulak
weight|ağırlık
fructose|fruktoz
rose|gül
tangerines|mandalina
tangerine|mandalina
edamame|edamame
flower|çiçek
flowers|çiçekler
jalapeno|jalapeno
chile|acı biber
chiles|acı biber
chilies|acı biber
mousse|mus
cantaloupe|kavun
watercress|su teresi
coriander|kişniş
cilantro|kişniş
parsley|maydanoz
ricotta|ricotta
eggnog|eggnog
bisque|kremalı çorba
squid|kalamar
calamari|kalamar
sardine|sardalya
sardines|sardalya
anchovy|hamsi
anchovies|hamsi
shark|köpekbalığı
swordfish|kılıç balığı
snapper|trança
cider|elma şırası
veggie|sebzeli
herb|ot
herbs|otlar
belly|karın
fermented|fermente
head|baş
dates|hurma
date|hurma
traditional|geleneksel
endive|hindiba
arugula|roka
couscous|kuskus
diced|küp doğranmış
mint|nane
pine|çam
chia|chia
kefir|kefir
naan|naan
croissants|kruvasan
croissant|kruvasan
falafel|falafel
croutons|kruton
crumbs|galeta unu
gooseberries|bektaşi üzümü
smoothies|smoothie
smoothie|smoothie
feta|beyaz peynir (feta)
sturgeon|mersin balığı
chorizo|chorizo
brined|salamura
paella|paella
ketchup|ketçap
catsup|ketçap
sherbet|şerbet
stomach|mide
flatbread|yassı ekmek
peppermint|nane
frog|kurbağa
jujube|hünnap
alfalfa|yonca
kelp|deniz yosunu
pimento|kırmızı biber
pimiento|kırmızı biber
cornstarch|mısır nişastası
pearl|inci
durum|durum buğdayı
meatloaf|rulo köfte
nectarines|nektarin
spirulina|spirulina
hulled|kabuğu ayıklanmış
crushed|ezilmiş
truffle|trüf
truffles|trüf
caraway|kimyon (karaviye)
thyme|kekik
savory|kekik
necks|boyun
gouda|gouda
human|insan
ghee|sade yağ
nutmeg|muskat
rosemary|biberiye
blood|kan
starfruit|yıldız meyvesi
brie|brie
camembert|camembert
poached|haşlanmış
cutlet|kotlet
paneer|paneer
mole|mole
tempura|tempura
german|Alman usulü
gnocchi|gnocchi
guacamole|guakamole
string|dize
lump|parça
slush|buzlu içecek
fine|ince
overripe|çok olgun
lemons|limon
lemon|limon
lemonade|limonata
limes|misket limonu
quinces|ayva
quince|ayva
mulberries|dut
mulberry|dut
clementines|klementin
jowl|gıdı
mangos|mango
mango|mango
scallions|taze soğan
hemp|kenevir
coconuts|hindistan cevizi
spicy|acılı
brine|salamura
year|yıl
average|ortalama
thinly|ince
cup|su bardağı (ABD)
cups|su bardağı (ABD)
sheep|koyun
mutton|koyun eti
cumin|kimyon
saffron|safran
cardamom|kakule
cayenne|kırmızı toz biber
sharp|keskin
tsp|tatlı kaşığı
tbsp|yemek kaşığı
tablespoon|yemek kaşığı
teaspoon|tatlı kaşığı
oz|ons
lb|libre
piece|adet
pieces|adet
serving|porsiyon
servings|porsiyon
container|kap
package|paket
packet|paket
bottle|şişe
can|kutu
box|kutu
bowl|kase
pint|pint
quart|quart
gallon|galon
fl|sıvı
whole|bütün
fruit|meyve
milk|süt
rice|pirinç
soup|çorba
soups|çorbalar
green|yeşil
red|kırmızı
with|ile
juice|meyve suyu
sweet|tatlı
corn|mısır
chocolate|çikolata
butter|tereyağı
fish|balık
lean|yağsız
light|light
honey|bal
wheat|buğday
oats|yulaf
oat|yulaf
oatmeal|yulaf lapası
nuts|kuruyemiş
nut|kuruyemiş
pizza|pizza
taco|taco
burrito|burrito
soy|soya
cocoa|kakao
candy|şekerleme
candies|şekerlemeler
gum|sakız
original|orijinal
mix|karışım
crisp|çıtır
crispy|çıtır
crunchy|çıtır
dark|koyu
breakfast|kahvaltı
kabob|kebap
kebab|kebap
shish|şiş
gyro|döner (gyro)
pilaf|pilav
stuffing|iç dolgu
dumpling|mantı (dumpling)
dumplings|mantı (dumpling)
ravioli|ravioli
spinach|ıspanak
beef|dana eti
venison|geyik eti
rabbit|tavşan
quail|bıldırcın
hen|tavuk
hens|tavuk
capons|horoz
gizzard|taşlık
giblets|sakatat
animal|hayvan
horse|at
bison|bizon
elk|geyik (elk)
moose|geyik (moose)
ostrich|devekuşu
emu|emu
caribou|ren geyiği
antelope|antilop
boar|yaban domuzu
hearts|yürek
"""

def _parse(raw):
    out = {}
    for line in raw.strip().splitlines():
        line = line.strip()
        if not line or '|' not in line:
            continue
        en, tr = line.split('|', 1)
        out[en.strip().lower()] = tr.strip()
    return out

PHRASES = _parse(PHRASES_RAW)
WORDS = _parse(WORDS_RAW)

# ---- Ek kalıplar ve kelimeler (ilk üretimden sonra eksik çıkanlar) ----
PHRASES.update(_parse("""
bone-in|kemikli
lip-on|kenarlı
lip-off|kenarı alınmış
kidney|kırmızı (kidney)
navy|beyaz (navy)
canned in tomato sauce|domates soslu konserve
drained solids with bone|süzülmüş, kemikli
except chocolate|çikolata hariç
all flavors except chocolate|çikolata dışı tüm aromalar
ready-to-heat|ısıtmaya hazır
ready-to-drink|içmeye hazır
ready-to-bake|pişirmeye hazır
by-products|yan ürünler
non-dairy|bitkisel (süt içermez)
low-fat|az yağlı
fat-free|yağsız
sugar-free|şekersiz
whole-wheat|tam buğday
stir-fried|wok'ta sotelenmiş
pan-fried|tavada kızartılmış
deep-fried|bol yağda kızartılmış
oven-heated|fırında ısıtılmış
oven-roasted|fırında kızartılmış
home-prepared|evde hazırlanmış
pre-cooked|önceden pişmiş
re-fried|ezilmiş
""".replace('|', '|')))
WORDS.update(_parse("""
except|hariç
shell|kabuk
shells|kabuk
bag|poşet
single|tek
thin|ince
very|çok
poured|dökülmüş
yield|verim
cubic|küp
inch|inç
dia|çap
multigrain|çok tahıllı
chunk|parçalı
chunks|parçalar
striploin|kontrfile
carcass|karkas
australian|Avustralya
pacific|Pasifik
atlantic|Atlantik
california|Kaliforniya
alaska|Alaska
northern|kuzey
eastern|doğu
western|batı
fl|sıvı
nlea|NLEA
item|adet
each|adet
large|büyük
extra|ekstra
jumbo|jumbo
regular|normal
cooked|pişmiş
yields|verim
yield|verim
container|kap
containers|kap
envelope|zarf
tablet|tablet
tablets|tablet
scoop|ölçek
scoops|ölçek
packet|paket
packets|paket
pat|parça
patty|köfte
link|sosis
links|sosis
wedge|dilim
wedges|dilim
strip|şerit
strips|şeritler
stalk|sap
stalks|sap
spear|filiz
spears|filiz
floweret|çiçek
florets|çiçekler
head|baş
clove|diş
cloves|diş
sprig|dal
ear|koçan
pod|kabuk
pods|kabuk
fruit|meyve
berry|dut
berries|dutgiller
nut|kuruyemiş
dozen|düzine
cake|kek
muffin|muffin
loaf|somun
roll|rulo
frank|sosis
hot|sıcak
cold|soğuk
small|küçük
medium|orta
miniature|minyatür
individual|tekli
whole|bütün
"""))

WORDS.update(_parse("""
fast|hızlı
light|light
bar|bar
food|gıda
baby|bebek
grain|tahıl
native|yerli
program|program
feed|besleme
hydrogenated|hidrojenize
us|ABD
cinnamon|tarçın
free|-sız
english|İngiliz
industrial|endüstriyel
italian|İtalyan
organic|organik
american|Amerikan
mollusks|yumuşakçalar
winter|kış
eye|göz
crustaceans|kabuklular
moisture|nem
rump|sağrı
asparagus|kuşkonmaz
recipe|tarif
caramel|karamel
shortening|katı yağ
cornmeal|mısır unu
puffed|patlatılmış
snap|taze
uncooked|pişmemiş
nectar|nektar
garden|bahçe
brand|marka
palm|palmiye
use|kullanım
meal|yemek
grass|ot
flank|döş
filet|fileto
salami|salam
tub|kap
process|işlenmiş
soda|soda
bone|kemik
bran|kepek
flakes|gevrek
citrus|turunçgil
blue|mavi
bakery|fırın
store|market
marshmallow|marshmallow
domesticated|evcil
lower|düşük
corned|tuzlanmış
crumbles|ufalanmış
chewy|yumuşak
collards|kara lahana
flesh|et
frankfurter|sosis
chunky|parçalı
turnover|turnover
puffs|puf
toast|kızarmış ekmek
wafer|gofret
wafers|gofret
scalloped|graten
cowpeas|börülce
sorghum|sorgum
foreshank|ön incik
broil|ızgara
double|duble
crunch|çıtır
tap|musluk
knuckle|eklem
yeast|maya
special|özel
frosted|şekerli
school|okul
european|Avrupa
grapefruit|greyfurt
creme|krema
malt|malt
pot|tencere
plus|artı
fiber|lif
canola|kanola
burger|burger
roasting|kızartmalık
stewing|haşlamalık
half|yarım
fully|tamamen
purpose|amaçlı
bleached|ağartılmış
gelatinized|jelatinize
multiple|çoklu
guava|guava
wrap|dürüm
distilled|damıtılmış
minute|dakika
full|tam
defatted|yağı alınmış
good|iyi
fore|ön
extruded|ekstrüde
navy|beyaz (navy)
cap|kapak
browned|kızartılmış
contains|içerir
replacement|yerine geçen
purchased|satın alınmış
deep|derin
pancreas|pankreas
club|kulüp
cocktail|kokteyl
higher|daha yüksek
dulce|tatlı
spanish|İspanyol
canadian|Kanada
dish|yemek
premature|prematüre
edible|yenilebilir
frying|kızartmalık
tenders|şeritler
poppy|haşhaş
basil|fesleğen
unspecified|belirtilmemiş
subcutaneous|deri altı
mechanically|mekanik olarak
muscle|kas
zero|sıfır
passion|çarkıfelek
sourdough|ekşi mayalı
extract|özüt
lard|domuz yağı
pound|pound
crumb|kırıntı
general|genel
precooked|önceden pişmiş
degermed|ruşeymi alınmış
self|kendinden
rising|kabaran
unbleached|ağartılmamış
gourd|su kabağı
thai|Tay
soaked|ıslatılmış
island|ada
lotus|nilüfer
standard|standart
sports|spor
hot|sıcak
acorn|meşe palamudu
unroasted|kavrulmamış
drippings|et suyu yağı
potassium|potasyum
undiluted|seyreltilmemiş
fashioned|usulü
supreme|supreme
bits|parçacıklar
mini|mini
turtle|kaplumbağa
glutinous|yapışkan
dietary|diyet
cacao|kakao
pretzel|pretzel
bite|lokma
cook|pişirme
time|süre
lactose|laktoz
deli|şarküteri
additives|katkılar
dumpling|mantı
griddle|ızgara tava
tartar|tartar
pike|turna balığı
sun|güneş
yam|tatlı patates (yam)
shiitake|shiitake
uses|kullanımlar
incomplete|eksik
podded|kabuklu
spareribs|kaburga
granules|granül
thousand|bin
agave|agave
sweet|tatlı
whipping|çırpma
heavy|yoğun
"""))

WORDS.update(_parse("""
bell|dolmalık
licorice|meyan kökü şekeri
order|sipariş
surface|yüzey
refuse|atık
excluding|hariç
"""))

WORDS.update(_parse("""
shishkabob|şiş kebap
shishkabobs|şiş kebap
kabobs|kebap
"""))
