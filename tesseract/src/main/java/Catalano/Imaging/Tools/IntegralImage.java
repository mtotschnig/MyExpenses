// Catalano Imaging Library
// The Catalano Framework
//
// Copyright © Diego Catalano, 2012-2016
// diego.catalano at live.com
//
// Copyright © Andrew Kirillov, 2007-2013
// andrew.kirillov at gmail.com
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

package Catalano.Imaging.Tools;

import Catalano.Imaging.FastBitmap;

/**
 * Integral Image.
 * <br /> The class implements integral image concept, which is described by Viola and Jones in: <b>P. Viola and M. J. Jones, "Robust real-time face detection", Int. Journal of Computer Vision 57(2), pp. 137–154, 2004</b>.
 * <para> An integral image <b>I</b> of an input image <b>G</b> is defined as the image in which the intensity at a pixel position is equal to the sum of the intensities of all the pixels above and to the left of that position in the original image.</para>
 * @author Diego Catalano
 */
public class IntegralImage {
    
    /**
     * Provides access to internal array keeping integral image data.
     */
    protected int[][] integralImage = null;
    
    private int width;
    private int height;

    /**
     * Initializes a new instance of the IntegralImage class.
     * @param fastBitmap An image to be processed.
     */
    public IntegralImage(FastBitmap fastBitmap) {
        this(fastBitmap, 1);
    }
    
    /**
     * Initializes a new instance of the IntegralImage class.
     * @param fastBitmap An image to be processed.
     * @param power Power of the number.
     */
    public IntegralImage(FastBitmap fastBitmap, int power) {
        this.width = fastBitmap.getWidth();
        this.height = fastBitmap.getHeight();
        Process(fastBitmap, power);
    }
    
    /**
     * Initializes a new instance of the IntegralImage class.
     * @param width Image width.
     * @param height Image height.
     */
    protected IntegralImage(int width, int height){
        this.width = width;
        this.height = height;
        this.integralImage = new int[height + 1][width  + 1];
    }

    /**
     * Width of the source image the integral image was constructed for.
     * @return Width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Height of the source image the integral image was constructed for.
     * @return Height.
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Provides access to internal array keeping integral image data.
     * @return Data.
     */
    public int[][] getInternalData(){
        return integralImage;
    }
    
    /**
     * Provides access to internal value keeping integral image data.
     * @param x X axis coordinate.
     * @param y Y axis coordinate.
     * @return Integral value.
     */
    public int getInternalData(int x, int y){
        return integralImage[x][y];
    }
    
    /**
     * Construct integral image from source grayscale image.
     * @param fastBitmap Image to be processed.
     * @return Returns integral image.
     */
    public static IntegralImage FromFastBitmap(FastBitmap fastBitmap){
        return FromFastBitmap(fastBitmap, 1);
    }
    
    /**
     * Construct integral image from source grayscale image.
     * @param fastBitmap Image to be processed.
     * @param power Power of the number.
     * @return Returns integral image.
     */
    public static IntegralImage FromFastBitmap(FastBitmap fastBitmap, int power){
        // get source image size
        int width  = fastBitmap.getWidth();
        int height = fastBitmap.getHeight();

        // create integral image
        IntegralImage im = new IntegralImage( width, height );
        int[][] integralImage = im.integralImage;

        for (int i = 1; i <= height; i++) {
            
            int rowSum = 0;
            
            for (int j = 1; j <= width; j++) {
                rowSum += Math.pow(fastBitmap.getGray(i - 1, j - 1), power);
                integralImage[i][j] = rowSum + integralImage[i - 1][j];
            }
        }
        
        return im;
    }
    
    /**
     * Process image.
     * @param fastBitmap Image to be processed.
     */
    private void Process(FastBitmap fastBitmap, int power){
        if (!fastBitmap.isGrayscale()) {
            try {
                throw new Exception("IntegralImage works only with Grayscale images");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        integralImage = new int[height + 1][width + 1];
        
        for (int x = 1; x < height + 1; x++) {
            int rowSum = 0;
            for (int y = 1; y < width + 1; y++) {
                rowSum += Math.pow(fastBitmap.getGray(x - 1, y - 1), power);
                integralImage[x][y] = rowSum + integralImage[x - 1][y];
            }
        }
    }
    
    /**
     * Calculate sum of pixels in the specified rectangle.
     * @param x1 Coordinate of left-top rectangle's corner.
     * @param y1 Coordinate of left-top rectangle's corner.
     * @param x2 Coordinate of right-bottom rectangle's corner.
     * @param y2 Coordinate of right-bottom rectangle's corner.
     * @return Returns sum of pixels in the specified rectangle.
     */
    public int getRectangleSum(int x1, int y1, int x2, int y2){
        // check if requested rectangle is out of the image
        if ( ( x2 < 0 ) || ( y2 < 0 ) || ( x1 >= height ) || ( y1 >= width ) )
            return 0;

        if ( x1 < 0 ) x1 = 0;
        if ( y1 < 0 ) y1 = 0;

        x2++;
        y2++;

        if ( x2 > height )  x2 = height;
        if ( y2 > width ) y2 = width;

        return integralImage[x2][y2] + integralImage[x1][y1] - integralImage[x1][y2] - integralImage[x2][y2];
    }
    
    /**
     * Calculate sum of pixels in the specified rectangle.
     * @param x Coordinate of central point of the rectangle.
     * @param y Coordinate of central point of the rectangle.
     * @param radius Radius of the rectangle.
     * @return Returns sum of pixels in the specified rectangle.
     */
    public int getRectangleSum(int x, int y, int radius){
        return getRectangleSum(x - radius, y - radius, x + radius, y + radius);
    }
    
    /**
     * Calculate horizontal (X) haar wavelet at the specified point.
     * @param x X coordinate of the point to calculate wavelet at.
     * @param y Y coordinate of the point to calculate wavelet at.
     * @param radius Wavelet size to calculate.
     * @return Returns value of the horizontal wavelet at the specified point.
     */
    public int getHarrXWavelet(int x, int y, int radius){
        int y1 = y - radius;
        int y2 = y + radius - 1;

        int a = getRectangleSum( x, y1, x + radius - 1, y2 );
        int b = getRectangleSum( x - radius, y1, x - 1, y2 );

        return (int) ( a - b );
    }
    
    /**
     * Calculate vertical (Y) haar wavelet at the specified point.
     * @param x X coordinate of the point to calculate wavelet at.
     * @param y Y coordinate of the point to calculate wavelet at.
     * @param radius Wavelet size to calculate.
     * @return Returns value of the vertical wavelet at the specified point.
     */
    public int getHaarYWavelet( int x, int y, int radius ){
        int x1 = x - radius;
        int x2 = x + radius - 1;

        float a = getRectangleSum( x1, y, x2, y + radius - 1 );
        float b = getRectangleSum( x1, y - radius, x2, y - 1 );

        return (int) ( a - b );
    }
    
    /**
     * Calculate mean value of pixels in the specified rectangle.
     * @param x1 X coordinate of left-top rectangle's corner.
     * @param y1 Y coordinate of left-top rectangle's corner.
     * @param x2 X coordinate of right-bottom rectangle's corner.
     * @param y2 Y coordinate of right-bottom rectangle's corner.
     * @return Returns mean value of pixels in the specified rectangle.
     */
    public float getRectangleMean(int x1, int y1, int x2, int y2){
        // check if requested rectangle is out of the image
        if ( ( x2 < 0 ) || ( y2 < 0 ) || ( x1 >= height ) || ( y1 >= width ) )
            return 0;

        if ( x1 < 0 ) x1 = 0;
        if ( y1 < 0 ) y1 = 0;

        x2++;
        y2++;

        if ( x2 > height ) x2 = height;
        if ( y2 > width ) y2 = width;

        // return sum divided by actual rectangles size
        return (float) ( (double) ( integralImage[x2][y2] + integralImage[x1][y1] - integralImage[x1][y2] - integralImage[x2][y1] ) /
            (double) ( ( x2 - x1 ) * ( y2 - y1 ) ) );
    }
    
    /**
     * Calculate mean value of pixels in the specified rectangle.
     * @param x X coordinate of central point of the rectangle.
     * @param y Y coordinate of central point of the rectangle.
     * @param radius Radius of the rectangle.
     * @return Returns mean value of pixels in the specified rectangle.
     */
    public float getRectangleMean(int x, int y, int radius){
        return getRectangleMean(x - radius, y - radius, x + radius, y + radius);
    }
    
    /**
     * Calculate mean value of pixels in the specified rectangle without checking it's coordinates.
     * @param x1 X coordinate of left-top rectangle's corner.
     * @param y1 Y coordinate of left-top rectangle's corner.
     * @param x2 X coordinate of right-bottom rectangle's corner.
     * @param y2 Y coordinate of right-bottom rectangle's corner.
     * @return Returns mean value of pixels in the specified rectangle.
     */
    public int getRectangleSumUnsafe( int x1, int y1, int x2, int y2 ){
        x2++;
        y2++;

        return integralImage[x2][y2] + integralImage[x1][y1] - integralImage[x2][y1] - integralImage[x1][y2];
    }
    
    /**
     * Calculate sum of pixels in the specified rectangle without checking it's coordinates.
     * @param x X coordinate of central point of the rectangle.
     * @param y Y coordinate of central point of the rectangle.
     * @param radius Radius of the rectangle.
     * @return Returns sum of pixels in the specified rectangle.
     */
    public int getRectangleSumUnsafe( int x, int y, int radius ){
        return getRectangleSumUnsafe( x - radius, y - radius, x + radius, y + radius );
    }
    
    /**
     * Calculate mean value of pixels in the specified rectangle without checking it's coordinates.
     * @param x1 X coordinate of left-top rectangle's corner.
     * @param y1 Y coordinate of left-top rectangle's corner.
     * @param x2 X coordinate of right-bottom rectangle's corner.
     * @param y2 Y coordinate of right-bottom rectangle's corner.
     * @return Returns mean value of pixels in the specified rectangle.
     */
    public float getRectangleMeanUnsafe( int x1, int y1, int x2, int y2 ){
        x2++;
        y2++;

        // return sum divided by actual rectangles size
        return (float) ( (double) ( integralImage[x2][y2] + integralImage[x1][y1] - integralImage[x1][y2] - integralImage[x2][y1] ) /
            (double) ( ( x2 - x1 ) * ( y2 - y1 ) ) );
    }
    
    /**
     *  Calculate mean value of pixels in the specified rectangle without checking it's coordinates.
     * @param x X coordinate of central point of the rectangle.
     * @param y Y coordinate of central point of the rectangle.
     * @param radius Radius of the rectangle.
     * @return Returns mean value of pixels in the specified rectangle.
     */
    public float getRectangleMeanUnsafe( int x, int y, int radius ){
        return getRectangleMeanUnsafe( x - radius, y - radius, x + radius, y + radius );
    }
}