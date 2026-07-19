# Rebuild money-log-ui-for-figma.pdf from png/
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parent
png_dir = root / "png"
out = root / "money-log-ui-for-figma.pdf"
order = [
    "01-auth-login.png",
    "02-auth-signup.png",
    "03-error-forbidden.png",
    "04-payment-list.png",
    "04b-payment-create-modal.png",
    "05-expend-group-list.png",
    "06-ledger-monthly.png",
    "06b-ledger-expense-modal.png",
    "07-ledger-excel.png",
    "08-fixed-expense-list.png",
    "09-member-admin-list.png",
    "10-member-profile.png",
    "11-target-default.png",
    "12-statistics-monthly.png",
]
imgs = [Image.open(png_dir / n).convert("RGB") for n in order]
max_w = max(i.width for i in imgs)
pages = []
for im in imgs:
    if im.width != max_w:
        canvas = Image.new("RGB", (max_w, im.height), (244, 247, 245))
        canvas.paste(im, (0, 0))
        im = canvas
    pages.append(im)
pages[0].save(out, save_all=True, append_images=pages[1:], resolution=144)
print("wrote", out)
