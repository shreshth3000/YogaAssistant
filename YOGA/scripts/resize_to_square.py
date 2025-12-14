from PIL import Image
from pathlib import Path

TARGET_DP = 72  # matches wrapper size

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / 'app' / 'src' / 'main' / 'res' / 'drawable'

FILES = [
    'weight_loss.png', 'flexibility.png', 'core_strength.png', 'stress_relief.png',
    'better_posture.png', 'digestion.png', 'endurance.png', 'relaxation.png',
    'stress.png', 'balance_issues.png', 'neck_pain.png', 'shoulder_pain.png',
    'joint_stiffness.png', 'low_flexibility.png', 'digestive_issues.png'
]

def force_square(img: Image.Image, size: int) -> Image.Image:
    # Stretch to exact square size (keeps transparency)
    return img.resize((size, size), Image.LANCZOS)

def main():
    for name in FILES:
        path = DRAWABLE / name
        if not path.exists():
            print(f'Skip missing: {path}')
            continue
        img = Image.open(path).convert('RGBA')
        squared = force_square(img, TARGET_DP)
        squared.save(path)
        print(f'Resized to {TARGET_DP}x{TARGET_DP}: {path}')

if __name__ == '__main__':
    main()



