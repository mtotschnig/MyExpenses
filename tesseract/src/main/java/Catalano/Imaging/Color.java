// Catalano Android Imaging Library
// The Catalano Framework
//
// Copyright © Diego Catalano, 2012-2016
// diego.catalano at live.com
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
//

package Catalano.Imaging;

/**
 * Represents RGB color.
 * @author Diego Catalano
 */
public class Color {
    
    /**
     * Black.
     * R: 0  G: 0  B: 0
     */
    public final static Color Black = new Color(0, 0, 0);
    
    /**
     * Blue.
     * R: 0  G: 0  B: 255
     */
    public final static Color Blue = new Color(0, 0, 255);
    
    /**
     * Cyan.
     * R: 0  G: 255  B: 255
     */
    public final static Color Cyan = new Color(0, 255, 255);
    
    /**
     * Dark Gray.
     * R: 64  G: 64  B: 64
     */
    public final static Color DarkGray = new Color(64, 64, 64);
    
    /**
     * Gray.
     * R: 128  G: 128  B: 128
     */
    public final static Color Gray = new Color(128, 128, 128);
    
    /**
     * Green.
     * R: 0  G: 255  B: 0
     */
    public final static Color Green = new Color(0, 255, 0);
    
    /**
     * Light Gray.
     * R: 192  G: 192  B: 192
     */
    public final static Color LightGray = new Color(192, 192, 192);
    
    /**
     * Magenta.
     * R: 255  G: 0  B: 255
     */
    public final static Color Magenta = new Color(255, 0, 255);
    
    /**
     * Orange.
     * R: 255  G: 200  B: 0
     */
    public final static Color Orange = new Color(255, 200, 0);
    
    /**
     * Pink.
     * R: 255  G: 175  B: 175
     */
    public final static Color Pink = new Color(255, 175, 175);
    
    /**
     * Red.
     * R: 255  G: 0  B: 0
     */
    public final static Color Red = new Color(255, 0, 0);
    
    /**
     * Yellow.
     * R: 255  G: 200  B: 0
     */
    public final static Color Yellow = new Color(255, 200, 0);
    
    /**
     * White.
     * R: 255  G: 255  B: 255
     */
    public final static Color White = new Color(255, 255, 255);
    
    /**
     * Red channel's component.
     */
    public int r = 0;
    
    /**
     * Green channel's component.
     */
    public int g = 0;
    
    /**
     * Blue channel's component.
     */
    public int b = 0;
    
    /**
     * Count many colors has in the image.
     * @param fastBitmap Image to be procesed.
     * @return Number of colors.
     */
    public static int Count(FastBitmap fastBitmap){
        
        if(fastBitmap.isGrayscale()){

            byte[] maxColors = new byte[256];
            int colors = 0;
            
            int[] data = fastBitmap.getData();
            for (int i = 0; i < data.length; i++)
                    maxColors[data[i]] = 1;

            for (int i = 0; i < 256; i++)
                if(maxColors[i] == 1) colors++;

            return colors;
    
        }
        
        byte[] maxColors = new byte[16777216];
        int colors = 0;

        int[] data = fastBitmap.getData();
        for (int i = 0; i < data.length; i++)
                maxColors[data[i]] = 1;

        for (int i = 0; i < 16777216; i++)
            if(maxColors[i] == 1) colors++;

        return colors;
        
    }

    /**
     * Initialize a new instance of the Color class.
     */
    public Color() {}
    
    /**
     * Initialize a new instance of the Color class.
     * @param red Red component.
     * @param green Green component.
     * @param blue Blue component.
     */
    public Color(int red, int green, int blue){
        this.r = red;
        this.g = green;
        this.b = blue;
    }
    
    /**
     * Initialize a new instance of the Color class.
     * @param rgb RGB array.
     */
    public Color(int[] rgb){
        this.r = rgb[0];
        this.g = rgb[1];
        this.b = rgb[2];
    }
    
    /**
     * Initialize a new instance of the Color class.
     * @param rgb Packed RGB.
     */
    public Color(int rgb){
        this.r = rgb >> 16 & 0xFF;
        this.g = rgb >> 8 & 0xFF;
        this.b = rgb & 0xFF;
    }
    
    /**
     * Compare two colors.
     * @param c1 First color.
     * @param c2 Second color.
     * @return True if the color is the same, otherwise false.
     */
    public static boolean isEqual(Color c1, Color c2){
        if((c1.r == c2.r)
            && (c1.g == c2.g)
            && (c1.b == c2.b))
            return true;
        
        return false;
            
    }
    
    @Override
    public boolean equals(Object obj) {
        Color c2 = (Color)obj;
        if((r == c2.r)
            && (g == c2.g)
            && (b == c2.b))
            return true;
        
        return false;
    }
    
}