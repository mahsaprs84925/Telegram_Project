#!/usr/bin/env python3
from PIL import Image, ImageDraw, ImageFont
import os

# Create a simple default profile image
size = (40, 40)
background_color = (204, 204, 204)  # Light gray
text_color = (153, 153, 153)  # Darker gray

# Create image
img = Image.new('RGB', size, background_color)
draw = ImageDraw.Draw(img)

# Add user icon (simple circle with "👤" or "U")
# Draw a simple circle for user
circle_color = (170, 170, 170)
circle_radius = 15
center = (20, 20)

# Draw circle
draw.ellipse([center[0] - circle_radius, center[1] - circle_radius, 
              center[0] + circle_radius, center[1] + circle_radius], 
             fill=circle_color)

# Try to add text "U" in the center
try:
    # Use a basic font
    font = ImageFont.load_default()
    text = "U"
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    text_x = center[0] - text_width // 2
    text_y = center[1] - text_height // 2
    draw.text((text_x, text_y), text, fill=text_color, font=font)
except:
    pass

# Save the image
img.save('default-profile.png', 'PNG')
print("Default profile image created successfully!")
