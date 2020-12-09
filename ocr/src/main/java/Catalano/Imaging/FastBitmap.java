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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import Catalano.Imaging.Filters.Grayscale;

/**
 * FastBitmap for manipulation of images.
 * @author Diego Catalano
 */
public class FastBitmap {
    Bitmap b;
    int[] pixels;
    private CoordinateSystem cSystem;
    private boolean isGrayscale = false;
    private int strideX, strideY;
    
    /**
     * Coodinate system.
     */
    public static enum CoordinateSystem {
        /**
         * Represents X and Y.
         */
        Cartesian,
        
        /**
         * Represents I and J.
         */
        Matrix
    };
	
    /**
        * Color space.
        */
    public static enum ColorSpace {
        /**
            * Grayscale.
            */
        Grayscale,
        /**
            * RGB.
            */
        RGB
    };
    
    /**
     * Initialize a new instance of the FastBitmap class.
     */
    public FastBitmap() {}

    /**
     * Initialize a new instance of the FastBitmap class.
     * @param fastBitmap FastBitmap type.
     */
    public FastBitmap(FastBitmap fastBitmap){
        this.b = fastBitmap.toBitmap();
        setCoordinateSystem(fastBitmap.getCoordinateSystem());
        refresh();

        if (fastBitmap.isRGB())
            isGrayscale = false;
        else
            isGrayscale = true;
    }
    
    /**
     * Initializes a new instance of the FastBitmap class.
     * @param pathname Pathname.
     */
    public FastBitmap(String pathname){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        opt.inPreferredConfig = Config.ARGB_8888;
        
        this.b = BitmapFactory.decodeFile(pathname, opt);
        //MT change begin
        setCoordinateSystem(FastBitmap.CoordinateSystem.Matrix);
        refresh();
        //MT change end
    }

    /**
     * Initialize a new instance of the FastBitmap class.
     * This constructor is creating OutOfMemory exception with large resolution.
     * @param bitmap Bitmap type.
     */
    public FastBitmap(Bitmap bitmap){
        if(bitmap.isMutable()){
            this.b = bitmap;
            setCoordinateSystem(FastBitmap.CoordinateSystem.Matrix);
            refresh();
        }
        else{
            throw new IllegalArgumentException("Bitmap needs to be mutable.");
        }
    }

    /**
     * Initialize a new instance of the FastBitmap class.
     * @param width Width.
     * @param height Height.
     */
    public FastBitmap(int width, int height){
        b = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        setCoordinateSystem(FastBitmap.CoordinateSystem.Matrix);
        refresh();
    }

    /**
     * Initialize a new instance of the FastBitmap class.
     * @param width Width.
     * @param height Height.
     * @param colorSpace Color space.
     */
    public FastBitmap(int width, int height, ColorSpace colorSpace){
        b = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        setCoordinateSystem(FastBitmap.CoordinateSystem.Matrix);
        refresh();
        if (colorSpace == ColorSpace.Grayscale) isGrayscale = true;
    }
    
    /**
     * Initialize a new instance of the FastBitmap class.
     * @param image Array.
     */
    public FastBitmap(int[][] image){
        this.b = Bitmap.createBitmap(image[0].length, image.length, Config.ARGB_8888);
        isGrayscale = true;
        setCoordinateSystem(FastBitmap.CoordinateSystem.Matrix);
        refresh();
        matrixToImage(image);
    }
    
    /**
     * Initialize a new instance of the FastBitmap class.
     * @param image Array.
     */
    public FastBitmap(int[][][] image){
    	this.b = Bitmap.createBitmap(image[0][0].length, image[0].length, Config.ARGB_8888);
    	isGrayscale = false;
    	setCoordinateSystem(FastBitmap.CoordinateSystem.Matrix);
        refresh();
        matrixToImage(image);
    }
    
    /**
     * Clear all the image.
     */
    public void Clear(){
    	for (int i = 0; i < pixels.length; i++) {
			pixels[i] = 0;
		}
    }
    
    /**
     * Read pixels from a bitmap.
     */
    private void refresh(){
    	pixels = new int[b.getWidth() * b.getHeight()];
    	b.getPixels(pixels, 0, getWidth(), 0, 0, b.getWidth(), b.getHeight());
    }

    /**
     * Get Height.
     * @return height.
     */
    public int getHeight(){
        return b.getHeight();
    }

    /**
     * Get Width.
     * @return Width.
     */
    public int getWidth(){
        return b.getWidth();
    }

    /**
     * Get the size of the image.
     * @return Number of pixels.
     */
    public int getSize() {
        return pixels.length;
    }
    
    /**
     * Get the data from the bitmap.
     * @return Data.
     */
    public int[] getData(){
    	return this.pixels;
    }
    
    /**
     * Set the data in the bitmap.
     * @param data Data.
     */
    public void setData(int[] data){
    	this.pixels = data;
    }

    /**
     * Get Color space.
     * @return ColorSpace.
     */
    public ColorSpace getColorSpace(){
        if (isGrayscale)
                return ColorSpace.Grayscale;
        else
                return ColorSpace.RGB;
    }
    
    /**
     * Get the actually coordinate system.
     * @return Coordinate system.
     */
    public CoordinateSystem getCoordinateSystem(){
        return cSystem;
    }
    
    /**
     * Set coordinate system.
     * @param coSystem Coordinate System.
     */
    public void setCoordinateSystem(CoordinateSystem coSystem){
    	this.cSystem = coSystem;
        if (coSystem == CoordinateSystem.Matrix){
            this.strideX = getWidth();
            this.strideY = 1;
        }
        else{
            this.strideX = 1;
            this.strideY = getWidth();
        }
    }
    
    /**
     * Convert the image to array of double representation.
     * @return Array of the image.
     */
    public double[] toArrayGrayAsDouble(){
        double[] array = new double[getHeight()*getWidth()];
        for (int i = 0; i < array.length; i++) {
            array[i] = getGray(i);
        }
        return array;
    }
    
    /**
     * Convert the image to array of double representation.
     * @return Array of the image.
     */
    public int[] toArrayGrayAsInt(){
        int[] array = new int[getHeight()*getWidth()];
        for (int i = 0; i < array.length; i++) {
            array[i] = getGray(i);
        }
        return array;
    }
    
    /**
     * Convert the image to array of double representation.
     * @return Array of the image.
     */
    public float[] toArrayGrayAsFloat(){
        float[] array = new float[getHeight()*getWidth()];
        for (int i = 0; i < array.length; i++) {
            array[i] = getGray(i);
        }
        return array;
    }
    
    /**
     * Convert the image to matrix of integer representation.
     * @return Matrix of the image.
     */
    public int[][] toMatrixGrayAsInt(){
        int height = getHeight();
        int width = getWidth();
        
        int[][] image = new int[height][width];
        int idx = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                image[i][j] = getGray(idx++);
            }
        }
        
        return image;
    }
    
    /**
     * Convert the image to matrix of double representation.
     * @return Matrix of the image.
     */
    public double[][] toMatrixGrayAsDouble(){
        int height = getHeight();
        int width = getWidth();
        
        double[][] image = new double[height][width];
        int idx = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                image[i][j] = getGray(idx++);
            }
        }
        
        return image;
    }
    
    /**
     * Convert the image to matrix of float representation.
     * @return Matrix of the image.
     */
    public float[][] toMatrixGrayAsFloat(){
        int height = getHeight();
        int width = getWidth();
        
        float[][] image = new float[height][width];
        int idx = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                image[i][j] = getGray(idx++);
            }
        }
        
        return image;
    }
    
    /**
     * Convert the image to matrix of integer representation.
     * @return Matrix of the image.
     */
    public int[][][] toMatrixRGBAsInt(){
        int height = getHeight();
        int width = getWidth();
        
        int[][][] image = new int[height][width][3];
        int idx = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                image[i][j][0] = getRed(idx);
                image[i][j][1] = getGreen(idx);
                image[i][j][2] = getBlue(idx);
                idx++;
            }
        }
        
        return image;
    }
    
    /**
     * Convert the image to matrix of integer representation.
     * @return Matrix of the image.
     */
    public double[][][] toMatrixRGBAsDouble(){
        int height = getHeight();
        int width = getWidth();
        
        double[][][] image = new double[height][width][3];
        int idx = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                image[i][j][0] = getRed(idx);
                image[i][j][1] = getGreen(idx);
                image[i][j][2] = getBlue(idx);
                idx++;
            }
        }
        
        return image;
    }
    
    /**
     * Convert the image to matrix of integer representation.
     * @return Matrix of the image.
     */
    public float[][][] toMatrixRGBAsFloat(){
        int height = getHeight();
        int width = getWidth();
        
        float[][][] image = new float[height][width][3];
        int idx = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                image[i][j][0] = getRed(idx);
                image[i][j][1] = getGreen(idx);
                image[i][j][2] = getBlue(idx);
                idx++;
            }
        }
        
        return image;
    }
    
    /**
     * Convert Array to FastBitmap.
     * @param image Array.
     */
    public void matrixToImage(int image[][]){
        int idx = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                setGray(idx++, image[x][y]);
            }
        }
    }
    
    /**
     * Convert Array to FastBitmap.
     * @param image Array.
     */
    public void matrixToImage(float image[][]){
        int idx = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                setGray(idx++, (int)image[x][y]);
            }
        }
    }
    
    /**
     * Convert Array to FastBitmap.
     * @param image Array.
     */
    public void matrixToImage(double image[][]){
        int idx = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                setGray(idx++, (int)image[x][y]);
            }
        }
    }
    
    /**
     * Convert Array to FastBitmap.
     * @param image Array.
     */
    public void matrixToImage(int image[][][]){
        int idx = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                setRGB(idx++, image[x][y][0], image[x][y][1], image[x][y][2]);
            }
        }
    }
    
    /**
     * Convert Array to FastBitmap.
     * @param image Array.
     */
    public void matrixToImage(float image[][][]){
        int idx = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                setRGB(idx++, (int)image[x][y][0], (int)image[x][y][1], (int)image[x][y][2]);
            }
        }
    }
    
    /**
     * Convert Array to FastBitmap.
     * @param image Array.
     */
    public void matrixToImage(double image[][][]){
        int idx = 0;
        for (int x = 0; x < image.length; x++) {
            for (int y = 0; y < image[0].length; y++) {
                setRGB(idx++, (int)image[x][y][0], (int)image[x][y][1], (int)image[x][y][2]);
            }
        }
    }
    
    /**
     * Return RGB color.
     * @param offset Offset.
     * @return RGB.
     */
    public int[] getRGB(int offset){
        int[] rgb = new int[3];
        rgb[0] = pixels[offset] >> 16 & 0xFF;
        rgb[1] = pixels[offset] >> 8 & 0xFF;
        rgb[2] = pixels[offset] & 0xFF;
        return rgb;
    }
    
    /**
     * Return RGB color.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @return RGB.
     */
    public int[] getRGB(int x, int y){
        int[] rgb = new int[3];
        rgb[0] = pixels[x*getWidth()+y] >> 16 & 0xFF;
        rgb[1] = pixels[x*getWidth()+y] >> 8 & 0xFF;
        rgb[2] = pixels[x*getWidth()+y] & 0xFF;
        return rgb;
    }

    /**
     * Set RGB.
     * @param x X axis coordinates.
     * @param y Y axis coordinates.
     * @param red Red channel's value.
     * @param green Green channel's value.
     * @param blue Blue channel's value.
     */
    public void setRGB(int x, int y, int red, int green, int blue){
        pixels[x * strideX + y * strideY] = 255 << 24 | red << 16 | green << 8 | blue;
    }

    /**
     * Set RGB.
     * @param x X axis coordinates.
     * @param y Y axis coordinates.
     * @param rgb RGB color.
     */
    public void setRGB(int x, int y, int[] rgb){
         pixels[x*strideX + y*strideY] = 255 << 24 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
    }

    /**
     * Set RGB.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @param color Color.
     */
    public void setRGB(int x, int y, Color color){
    	 pixels[x*getWidth()+y] = 255 << 24 | color.r << 16 | color.g << 8 | color.b;
    }
    
    /**
     * Set RGB.
     * @param offset Offset.
     * @param color Color.
     */
    public void setRGB(int offset, Color color){
    	pixels[offset] = 255 << 24 | color.r << 16 | color.g << 8 | color.b;
    }
    
    /**
     * Set RGB value.
     * @param offset Offset.
     * @param red Red channel's value.
     * @param green Green channel's value.
     * @param blue Blue channel's value.
     */
    public void setRGB(int offset, int red, int green, int blue){
    	pixels[offset] = 255 << 24 | red << 16 | green << 8 | blue;
    }
    
    /**
     * Set RGB value.
     * @param offset Offset.
     * @param rgb RGB array.
     */
    public void setRGB(int offset, int[] rgb){
    	pixels[offset] = 255 << 24 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
    }

    /**
     * Get Gray.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @return Gray channel's value.
     */
    public int getGray(int x, int y){
        return pixels[x * strideX + y * strideY] & 0xFF;
    }

    /**
     * Get gray channel's value.
     * @param offset Offset.
     * @return Gray channel's value.
     */
    public int getGray(int offset){
    	return pixels[offset] & 0xFF;
    }
    
    /**
     * Set gray channel's value.
     * @param offset Offset.
     * @param value Gray channel's value.
     */
    public void setGray(int offset, int value){
    	pixels[offset] =  255 << 24 | value << 16 | value << 8 | value;
    }

    /**
     * Set Gray.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @param value Gray channel's value.
     */
    public void setGray(int x, int y, int value){
        pixels[x * strideX + y * strideY] =  255 << 24 | value << 16 | value << 8 | value;
    }

    /**
     * Get Red.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @return Red channel's value.
     */
    public int getRed(int x, int y){
        return pixels[x * strideX + y * strideY] >> 16 & 0xFF;
    }

    /**
     * Get red channel's value.
     * @param offset Offset.
     * @return Red channel's value.
     */
    public int getRed(int offset){
    	return pixels[offset] >> 16 & 0xFF;
    }
    
    /**
     * Set red channel's value.
     * @param offset Offset.
     * @param value Red channel's value.
     */
    public void setRed(int offset, int value){
        pixels[offset] = pixels[offset] & 0xff00ffff | value << 16;
    }

    /**
     * Set Red.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @param value Red channel's value.
     */
    public void setRed(int x, int y, int value){
        pixels[x * strideX + y * strideY] = pixels[x * strideX + y * strideY] & 0xff00ffff | value << 16;
    }

    /**
     * Get Green.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @return Green channel's value.
     */
    public int getGreen(int x, int y){
        return pixels[x * strideX + y * strideY] >> 8 & 0xFF;
    }

    /**
     * Get green channel's value.
     * @param offset Offset.
     * @return Green channel's value.
     */
    public int getGreen(int offset){
    	return pixels[offset] >> 8 & 0xFF;
    }
    
    /**
     * Set green channel's value.
     * @param offset Offset.
     * @param value Green channel's value.
     */
    public void setGreen(int offset, int value){
        pixels[offset] = pixels[offset] & 0xffff00ff | value << 8;
    }

    /**
     * Set Green.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @param value Green channel's value.
     */
    public void setGreen(int x, int y, int value){
        pixels[x * strideX + y * strideY] = pixels[x * strideX + y * strideY] & 0xffff00ff | value << 8;
    }

    /**
     * Get Blue.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @return Blue channel's value.
     */
    public int getBlue(int x, int y){
        return pixels[x * strideX + y * strideY] & 0xFF;
    }

    /**
     * Get blue channel's value.
     * @param offset Offset.
     * @return Blue channel's value.
     */
    public int getBlue(int offset){
    	return pixels[offset] & 0xFF;
    }
    
    /**
     * Set blue channel's value.
     * @param offset Offset.
     * @param value Blue channel's value.
     */
    public void setBlue(int offset, int value){
        pixels[offset] = pixels[offset] & 0xffffff00 | value;
    }

    /**
     * Set Blue.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @param value Blue channel's value.
     */
    public void setBlue(int x, int y, int value){
        pixels[x * strideX + y * strideY] = pixels[x * strideX + y * strideY] & 0xffffff00 | value;
    }
    
    /**
     * Clamp values.
     * @param value Value.
     * @param min Minimum value.
     * @param max Maximum value.
     * @return Clamped values.
     */
    public int clampValues(int value, int min, int max){
        if(value < min)
            return min;
        else if(value > max)
            return max;
        return value;
    }

    /**
     * Set image to FastBitmap.
     * @param bitmap Bitmap.
     */
    public void setImage(Bitmap bitmap){
        this.b = bitmap;
        setCoordinateSystem(CoordinateSystem.Matrix);
        pixels = new int[b.getHeight() * b.getWidth()];
        b.getPixels(pixels, 0, getWidth(), 0, 0, b.getWidth(), b.getHeight());
    }

    /**
     * Set image to FastBitmap.
     * @param fastBitmap FastBitmap.
     */
    public void setImage(FastBitmap fastBitmap){
        this.b = fastBitmap.toBitmap();
        setCoordinateSystem(fastBitmap.getCoordinateSystem());
        pixels = new int[b.getHeight() * b.getWidth()];
        b.getPixels(pixels, 0, getWidth(), 0, 0, b.getWidth(), b.getHeight());
        if (fastBitmap.isRGB())
                this.isGrayscale = false;
        else
            isGrayscale = true;
    }

    /**
     * Verify RGB space color.
     * @return True if is RGB, otherwise false.
     */
    public boolean isRGB(){
        return !isGrayscale;
    }

    /**
     * Verify Grayscale space color.
     * @return True if is Grayscale, otherwise false.
     */
    public boolean isGrayscale(){
        return isGrayscale;
    }

    /**
     * Convert any others space colors to RGB.
     */
    public void toRGB(){
        this.isGrayscale = false;
    }

     /** Convert image to grayscale.
     * This method will convert to Luminosity method.
     */
    public void toGrayscale(){
        new Grayscale().applyInPlace(this);
    }
    
    /**
     * Indicate image to grayscale.
     * 
     * This method don't convert to grayscale image.
     * 
     * Only indicate the property to grayscale, It's used to optimize some filters
     * to avoid copy in the buffer.
     * 
     * @param indicate True if need to indicate the image is grayscale, otherwise false.
     */
    public void indicateGrayscale(boolean indicate){
    	this.isGrayscale = indicate;
    }

    /**
     * Convert FastBitmap to Bitmap.
     * @return Bitmap.
     */
    public Bitmap toBitmap(){
        if (isRGB())
                b.setPixels(pixels, 0, Math.max(strideX, strideY), 0, 0, b.getWidth(), b.getHeight());
        else{
        	
        	int size = getHeight() * getWidth();
        	for (int i = 0; i < size; i++) {
				int g = pixels[i] & 0xFF;
        		pixels[i] = 255 << 24 | g << 16 | g << 8 | g;
			}
            b.setPixels(pixels, 0, Math.max(strideX, strideY), 0, 0, b.getWidth(), b.getHeight());
        }
        return b;
    }
    
    public void saveAsJPG(String pathname){
        saveAsJPG(pathname, 100);
    }
    
    public void saveAsJPG(String pathname, int quality){
        this.b = toBitmap();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(pathname);
            b.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void saveAsJPG(String pathname, int quality, int xDpi, int yDpi){
        
        this.b = toBitmap();
        
        //Get the entire file in bytes
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, quality, byteArray);
        byte[] imageData = byteArray.toByteArray();

        //Modify the metadata
        imageData[13] = 1; //resUnit
        imageData[14] = (byte) (xDpi >> 8);
        imageData[15] = (byte) (xDpi & 0xFF);
        imageData[16] = (byte) (yDpi >> 8);
        imageData[17] = (byte) (yDpi & 0xFF);

        //Write the file
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(new File(pathname));
            fileOutputStream.write(imageData);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveAsPNG(String pathname){
        
        this.b = toBitmap();
        
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(pathname);
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Recycle.
     * Free the native object associated with this bitmap, and clear the reference to the pixel data.
     * This will not free the pixel data synchronously; it simply allows it to be garbage collected if
     * there are no other references.
     */
    public void recycle(){
    	this.b.recycle();
    }
}