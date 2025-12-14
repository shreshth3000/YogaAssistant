#!/usr/bin/env python3
"""
Yoga Icon Asset Generator - Production Version
Generates high-quality yoga icons for the mobile app using Midjourney v7
"""

import os
import requests
import json
from datetime import datetime
from openai import OpenAI
import time

class YogaIconGenerator:
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.base_url = "https://api.a4f.co/v1"
        self.working_model = "provider-5/midjourney-v7"  # Confirmed working model
        self.client = OpenAI(api_key=api_key, base_url=self.base_url)
        self.output_dir = "yoga_app_icons"
        self.ensure_output_dir()
        
    def ensure_output_dir(self):
        """Create output directory if it doesn't exist"""
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)
            print(f"Created output directory: {self.output_dir}")
    
    def download_image(self, url: str, filename: str) -> bool:
        """Download image from URL and save to file"""
        try:
            response = requests.get(url)
            response.raise_for_status()
            
            filepath = os.path.join(self.output_dir, filename)
            with open(filepath, 'wb') as f:
                f.write(response.content)
            
            print(f"✓ Downloaded: {filename}")
            return True
        except Exception as e:
            print(f"✗ Download failed for {filename}: {e}")
            return False
    
    def generate_icon(self, prompt: str, filename: str, size: str = "1024x1024") -> bool:
        """Generate a single icon"""
        try:
            print(f"Generating: {filename}")
            print(f"Prompt: {prompt[:80]}...")
            
            response = self.client.images.generate(
                model=self.working_model,
                prompt=prompt,
                n=1,
                size=size
            )
            
            if response.data and len(response.data) > 0:
                image_url = response.data[0].url
                print(f"✓ Generated! URL: {image_url}")
                
                # Download the image
                return self.download_image(image_url, filename)
            else:
                print("✗ No image data returned")
                return False
                
        except Exception as e:
            print(f"✗ Error generating {filename}: {e}")
            return False
    
    def generate_app_icons(self):
        """Generate all necessary app icons"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Define icon specifications for different use cases
        icon_specs = [
            {
                "name": "app_icon_main",
                "prompt": "Yoga app icon, tree pose silhouette, minimalist design, clean white background, modern style, perfect for mobile app launcher",
                "size": "1024x1024",
                "description": "Main app icon for launcher"
            },
            {
                "name": "app_icon_small",
                "prompt": "Yoga tree pose icon, simple silhouette, clean white background, minimalist design, perfect for small app icons",
                "size": "512x512",
                "description": "Small app icon variant"
            },
            {
                "name": "notification_icon",
                "prompt": "Yoga tree pose notification icon, simple line art, white background, minimalist design, perfect for Android notification",
                "size": "256x256",
                "description": "Notification icon"
            },
            {
                "name": "splash_icon",
                "prompt": "Yoga tree pose logo, elegant design, clean white background, professional style, perfect for app splash screen",
                "size": "1024x1024",
                "description": "Splash screen icon"
            },
            {
                "name": "feature_icon",
                "prompt": "Yoga tree pose feature icon, flat design, modern style, clean background, perfect for feature highlights",
                "size": "512x512",
                "description": "Feature highlight icon"
            }
        ]
        
        print(f"Generating {len(icon_specs)} app icons...")
        print("=" * 60)
        
        successful_generations = 0
        
        for spec in icon_specs:
            filename = f"{spec['name']}_{timestamp}.png"
            print(f"\n--- {spec['description']} ---")
            
            success = self.generate_icon(spec['prompt'], filename, spec['size'])
            if success:
                successful_generations += 1
            
            # Delay between requests to avoid rate limiting
            time.sleep(3)
        
        print(f"\n{'='*60}")
        print(f"Generation complete!")
        print(f"Successfully generated: {successful_generations}/{len(icon_specs)} icons")
        print(f"Icons saved in: {self.output_dir}/")
        print(f"{'='*60}")
        
        return successful_generations
    
    def generate_variations(self):
        """Generate different style variations of the yoga tree pose"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        style_variations = [
            {
                "name": "minimalist_style",
                "prompt": "Yoga tree pose icon, minimalist style, simple line art, clean white background, perfect for modern app design",
                "description": "Minimalist style"
            },
            {
                "name": "geometric_style",
                "prompt": "Yoga tree pose icon, geometric style, clean lines, modern design, white background, perfect for tech apps",
                "description": "Geometric style"
            },
            {
                "name": "flat_design",
                "prompt": "Yoga tree pose icon, flat design style, modern UI design, clean white background, perfect for mobile apps",
                "description": "Flat design style"
            },
            {
                "name": "outline_style",
                "prompt": "Yoga tree pose icon, outline style, simple line drawing, white background, minimalist design",
                "description": "Outline style"
            },
            {
                "name": "gradient_style",
                "prompt": "Yoga tree pose icon, subtle gradient background, modern design, clean style, perfect for premium apps",
                "description": "Gradient style"
            }
        ]
        
        print(f"Generating {len(style_variations)} style variations...")
        print("=" * 60)
        
        successful_generations = 0
        
        for variation in style_variations:
            filename = f"yoga_tree_pose_{variation['name']}_{timestamp}.png"
            print(f"\n--- {variation['description']} ---")
            
            success = self.generate_icon(variation['prompt'], filename)
            if success:
                successful_generations += 1
            
            # Delay between requests
            time.sleep(3)
        
        print(f"\n{'='*60}")
        print(f"Style variations complete!")
        print(f"Successfully generated: {successful_generations}/{len(style_variations)} variations")
        print(f"{'='*60}")
        
        return successful_generations

def main():
    """Main function"""
    print("Yoga Icon Asset Generator - Production Version")
    print("=" * 60)
    print("Using Midjourney v7 for high-quality icon generation")
    print("=" * 60)
    
    # Initialize generator
    api_key = "ddc-a4f-04e25c907b344795bbc84138ef96eee8"
    generator = YogaIconGenerator(api_key)
    
    while True:
        print("\nOptions:")
        print("1. Generate app icons (main icons for different use cases)")
        print("2. Generate style variations (different artistic styles)")
        print("3. Generate both app icons and variations")
        print("4. Exit")
        
        choice = input("\nEnter your choice (1-4): ").strip()
        
        if choice == "1":
            generator.generate_app_icons()
        elif choice == "2":
            generator.generate_variations()
        elif choice == "3":
            print("Generating both app icons and style variations...")
            app_success = generator.generate_app_icons()
            print("\n" + "="*60)
            variations_success = generator.generate_variations()
            print(f"\nTotal generated: {app_success + variations_success} icons")
        elif choice == "4":
            print("Goodbye!")
            break
        else:
            print("Invalid choice. Please try again.")

if __name__ == "__main__":
    main()

