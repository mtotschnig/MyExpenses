// Catalano Android Imaging Library
// The Catalano Framework
//
// Copyright © Diego Catalano, 2012-2016
// diego.catalano at live.com
//
// Copyright © Andrew Kirillov, 2007-2008
// andrew.kirillov@gmail.com
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
import Catalano.Imaging.Tools.IntegralImage;

/**
 * Adaptive thresholding using the integral image.
 * @author Diego Catalano
 */
public class BradleyLocalThreshold implements IApplyInPlace{
    private int windowSize = 41;
    private float pixelBrightnessDifferenceLimit = 0.15f;

    /**
     * Initialize a new instance of the BradleyLocalThreshold class.
     */
    public BradleyLocalThreshold() {}
    
    /**
     * Initialize a new instance of the BradleyLocalThreshold class.
     * @param windowSize Window size to calculate average value of pixels for.
     */
    public BradleyLocalThreshold(int windowSize) {
        this.windowSize = windowSize;
    }
    
    /**
     * Initialize a new instance of the BradleyLocalThreshold class.
     * @param windowSize Window size to calculate average value of pixels for.
     * @param pixelBrightnessDiff Brightness difference limit between processing pixel and average value across neighbors.
     */
    public BradleyLocalThreshold(int windowSize, float pixelBrightnessDiff) {
        this.windowSize = windowSize;
    }

    /**
     * Window size to calculate average value of pixels for.
     * @return Window size.
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Window size to calculate average value of pixels for.
     * @param windowSize Window size.
     */
    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * Brightness difference limit between processing pixel and average value across neighbors.
     * @return Brightness difference.
     */
    public float getPixelBrightnessDifferenceLimit() {
        return pixelBrightnessDifferenceLimit;
    }

    /**
     * Brightness difference limit between processing pixel and average value across neighbors.
     * @param pixelBrightnessDifferenceLimit Brightness difference.
     */
    public void setPixelBrightnessDifferenceLimit(float pixelBrightnessDifferenceLimit) {
        this.pixelBrightnessDifferenceLimit = pixelBrightnessDifferenceLimit;
    }

    @Override
    public void applyInPlace(FastBitmap fastBitmap) {
        IntegralImage im = new IntegralImage(fastBitmap);
        
        int width    = fastBitmap.getWidth();
        int height   = fastBitmap.getHeight();
        int widthM1  = width - 1;
        int heightM1 = height - 1;
        
        int radius = windowSize / 2;
        
        float avgBrightnessPart = 1.0f - pixelBrightnessDifferenceLimit;
        
        for (int x = 0; x < height; x++) {
            
                int x1 = x - radius;
                int x2 = x + radius;

                if ( x1 < 0 )
                    x1 = 0;
                if ( x2 > heightM1 )
                    x2 = heightM1;
            
            for (int y = 0; y < width; y++) {
                    int y1 = y - radius;
                    int y2 = y + radius;

                    if ( y1 < 0 )
                        y1 = 0;
                    if ( y2 > widthM1 )
                        y2 = widthM1;
                    
                    
                    int gray = fastBitmap.getGray(x, y) < (int)(im.getRectangleMeanUnsafe(x1, y1, x2, y2) * avgBrightnessPart) ? 0 : 255;
                    fastBitmap.setGray(x, y, gray);
            }
        }
    }
}