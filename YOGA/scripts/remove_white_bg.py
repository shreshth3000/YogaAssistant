from PIL import Image
from pathlib import Path

# Configure source folder inside the project
SRC = Path(__file__).resolve().parents[1] / 'app' / 'src' / 'main' / 'res' / 'drawable'

# PNGs we added for preferences
FILES = [
    'weight_loss.png', 'flexibility.png', 'core_strength.png', 'stress_relief.png',
    'better_posture.png', 'digestion.png', 'endurance.png', 'relaxation.png',
    'stress.png', 'balance_issues.png', 'neck_pain.png', 'shoulder_pain.png',
    'joint_stiffness.png', 'low_flexibility.png', 'digestive_issues.png'
]

# Threshold for what counts as background (near white)
THRESHOLD = 245

def remove_white_background(png_path: Path) -> None:
    img = Image.open(png_path).convert('RGBA')
    datas = img.getdata()
    new_data = []
    for r, g, b, a in datas:
        if r >= THRESHOLD and g >= THRESHOLD and b >= THRESHOLD:
            new_data.append((r, g, b, 0))
        else:
            new_data.append((r, g, b, a))
    img.putdata(new_data)
    img.save(png_path)

def main():
    for name in FILES:
        path = SRC / name
        if path.exists():
            print(f'Processing {path}')
            remove_white_background(path)
        else:
            print(f'Skipped (missing): {path}')

if __name__ == '__main__':
    main()



