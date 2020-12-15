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

package Catalano.Imaging.Filters;

import Catalano.Imaging.FastBitmap;
import Catalano.Imaging.IApplyInPlace;

/**
 * Base class for image grayscaling.
 * 
 * Supported types: RGB.
 * Coordinate System: Independent.
 * 
 * @author Diego Catalano
 */
public class Grayscale implements IApplyInPlace{
    
    double redCoefficient = 0.2125, greenCoefficient = 0.7154, blueCoefficient = 0.0721;
    
    /**
     * Three methods for grayscale.
     */
    public static enum Algorithm {

        /**
         * (Max(red, green, blue) + Min(red, green, blue)) / 2
         */
        Lightness,
        
        /**
         * (red + green + blue) / 3
         */
        Average,
        
        /**
         * (red * green * blue) ^ 1/3
         */
        GeometricMean,
        
        /**
         * 0.2125R + 0.7154G + 0.0721B
         */
        Luminosity,
        
        /**
         * Min(red, green, max)
         */
        MinimumDecomposition,
        
        /**
         * Max(red, gree, blue)
         */
        MaximumDecomposition
    };
    private Algorithm grayscaleMethod;
    private boolean isAlgorithm = false;

    /**
     * Initializes a new instance of the Grayscale class.
     * In this constructor, will be 0.2125B + 0.7154G + 0.0721B.
     */
    public Grayscale() {}

    /**
     * Initializes a new instance of the Grayscale class. 
     * @param redCoefficient Portion of red channel's value to use during conversion from RGB to grayscale. 
     * @param greenCoefficient Portion of green channel's value to use during conversion from RGB to grayscale. 
     * @param blueCoefficient Portion of blue channel's value to use during conversion from RGB to grayscale. 
     */
    public Grayscale(double redCoefficient, double greenCoefficient, double blueCoefficient) {
        this.redCoefficient = redCoefficient;
        this.greenCoefficient = greenCoefficient;
        this.blueCoefficient = blueCoefficient;
        this.isAlgorithm = false;
    }
    
    /**
     * Initializes a new instance of the Grayscale class. 
     * @param grayscaleMethod Methods for grayscaling.
     */
    public Grayscale(Algorithm grayscaleMethod){
        this.grayscaleMethod = grayscaleMethod;
        this.isAlgorithm = true;
    }

    /**
     * Get red coefficient
     * @return red coefficient
     */
    public double getRedCoefficient() {
        return redCoefficient;
    }

    /**
     * Set red coefficient
     * @param redCoefficient red coefficient
     */
    public void setRedCoefficient(double redCoefficient) {
        this.redCoefficient = redCoefficient;
    }

    /**
     * Get green coefficient
     * @return green coefficient
     */
    public double getGreenCoefficient() {
        return greenCoefficient;
    }

    /**
     * Set green coefficient
     * @param greenCoefficient green coefficient
     */
    public void setGreenCoefficient(double greenCoefficient) {
        this.greenCoefficient = greenCoefficient;
    }

    /**
     * Get blue coefficient
     * @return blue coefficient
     */
    public double getBlueCoefficient() {
        return blueCoefficient;
    }

    /**
     * Set blue coefficient
     * @param blueCoefficient blue coefficient
     */
    public void setBlueCoefficient(double blueCoefficient) {
        this.blueCoefficient = blueCoefficient;
    }

    /**
     * Get Grayscale Method
     * @return Grayscale Method
     */
    public Algorithm getGrayscaleMethod() {
        return grayscaleMethod;
    }

    /**
     * Set Grayscale Method
     * @param grayscaleMethod Grayscale Method
     */
    public void setGrayscaleMethod(Algorithm grayscaleMethod) {
        this.grayscaleMethod = grayscaleMethod;
    }
    
    /**
     * Apply filter to a FastBitmap.
     * @param fastBitmap Image to be processed.
     */
    @Override
    public void applyInPlace(FastBitmap fastBitmap){
            if(!isAlgorithm){
                double r,g,b;
                int gray;
                
                fastBitmap.indicateGrayscale(true);

                int[] pixels = fastBitmap.getData();
                for (int i = 0; i < pixels.length; i++) {
					r = pixels[i] >> 16 & 0xFF;
                	g = pixels[i] >> 8 & 0xFF;
                	b = pixels[i] & 0xFF;
                	
                	gray = (int)(r*redCoefficient+g*greenCoefficient+b*blueCoefficient);
                	pixels[i] = 255 << 24 | gray << 16 | gray << 8 | gray;
				}
                
            }
            else{
            	fastBitmap.indicateGrayscale(true);
                Apply(fastBitmap, this.grayscaleMethod);
            }
    }
    
    private void Apply(FastBitmap fastBitmap,Algorithm grayMethod){
            double r,g,b;
            int gray;

            int[] pixels = fastBitmap.getData();
            switch(grayMethod){
                case Lightness:

                    double max,min;
                    for (int i = 0; i < pixels.length; i++) {
						r = pixels[i] >> 16 & 0xFF;
                    	g = pixels[i] >> 8 & 0xFF;
                		b = pixels[i] & 0xFF;
                		
                        max = Math.max(r, g);
                        max = Math.max(max, b);
                        min = Math.min(r, g);
                        min = Math.min(min, b);
                        gray = (int)((max+min)/2);
                        
                        pixels[i] = 255 << 24 | gray << 16 | gray << 8 | gray;
					}
                    
                break;

                case Average:
                    for (int i = 0; i < pixels.length; i++) {
						r = pixels[i] >> 16 & 0xFF;
                    	g = pixels[i] >> 8 & 0xFF;
                		b = pixels[i] & 0xFF;
                		
                        gray = (int)((r+g+b)/3);
                        
                        pixels[i] = 255 << 24 | gray << 16 | gray << 8 | gray;
					}
                break;
                
                case GeometricMean:
                    for (int i = 0; i < pixels.length; i++) {
						r = pixels[i] >> 16 & 0xFF;
                    	g = pixels[i] >> 8 & 0xFF;
                		b = pixels[i] & 0xFF;
                		
                        gray = (int)(Math.pow(r*g*b,0.33D));
                        
                        pixels[i] = 255 << 24 | gray << 16 | gray << 8 | gray;
					}
                break;

                case Luminosity:
                    for (int i = 0; i < pixels.length; i++) {
						r = pixels[i] >> 16 & 0xFF;
                    	g = pixels[i] >> 8 & 0xFF;
                		b = pixels[i] & 0xFF;
                		
                        gray = (int)(r*0.2125+g*0.7154+b*0.0721);
                        
                        pixels[i] = 255 << 24 | gray << 16 | gray << 8 | gray;
					}
                break;
                    
                case MinimumDecomposition:
                    for (int i = 0; i < pixels.length; i++) {
						r = pixels[i] >> 16 & 0xFF;
                    	g = pixels[i] >> 8 & 0xFF;
                		b = pixels[i] & 0xFF;
                		
                        gray = (int)Math.min(r, g);
                        gray = (int)Math.min(gray, b);
                        
                        pixels[i] = 255 << 24 | gray << 16 | gray << 8 | gray;
					}
                break;
                    
                case MaximumDecomposition:
                    for (int i = 0; i < pixels.length; i++) {
						r = pixels[i] >> 16 & 0xFF;
                    	g = pixels[i] >> 8 & 0xFF;
                		b = pixels[i] & 0xFF;
                		
                        gray = (int)Math.max(r, g);
                        gray = (int)Math.max(gray, b);
                        
                        pixels[i] = 255 << 24 | gray << 16 | gray << 8 | gray;
					}
                break;
            }
    }
}