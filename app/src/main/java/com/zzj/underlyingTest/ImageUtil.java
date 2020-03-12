package com.zzj.underlyingTest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import com.zzj.underlying.DataConver;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;


public class ImageUtil {
    private static final int VALUE_FOR_4_ALIGN = 0b11;
    private static final int VALUE_FOR_2_ALIGN = 0b01;


    /**
     * Bitmap转化为ARGB数据，再转化为NV21数据
     *
     * @param src    传入的Bitmap，格式为{@link Bitmap.Config#ARGB_8888}
     * @param width  NV21图像的宽度
     * @param height NV21图像的高度
     * @return nv21数据
     */
    public static byte[] bitmapToNv21(Bitmap src, int width, int height) {
        if (src != null && src.getWidth() >= width && src.getHeight() >= height) {
            int[] argb = new int[width * height];
            src.getPixels(argb, 0, width, 0, 0, width, height);
            return DataConver.argbToNv21(argb, width, height);
        } else {
            return null;
        }
    }

    /**
     * ARGB数据转化为NV21数据
     *
     * @param argb   argb数据
     * @param width  宽度
     * @param height 高度
     * @return nv21数据
     */
    private static byte[] argbToNv21(int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int index = 0;
        byte[] nv21 = new byte[width * height * 3 / 2];
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                int R = (argb[index] & 0xFF0000) >> 16;
                int G = (argb[index] & 0x00FF00) >> 8;
                int B = argb[index] & 0x0000FF;
                int Y = (66 * R + 129 * G + 25 * B + 128 >> 8) + 16;
                int U = (-38 * R - 74 * G + 112 * B + 128 >> 8) + 128;
                int V = (112 * R - 94 * G - 18 * B + 128 >> 8) + 128;
                nv21[yIndex++] = (byte) (Y < 0 ? 0 : (Y > 255 ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21.length - 2) {
                    nv21[uvIndex++] = (byte) (V < 0 ? 0 : (V > 255 ? 255 : V));
                    nv21[uvIndex++] = (byte) (U < 0 ? 0 : (U > 255 ? 255 : U));
                }

                ++index;
            }
        }
        return nv21;
    }

    /**
     * bitmap转化为bgr数据，格式为{@link Bitmap.Config#ARGB_8888}
     *
     * @param image 传入的bitmap
     * @return bgr数据
     */
    public static byte[] bitmapToBgr(Bitmap image) {
        if (image == null) {
            return null;
        }
        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        image.copyPixelsToBuffer(buffer);
        byte[] temp = buffer.array();
        byte[] pixels = new byte[(temp.length / 4) * 3];
        for (int i = 0; i < temp.length / 4; i++) {
            pixels[i * 3] = temp[i * 4 + 2];
            pixels[i * 3 + 1] = temp[i * 4 + 1];
            pixels[i * 3 + 2] = temp[i * 4];
        }
        return pixels;
    }

    /**
     * 裁剪bitmap
     *
     * @param bitmap 传入的bitmap
     * @param rect   需要被裁剪的区域
     * @return 被裁剪后的bitmap
     */
    public static Bitmap imageCrop(Bitmap bitmap, Rect rect) {
        if (bitmap == null || rect == null || rect.isEmpty() || bitmap.getWidth() < rect.right || bitmap.getHeight() < rect.bottom) {
            return null;
        }
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), null, false);
    }

    public static Bitmap getBitmapFromUri(Uri uri, Context context) {
        if (uri == null || context == null) {
            return null;
        }
        try {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree) {
        if (b == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotateDegree);
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
    }

    /**
     * 确保传给引擎的BGR24数据宽度为4的倍数
     *
     * @param bitmap 传入的bitmap
     * @return 调整后的bitmap
     */
    public static Bitmap alignBitmapForBgr24(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 4) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        boolean needAdjust = false;

        //保证宽度是4的倍数
        if ((width & VALUE_FOR_4_ALIGN) != 0) {
            width &= ~VALUE_FOR_4_ALIGN;
            needAdjust = true;
        }

        if (needAdjust) {
            bitmap = imageCrop(bitmap, new Rect(0, 0, width, height));
        }
        return bitmap;
    }

    /**
     * 确保传给引擎的NV21数据宽度为4的倍数，高为2的倍数
     *
     * @param bitmap 传入的bitmap
     * @return 调整后的bitmap
     */
    public static Bitmap alignBitmapForNv21(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 4 || bitmap.getHeight() < 2) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        boolean needAdjust = false;
        //保证宽度是4的倍数
        if ((width & VALUE_FOR_4_ALIGN) != 0) {
            width &= ~VALUE_FOR_4_ALIGN;
            needAdjust = true;
        }

        //保证高度是2的倍数
        if ((height & VALUE_FOR_2_ALIGN) != 0) {
            height--;
            needAdjust = true;
        }

        if (needAdjust) {
            bitmap = imageCrop(bitmap, new Rect(0, 0, width, height));
        }
        return bitmap;
    }

    //加载原图片
    public static Bitmap decodeImage(String filePath) {

        Bitmap bitmap;
        try {
            BitmapFactory.Options o1 = new BitmapFactory.Options();
            o1.inJustDecodeBounds = true;
            Bitmap bmp = BitmapFactory.decodeFile(filePath, o1);
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            if (o1.outWidth > 2000 || o1.outHeight > 2000) {
                o2.inJustDecodeBounds = false;
                o2.inSampleSize = 2;
            } else {
                o2.inJustDecodeBounds = false;
                o2.inSampleSize = 1;
            }
            bitmap = BitmapFactory.decodeFile(filePath, o2);
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }
            Bitmap temp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (!temp.equals(bitmap)) {
                bitmap.recycle();
            }
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getScaleBitmap(Bitmap bitmap, int size) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = 1.0f;
        if (width > height) {
            scale = ((float) size) / height;
        } else {
            scale = ((float) size) / width;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap scaledBitMap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

        int scaledWidth = scaledBitMap.getWidth();
        int scaledHeight = scaledBitMap.getHeight();
        Bitmap cutBitmap;
        if (scaledWidth > scaledHeight) {
            cutBitmap = Bitmap.createBitmap(scaledBitMap, (scaledWidth - scaledHeight) / 2, 0, scaledHeight, scaledHeight);
        } else {
            cutBitmap = Bitmap.createBitmap(scaledBitMap, 0, (scaledHeight - scaledWidth) / 2, scaledWidth, scaledWidth);
        }

        return cutBitmap;
    }


    public static Bitmap getBitmap(String url) {
        Bitmap bm = null;
        try {
            URL iconUrl = new URL(url);
            URLConnection conn = iconUrl.openConnection();
            HttpURLConnection http = (HttpURLConnection) conn;

            int length = http.getContentLength();

            conn.connect();
            // 获得图像的字符流
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, length);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();// 关闭流
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bm;
    }
}
