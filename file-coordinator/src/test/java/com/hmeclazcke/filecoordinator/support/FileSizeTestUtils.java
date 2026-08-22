package com.hmeclazcke.filecoordinator.support;

public final class FileSizeTestUtils {

    private FileSizeTestUtils() {
    }

    public static long megabytes(long amount) {
        return amount * 1024 * 1024;
    }

    public static long gigabytes(long amount) {
        return amount * 1024 * 1024 * 1024;
    }
}