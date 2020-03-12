package com.zzj.underlying;

public class DataConver {


    public static native String getTestString();

    public static native byte[] argbToNv21(int[] arr, int width, int height);

    public static native byte[] nv21Rotate90(byte[] arr, int width, int height);

    public static native byte[] rotateYUV420Degree90(byte[] arr, int width, int height);

    static {
        System.loadLibrary("underlying");
    }
}
