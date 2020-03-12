//
// Created by zhangzj on 2019/4/23.
//

#include "com_zzj_underlying_DataConver.h"

JNIEXPORT jstring JNICALL Java_com_zzj_underlying_DataConver_getTestString
        (JNIEnv *env, jclass) {
    return env->NewStringUTF("SDK SUCCESS");
}

JNIEXPORT jbyteArray JNICALL Java_com_zzj_underlying_DataConver_argbToNv21
        (JNIEnv *env, jclass, jintArray arr, jint width, jint height) {

    jint *argb;
    argb = env->GetIntArrayElements(arr, NULL);

    int frameSize = width * height;
    int yIndex = 0;
    int uvIndex = frameSize;
    int index = 0;

    int nv21Length = width * height * 3 / 2;
    jbyte *nv21 = new jbyte[nv21Length];

    for (int j = 0; j < height; ++j) {
        for (int i = 0; i < width; ++i) {
            int R = (argb[index] & 0xFF0000) >> 16;
            int G = (argb[index] & 0x00FF00) >> 8;
            int B = argb[index] & 0x0000FF;
            int Y = (66 * R + 129 * G + 25 * B + 128 >> 8) + 16;
            int U = (-38 * R - 74 * G + 112 * B + 128 >> 8) + 128;
            int V = (112 * R - 94 * G - 18 * B + 128 >> 8) + 128;
            nv21[yIndex++] = (unsigned char) (Y < 0 ? 0 : (Y > 255 ? 255 : Y));
            if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21Length - 2) {
                nv21[uvIndex++] = (unsigned char) (V < 0 ? 0 : (V > 255 ? 255 : V));
                nv21[uvIndex++] = (unsigned char) (U < 0 ? 0 : (U > 255 ? 255 : U));
            }

            ++index;
        }
    }
    jbyteArray result = env->NewByteArray(nv21Length);
    env->SetByteArrayRegion(result, 0, nv21Length, nv21);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_zzj_underlying_DataConver_nv21Rotate90
        (JNIEnv *env, jclass, jbyteArray arr, jint width, jint height) {

    jbyte *nv21_data = env->GetByteArrayElements(arr, NULL);

    int y_size = width * height;
    int buffser_size = y_size * 3 / 2;
    jbyte *nv21_rotated = new jbyte[buffser_size];
    // Rotate the Y luma


    int i = 0;
    int startPos = (height - 1) * width;
    for (int x = 0; x < width; x++) {
        int offset = startPos;
        for (int y = height - 1; y >= 0; y--) {
            nv21_rotated[i] = nv21_data[offset + x];
            i++;
            offset -= width;
        }
    }
    // Rotate the U and V color components
    i = buffser_size - 1;
    for (int x = width - 1; x > 0; x = x - 2) {
        int offset = y_size;
        for (int y = 0; y < height / 2; y++) {
            nv21_rotated[i] = nv21_data[offset + x];
            i--;
            nv21_rotated[i] = nv21_data[offset + (x - 1)];
            i--;
            offset += width;
        }
    }
    jbyteArray result = env->NewByteArray(buffser_size);
    env->SetByteArrayRegion(result, 0, buffser_size, nv21_rotated);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_zzj_underlying_DataConver_rotateYUV420Degree90
        (JNIEnv *env, jclass, jbyteArray arr, jint imageWidth, jint imageHeight) {

    jbyte *data = (jbyte *) env->GetByteArrayElements(arr, 0);

    int nv21Length = imageWidth * imageHeight * 3 / 2;
    jbyte *yuv = new jbyte[nv21Length];

    int i = 0;
    for (int x = 0; x < imageWidth; x++) {
        for (int y = imageHeight - 1; y >= 0; y--) {
            yuv[i] = data[y * imageWidth + x];
            i++;
        }
    }

    i = imageWidth * imageHeight * 3 / 2 - 1;
    for (int x = imageWidth - 1; x > 0; x = x - 2) {
        for (int y = 0; y < imageHeight / 2; y++) {
            yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
            i--;
            yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
            i--;
        }
    }

    jbyteArray result = env->NewByteArray(nv21Length);
    env->SetByteArrayRegion(result, 0, nv21Length, yuv);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_zzj_underlying_DataConver_rotateYUV420Degree270
        (JNIEnv *env, jclass, jbyteArray data, int imageWidth, int imageHeight) {

    jbyte *data = (jbyte *) env->GetByteArrayElements(arr, 0);

    int nv21Length = imageWidth * imageHeight * 3 / 2;
    jbyte *yuv = new jbyte[nv21Length];

    int nWidth = 0, nHeight = 0;
    int wh = 0;
    int uvHeight = 0;
    if (imageWidth != nWidth || imageHeight != nHeight) {
        nWidth = imageWidth;
        nHeight = imageHeight;
        wh = imageWidth * imageHeight;
        uvHeight = imageHeight >> 1;// uvHeight = height / 2
    }

    int k = 0;
    for (int i = 0; i < imageWidth; i++) {
        int nPos = 0;
        for (int j = 0; j < imageHeight; j++) {
            yuv[k] = data[nPos + i];
            k++;
            nPos += imageWidth;
        }
    }
    for (int i = 0; i < imageWidth; i += 2) {
        int nPos = wh;
        for (int j = 0; j < uvHeight; j++) {
            yuv[k] = data[nPos + i];
            yuv[k + 1] = data[nPos + i + 1];
            k += 2;
            nPos += imageWidth;
        }
    }
    jbyte *yuv2 = new jbyte[nv21Length];
    yuv2 = rotateYUV420Degree180(rotateYUV420Degree90(yuv, imageWidth, imageHeight), imageWidth,
                                 imageHeight);

    jbyteArray result = env->NewByteArray(nv21Length);
    env->SetByteArrayRegion(result, 0, nv21Length, yuv);
    return result;
}

unsigned char *rotateYUV420Degree90(unsigned char *data, int imageWidth, int imageHeight) {
    //byte[]
    //yuv = new byte[imageWidth * imageHeight * 3 / 2];
    unsigned char *yuv = new unsigned char[imageWidth * imageHeight * 3 / 2];
    // Rotate the Y luma
    int i = 0;
    for (int x = 0; x < imageWidth; x++) {
        for (int y = imageHeight - 1; y >= 0; y--) {
            yuv[i] = data[y * imageWidth + x];

            i++;
        }
    }
    // Rotate the U and V color components
    i = imageWidth * imageHeight * 3 / 2 - 1;
    for (int x = imageWidth - 1; x > 0; x = x - 2) {
        for (int y = 0; y < imageHeight / 2; y++) {
            yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
            i--;
            yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
            i--;
        }
    }
    return yuv;
}

unsigned char *rotateYUV420Degree180(unsigned char *data, int imageWidth, int imageHeight) {

    // unsigned char yuv = new byte[imageWidth * imageHeight * 3 / 2];
    unsigned char *yuv = new unsigned char[imageWidth * imageHeight * 3 / 2];
    int i = 0;
    int count = 0;
    for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
        yuv[count] = data[i];
        count++;
    }
    i = imageWidth * imageHeight * 3 / 2 - 1;
    for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth * imageHeight; i -= 2) {
        yuv[count++] = data[i - 1];
        yuv[count++] = data[i];
    }
    return yuv;
}

