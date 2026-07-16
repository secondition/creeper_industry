package com.secondition.creeperindustry.content.automation.breaker;

public record SignalRange(int width, int height, int depth) {
    public static final int SMALL_SIZE = 1;
    public static final int LARGE_SIZE = 3;
    public static final SignalRange DEFAULT = new SignalRange(SMALL_SIZE, SMALL_SIZE, SMALL_SIZE);

    public SignalRange {
        width = normalizeSize(width);
        height = normalizeSize(height);
        depth = normalizeSize(depth);
    }

    public int volume() {
        return width * height * depth;
    }

    public SignalRange toggleWidth() {
        return new SignalRange(toggle(width), height, depth);
    }

    public SignalRange toggleHeight() {
        return new SignalRange(width, toggle(height), depth);
    }

    public SignalRange toggleDepth() {
        return new SignalRange(width, height, toggle(depth));
    }

    public static int normalizeSize(int size) {
        return size == LARGE_SIZE ? LARGE_SIZE : SMALL_SIZE;
    }

    private static int toggle(int size) {
        return size == SMALL_SIZE ? LARGE_SIZE : SMALL_SIZE;
    }
}
