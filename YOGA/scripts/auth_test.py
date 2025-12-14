#!/usr/bin/env python3
"""
Authentication Test Script
Test both Bearer and non-Bearer authentication methods
"""

import requests
import json
from openai import OpenAI

# Configuration
API_KEY = "ddc-a4f-04e25c907b344795bbc84138ef96eee8"
BASE_URL = "https://api.a4f.co/v1"

def test_auth_methods():
    """Test different authentication methods"""
    print("Testing Authentication Methods")
    print("=" * 40)
    
    # Test 1: With Bearer token
    print("\n1. Testing with Bearer token...")
    try:
        response = requests.get(
            f"{BASE_URL}/models",
            headers={"Authorization": f"Bearer {API_KEY}"}
        )
        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"✓ Bearer auth successful! Found {len(data.get('data', []))} models")
        else:
            print(f"✗ Bearer auth failed: {response.text}")
    except Exception as e:
        print(f"✗ Bearer auth error: {e}")
    
    # Test 2: Without Bearer token (direct API key)
    print("\n2. Testing without Bearer token...")
    try:
        response = requests.get(
            f"{BASE_URL}/models",
            headers={"Authorization": API_KEY}
        )
        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"✓ Direct API key auth successful! Found {len(data.get('data', []))} models")
        else:
            print(f"✗ Direct API key auth failed: {response.text}")
    except Exception as e:
        print(f"✗ Direct API key auth error: {e}")
    
    # Test 3: With X-API-Key header
    print("\n3. Testing with X-API-Key header...")
    try:
        response = requests.get(
            f"{BASE_URL}/models",
            headers={"X-API-Key": API_KEY}
        )
        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"✓ X-API-Key auth successful! Found {len(data.get('data', []))} models")
        else:
            print(f"✗ X-API-Key auth failed: {response.text}")
    except Exception as e:
        print(f"✗ X-API-Key auth error: {e}")

def test_image_generation_bearer():
    """Test image generation with Bearer token"""
    print("\n" + "="*50)
    print("Testing Image Generation with Bearer Token")
    print("="*50)
    
    try:
        client = OpenAI(api_key=API_KEY, base_url=BASE_URL)
        
        print("Testing with provider-2/flux.1-schnell...")
        response = client.images.generate(
            model="provider-2/flux.1-schnell",
            prompt="A cute baby sea otter",
            n=1,
            size="1024x1024"
        )
        
        if response.data and len(response.data) > 0:
            image_url = response.data[0].url
            print(f"✓ Success! Image URL: {image_url}")
            return image_url
        else:
            print("✗ No image data returned")
            return None
            
    except Exception as e:
        print(f"✗ Error with Bearer token: {e}")
        return None

def test_image_generation_yoga():
    """Test image generation with yoga prompts"""
    print("\n" + "="*50)
    print("Testing Yoga Icon Generation")
    print("="*50)
    
    yoga_prompts = [
        "A minimalist yoga tree pose silhouette icon, clean white background, simple line art style, perfect for mobile app icon",
        "Yoga tree pose icon, flat design, modern style, clean background, suitable for fitness app",
        "Simple yoga tree pose symbol, vector style, white background, minimalist design for mobile app"
    ]
    
    try:
        client = OpenAI(api_key=API_KEY, base_url=BASE_URL)
        
        for i, prompt in enumerate(yoga_prompts, 1):
            print(f"\n--- Testing Yoga Prompt {i} ---")
            print(f"Prompt: {prompt[:80]}...")
            
            try:
                response = client.images.generate(
                    model="provider-2/flux.1-schnell",
                    prompt=prompt,
                    n=1,
                    size="1024x1024"
                )
                
                if response.data and len(response.data) > 0:
                    image_url = response.data[0].url
                    print(f"✓ Success! Image URL: {image_url}")
                    
                    # Download the image
                    download_image(image_url, f"yoga_tree_pose_prompt{i}.png")
                else:
                    print("✗ No image data returned")
                    
            except Exception as e:
                print(f"✗ Error generating image: {e}")
            
            # Small delay between requests
            import time
            time.sleep(2)
            
    except Exception as e:
        print(f"✗ Error setting up client: {e}")

def download_image(url, filename):
    """Download image from URL"""
    try:
        response = requests.get(url)
        response.raise_for_status()
        
        # Create output directory
        import os
        output_dir = "generated_icons"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        
        filepath = os.path.join(output_dir, filename)
        with open(filepath, 'wb') as f:
            f.write(response.content)
        
        print(f"✓ Downloaded: {filename}")
        return True
    except Exception as e:
        print(f"✗ Download failed: {e}")
        return False

def main():
    print("A4F API Authentication and Image Generation Test")
    print("=" * 60)
    
    # Test authentication methods
    test_auth_methods()
    
    # Test image generation with Bearer token
    test_image_generation_bearer()
    
    # Test yoga icon generation
    test_image_generation_yoga()
    
    print("\n" + "="*60)
    print("All tests completed!")
    print("Check the 'generated_icons' folder for downloaded images.")
    print("="*60)

if __name__ == "__main__":
    main()

