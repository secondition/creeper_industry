package com.secondition.creeperindustry.content.production.biosphere;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BiosphereApproximateCollisionShapes {
    private static final double MODEL_MIN = 0.0D;
    private static final double MODEL_MAX = 16.0D;
    private static final double EPSILON = 1.0E-6D;
    private static final double MIN_THICKNESS = 2.0D;
    private static final double MIN_VOLUME = 16.0D;

    private static final Map<BiosphereType, List<Box>> STRUCTURE_BOX_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, VoxelShape> SHAPE_CACHE = new ConcurrentHashMap<>();

    private BiosphereApproximateCollisionShapes() {
    }

    public static VoxelShape getShape(BiosphereType type, int xPart, int yPart, int zPart, Direction facing) {
        if (type != BiosphereType.BOTANICAL || facing.getAxis().isVertical()) {
            return Shapes.block();
        }

        String cacheKey = type.name() + "#" + xPart + "#" + yPart + "#" + zPart + "#" + facing.getSerializedName();
        return SHAPE_CACHE.computeIfAbsent(cacheKey, ignored -> buildPartShape(type, xPart, yPart, zPart, facing));
    }

    public static VoxelShape getStructureShape(BiosphereType type, Direction facing) {
        if (type != BiosphereType.BOTANICAL || facing.getAxis().isVertical()) {
            return Shapes.empty();
        }

        String cacheKey = type.name() + "#structure#" + facing.getSerializedName();
        return SHAPE_CACHE.computeIfAbsent(cacheKey, ignored -> buildStructureShape(type, facing));
    }

    private static VoxelShape buildStructureShape(BiosphereType type, Direction facing) {
        VoxelShape shape = Shapes.empty();
        for (int xPart = 0; xPart < BiosphereBlock.WIDTH_X; xPart++) {
            for (int yPart = 0; yPart < BiosphereBlock.HEIGHT_Y; yPart++) {
                for (int zPart = 0; zPart < BiosphereBlock.DEPTH_Z; zPart++) {
                    VoxelShape partShape = getShape(type, xPart, yPart, zPart, facing);
                    if (partShape.isEmpty()) {
                        continue;
                    }

                    BlockOffset offset = getBlockOffset(facing, xPart, yPart, zPart);
                    shape = Shapes.or(shape, partShape.move(offset.x(), offset.y(), offset.z()));
                }
            }
        }
        return shape.optimize();
    }

    private static VoxelShape buildPartShape(BiosphereType type, int xPart, int yPart, int zPart, Direction facing) {
        double sliceMinX = xPart * 16.0D;
        double sliceMinY = yPart * 16.0D;
        double sliceMinZ = zPart * 16.0D;
        double sliceMaxX = sliceMinX + 16.0D;
        double sliceMaxY = sliceMinY + 16.0D;
        double sliceMaxZ = sliceMinZ + 16.0D;

        List<Box> localBoxes = new ArrayList<>();
        for (Box structureBox : loadNorthStructureBoxes(type)) {
            Box sliced = structureBox.intersect(sliceMinX, sliceMinY, sliceMinZ, sliceMaxX, sliceMaxY, sliceMaxZ);
            if (sliced == null) {
                continue;
            }

            localBoxes.add(rotateLocalBox(sliced.move(-sliceMinX, -sliceMinY, -sliceMinZ), facing));
        }

        return buildShape(localBoxes);
    }

    private static List<Box> loadNorthStructureBoxes(BiosphereType type) {
        return STRUCTURE_BOX_CACHE.computeIfAbsent(type, BiosphereApproximateCollisionShapes::loadNorthStructureBoxesInternal);
    }

    private static List<Box> loadNorthStructureBoxesInternal(BiosphereType type) {
        if (type != BiosphereType.BOTANICAL) {
            return List.of(new Box(0.0D, 0.0D, 0.0D, 48.0D, 112.0D, 32.0D));
        }

        try {
            List<Box> boxes = new ArrayList<>();
            for (int xPart = 0; xPart < BiosphereBlock.WIDTH_X; xPart++) {
                for (int yPart = 0; yPart < BiosphereBlock.HEIGHT_Y; yPart++) {
                    for (int zPart = 0; zPart < BiosphereBlock.DEPTH_Z; zPart++) {
                        String partName = "part_x" + xPart + "_y" + yPart + "_z" + zPart;
                        String resourcePath = "/assets/creeper_industry/models/block/botanical_biosphere/" + partName + ".json";
                        List<JsonObject> elements = loadEffectiveElements(resourcePath, new HashSet<>());
                        for (JsonObject element : elements) {
                            Box box = elementToBox(element)
                                    .move(xPart * 16.0D, yPart * 16.0D, zPart * 16.0D);
                            if (box.isMeaningful()) {
                                boxes.add(box);
                            }
                        }
                    }
                }
            }
            return List.copyOf(boxes);
        } catch (IOException | RuntimeException exception) {
            return List.of(new Box(0.0D, 0.0D, 0.0D, 48.0D, 112.0D, 32.0D));
        }
    }

    private static List<JsonObject> loadEffectiveElements(String resourcePath, Set<String> visiting) throws IOException {
        if (!visiting.add(resourcePath)) {
            throw new IllegalStateException("Detected model parent cycle at " + resourcePath);
        }

        try (InputStream stream = BiosphereApproximateCollisionShapes.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Missing model resource: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray elements = json.getAsJsonArray("elements");
                if (elements != null) {
                    List<JsonObject> resolved = new ArrayList<>(elements.size());
                    for (JsonElement element : elements) {
                        resolved.add(element.getAsJsonObject());
                    }
                    return resolved;
                }

                String parent = json.has("parent") ? json.get("parent").getAsString() : null;
                if (parent == null || parent.isBlank()) {
                    return List.of();
                }
                return loadEffectiveElements(resolveParentPath(parent), visiting);
            }
        } finally {
            visiting.remove(resourcePath);
        }
    }

    private static String resolveParentPath(String parent) {
        String namespace = "minecraft";
        String path = parent;
        int separator = parent.indexOf(':');
        if (separator >= 0) {
            namespace = parent.substring(0, separator);
            path = parent.substring(separator + 1);
        }
        return "/assets/" + namespace + "/models/" + path + ".json";
    }

    private static Box elementToBox(JsonObject element) {
        double[] from = readVec3(element.getAsJsonArray("from"));
        double[] to = readVec3(element.getAsJsonArray("to"));
        Box box = new Box(
                Math.min(from[0], to[0]),
                Math.min(from[1], to[1]),
                Math.min(from[2], to[2]),
                Math.max(from[0], to[0]),
                Math.max(from[1], to[1]),
                Math.max(from[2], to[2])
        );

        JsonObject rotation = element.getAsJsonObject("rotation");
        if (rotation != null) {
            box = rotateBox(
                    box,
                    readVec3(rotation.getAsJsonArray("origin")),
                    rotation.get("axis").getAsString(),
                    Math.toRadians(rotation.get("angle").getAsDouble())
            );
        }

        return box;
    }

    private static double[] readVec3(JsonArray values) {
        if (values == null || values.size() != 3) {
            throw new IllegalArgumentException("Expected a vec3 array.");
        }
        return new double[] {
                values.get(0).getAsDouble(),
                values.get(1).getAsDouble(),
                values.get(2).getAsDouble()
        };
    }

    private static Box rotateBox(Box box, double[] origin, String axis, double radians) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (double x : new double[] {box.minX, box.maxX}) {
            for (double y : new double[] {box.minY, box.maxY}) {
                for (double z : new double[] {box.minZ, box.maxZ}) {
                    double[] rotated = rotatePoint(x, y, z, origin, axis, radians);
                    minX = Math.min(minX, rotated[0]);
                    minY = Math.min(minY, rotated[1]);
                    minZ = Math.min(minZ, rotated[2]);
                    maxX = Math.max(maxX, rotated[0]);
                    maxY = Math.max(maxY, rotated[1]);
                    maxZ = Math.max(maxZ, rotated[2]);
                }
            }
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static double[] rotatePoint(double x, double y, double z, double[] origin, String axis, double radians) {
        double translatedX = x - origin[0];
        double translatedY = y - origin[1];
        double translatedZ = z - origin[2];
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double rotatedX = translatedX;
        double rotatedY = translatedY;
        double rotatedZ = translatedZ;

        switch (axis) {
            case "x" -> {
                rotatedY = translatedY * cos - translatedZ * sin;
                rotatedZ = translatedY * sin + translatedZ * cos;
            }
            case "y" -> {
                rotatedX = translatedX * cos + translatedZ * sin;
                rotatedZ = -translatedX * sin + translatedZ * cos;
            }
            case "z" -> {
                rotatedX = translatedX * cos - translatedY * sin;
                rotatedY = translatedX * sin + translatedY * cos;
            }
            default -> throw new IllegalArgumentException("Unsupported rotation axis: " + axis);
        }

        return new double[] {
                rotatedX + origin[0],
                rotatedY + origin[1],
                rotatedZ + origin[2]
        };
    }

    private static Box rotateLocalBox(Box box, Direction facing) {
        return switch (facing) {
            case EAST -> new Box(16.0D - box.maxZ, box.minY, box.minX, 16.0D - box.minZ, box.maxY, box.maxX);
            case SOUTH -> new Box(16.0D - box.maxX, box.minY, 16.0D - box.maxZ, 16.0D - box.minX, box.maxY, 16.0D - box.minZ);
            case WEST -> new Box(box.minZ, box.minY, 16.0D - box.maxX, box.maxZ, box.maxY, 16.0D - box.minX);
            default -> box;
        };
    }

    private static BlockOffset getBlockOffset(Direction facing, int xPart, int yPart, int zPart) {
        return switch (facing) {
            case NORTH -> new BlockOffset(xPart, yPart, zPart);
            case EAST -> new BlockOffset(-zPart, yPart, xPart);
            case SOUTH -> new BlockOffset(-xPart, yPart, -zPart);
            case WEST -> new BlockOffset(zPart, yPart, -xPart);
            default -> new BlockOffset(xPart, yPart, zPart);
        };
    }

    private static VoxelShape buildShape(List<Box> boxes) {
        if (boxes.isEmpty()) {
            return Shapes.empty();
        }

        VoxelShape shape = Shapes.empty();
        for (Box box : boxes) {
            shape = Shapes.or(shape, Block.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
        }
        return shape.optimize();
    }

    private record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private Box clipToBlock() {
            return new Box(
                    clamp(minX),
                    clamp(minY),
                    clamp(minZ),
                    clamp(maxX),
                    clamp(maxY),
                    clamp(maxZ)
            );
        }

        private boolean isMeaningful() {
            double width = maxX - minX;
            double height = maxY - minY;
            double depth = maxZ - minZ;
            return width > EPSILON
                    && height > EPSILON
                    && depth > EPSILON
                    && Math.min(width, Math.min(height, depth)) + EPSILON >= MIN_THICKNESS
                    && width * height * depth + EPSILON >= MIN_VOLUME;
        }

        private Box move(double xOffset, double yOffset, double zOffset) {
            return new Box(
                    minX + xOffset,
                    minY + yOffset,
                    minZ + zOffset,
                    maxX + xOffset,
                    maxY + yOffset,
                    maxZ + zOffset
            );
        }

        private Box intersect(double otherMinX, double otherMinY, double otherMinZ, double otherMaxX, double otherMaxY, double otherMaxZ) {
            double clippedMinX = Math.max(minX, otherMinX);
            double clippedMinY = Math.max(minY, otherMinY);
            double clippedMinZ = Math.max(minZ, otherMinZ);
            double clippedMaxX = Math.min(maxX, otherMaxX);
            double clippedMaxY = Math.min(maxY, otherMaxY);
            double clippedMaxZ = Math.min(maxZ, otherMaxZ);
            if (clippedMaxX - clippedMinX <= EPSILON || clippedMaxY - clippedMinY <= EPSILON || clippedMaxZ - clippedMinZ <= EPSILON) {
                return null;
            }
            return new Box(clippedMinX, clippedMinY, clippedMinZ, clippedMaxX, clippedMaxY, clippedMaxZ);
        }

        private static double clamp(double value) {
            return Math.max(MODEL_MIN, Math.min(MODEL_MAX, value));
        }
    }

    private record BlockOffset(double x, double y, double z) {
    }
}
