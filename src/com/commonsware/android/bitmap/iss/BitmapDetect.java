package com.commonsware.android.bitmap.iss;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.StringRes;
import android.util.Log;

/**
 * Created by nico.deceulaer on 21/10/2014.
 */
public class BitmapDetect {
    private static final int    CARD_HEIGHT = 180;
    private static final int    CARD_WIDTH  = 320;
    private static final int    MIN_WIDHT   = ( 8 + 8 + 8 );
    private static final int    MAGIC_TOKEN = 0xBADCAFE;
    private static final String TAG         = "BitmapDetect";

    private Bitmap  mBitmap = null;

    private BitmapDetect( Bitmap bitmap ) {
        this.mBitmap = bitmap;
    }

    // is this bitmap the size we expect for a Launcher Icon ?
    private boolean CheckSize() {
        int height = mBitmap.getHeight();
        int width  = mBitmap.getWidth();

        return (height == CARD_HEIGHT) && (width == CARD_WIDTH );
    }

    // push secret into consecutive LSB bits of stream
    private void EncodeLsb( int[] stream, int[] secret ) {
        int alpha, red, green, blue, color;
        int bitCnt  = 0;
        int byteCnt = 0;
        int secretByte = 0;
        int secretBit  = 0;

        for ( int i = 0; i < 32 * secret.length; i++ ) {
            if( bitCnt == 0 ) {
                // take next byte to scrape of bits
                secretByte = secret[byteCnt++];
                bitCnt = 32;
            }
            // now scrape of a single bit
            secretBit  = (secretByte >> 31) & 1;  // start with MSB of secret stream
            secretByte = secretByte << 1;
            bitCnt--;

            /*
            encode the data in the LSB of the RED CHANNEL

            http://en.wikipedia....n_the_human_eye
            http://en.wikipedia.org/wiki/Cone_cell

            Sensitivity in daylight vs. darkness
            Our eyes are most sensitive in daylight to green light around 550nm and slightly less sensitive to yellow light.
            They are approximately half as sensitive to orange light and only about a tenth as sensitive to red and violet
            light. Therefore, for example, we can see a 100-Watt green light bulb three times farther away than we can a
            red or violet bulb of the same power. A 532nm Nd:YAG laser diode module will emit highly visible green
            output and is, therefore, a popular choice for outdoor pointing and alignment applications. In darkness,
            however, we are most sensitive to blues and least sensitive to reds.
            */
            color     = stream[i];
            alpha     =  Color.alpha( color );
            red       = (Color.red(color) & 0xfffffffe) | secretBit;
            green     =  Color.green(color);
            blue      =  Color.blue(color);
            stream[i] =  Color.argb(alpha, red, green, blue);
        }
    }

    // extract secret info from consecutive LSB bits of stream
    private void DecodeLsb( int[] stream, int[] secret ) {
        int color;
        int bitCnt  = 0;
        int byteCnt = 0;
        int secretByte = 0;
        int secretBit;

        int i = 0;
        while( byteCnt < secret.length) {
            color      = stream[i++];
            secretBit  = Color.red(color) & 1;
            secretByte = (secretByte << 1 ) | secretBit;
            bitCnt++;
            if( bitCnt == 32 ) {
                secret[byteCnt++] = secretByte;
                secretByte        = 0;
                bitCnt            = 0;
            }
        }
    }

    static public Bitmap EncodeFingerprint( Bitmap bitmap, int token )
    {
        BitmapDetect detector = new BitmapDetect( bitmap );
        return detector.EncodeFingerprint( token );
    }

    private Bitmap EncodeFingerprint( int token ) {
        Bitmap newBitmap  = mBitmap.copy( mBitmap.getConfig(), true);
        int width         = mBitmap.getWidth();
        int[] secret      = { MAGIC_TOKEN, token, MAGIC_TOKEN };

        Log.i(TAG, "EncodeData(" + token + ")");

        int[] pixels = new int[ width];
        newBitmap.getPixels( pixels, 0, width, 0, 0, width, 1 );
        Log.i(TAG, "pixels before: " +
                Integer.toHexString(pixels[0]) + " " +
                Integer.toHexString(pixels[1]) + " " +
                Integer.toHexString(pixels[2]) );

        Log.i(TAG, "secret: " +
                Integer.toHexString(secret[0]) + " " +
                Integer.toHexString(secret[1]) + " " +
                Integer.toHexString(secret[2])  );

        EncodeLsb( pixels, secret );

        newBitmap.setPixels(pixels, 0, width, 0, 0, width, 1 );

        return newBitmap;
    }


    // See if this image has some internal data hidden
    // returns -1 if nothing found
    // returns number if some hidden data was found
    static public int DetectFingerprint( Bitmap bitmap )
    {
        BitmapDetect detector = new BitmapDetect( bitmap );
        return detector.DetectFingerprint();
    }

    private int DetectFingerprint() {
        int    width  = mBitmap.getWidth();
        int[]  pixels = new int[ width ];
        int[]  secret = {-1, -1, -1};

        Log.i(TAG, "DetectFingerprint");
        mBitmap.getPixels( pixels, 0, width, 0, 0, width, 1 );
        Log.i(TAG, "pixels: " +
                Integer.toHexString(pixels[0]) + " " +
                Integer.toHexString(pixels[1]) + " " +
                Integer.toHexString(pixels[2]) );

        DecodeLsb( pixels, secret );
        Log.i(TAG, "secret: " +
                    Integer.toHexString(secret[0]) + " " +
                    Integer.toHexString(secret[1]) + " " +
                    Integer.toHexString(secret[2])  );

        if( secret[0] == MAGIC_TOKEN &&
            secret[2] == MAGIC_TOKEN )
        {
            Log.i(TAG, "fingerprint found: " + secret[1] );
            return secret[1];
        }
        else
        {
            return -1;
        }
    }
}