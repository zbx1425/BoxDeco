package cn.zbx1425.boxdeco.block;

import cn.zbx1425.boxdeco.BoxDeco;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class SlopedRailingBlock extends HorizontalDirectionalBlock {

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<Tilt> TILT = EnumProperty.create("tilt", Tilt.class);

    @SuppressWarnings("unchecked")
    private static final Map<Direction, VoxelShape>[][] SHAPES = new Map[3][2];

    public SlopedRailingBlock(Properties properties) {
        super(properties);
    }

    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level,
                                           @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPES[state.getValue(PART).ordinal()][state.getValue(TILT).ordinal()]
                .get(state.getValue(FACING));
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        Pair<Part, Tilt> railingShape = getSlopedRailingShape(state, context.getLevel(), pos);
        return state.setValue(PART, railingShape.getFirst()).setValue(TILT, railingShape.getSecond());
    }

    protected @NonNull BlockState updateShape(@NonNull BlockState state, @NonNull LevelReader level, @NonNull ScheduledTickAccess ticks,
                                              @NonNull BlockPos pos, @NonNull Direction directionToNeighbour, @NonNull BlockPos neighbourPos,
                                              @NonNull BlockState neighbourState, @NonNull RandomSource random) {
        if (directionToNeighbour == Direction.UP) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        Direction railingDirection = state.getValue(FACING);
        if (directionToNeighbour == railingDirection || directionToNeighbour == railingDirection.getOpposite()) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        Pair<Part, Tilt> railingShape = getSlopedRailingShape(state, level, pos);
        return state.setValue(PART, railingShape.getFirst()).setValue(TILT, railingShape.getSecond());
    }

    private static Pair<Part, Tilt> getSlopedRailingShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction railingDirection = state.getValue(FACING);
        Optional<Tilt> leftStairState = tryGetStairTilt(railingDirection, level, pos.relative(railingDirection.getCounterClockWise()));
        Optional<Tilt> rightStairState = tryGetStairTilt(railingDirection, level, pos.relative(railingDirection.getClockWise()));
        Optional<Tilt> belowStairState = tryGetStairTilt(railingDirection, level, pos.below());

        // Lower end
        if (leftStairState.isPresent() && rightStairState.isEmpty() && belowStairState.isEmpty()) {
            return Pair.of(Part.LOWER, leftStairState.get());
        }
        if (rightStairState.isPresent() && leftStairState.isEmpty() && belowStairState.isEmpty()) {
            return Pair.of(Part.LOWER, rightStairState.get());
        }

        // Upper end
        if (belowStairState.isPresent() && leftStairState.isEmpty() && rightStairState.isEmpty()) {
            return Pair.of(Part.UPPER, belowStairState.get());
        }

        // Middle
        if (leftStairState.isPresent() && belowStairState.isPresent() && rightStairState.isEmpty()) {
            return Pair.of(Part.MIDDLE, belowStairState.get());
        }
        if (rightStairState.isPresent() && belowStairState.isPresent() && leftStairState.isEmpty()) {
            return Pair.of(Part.MIDDLE, belowStairState.get());
        }

        // Fallback
        return Pair.of(Part.MIDDLE, belowStairState.orElse(Tilt.LEFT_UP));
    }

    private static Optional<Tilt> tryGetStairTilt(Direction refFacing, BlockGetter level, BlockPos pos) {
        BlockState stairState = level.getBlockState(pos);
        if (!(stairState.getBlock() instanceof StairBlock)) return Optional.empty();
        Direction stairDirection = stairState.getValueOrElse(FACING, refFacing);
        if (stairDirection == refFacing || stairDirection.getOpposite() == refFacing) return Optional.empty();
        if (refFacing.getCounterClockWise() == stairDirection) {
            return Optional.of(Tilt.LEFT_UP);
        } else {
            return Optional.of(Tilt.RIGHT_UP);
        }
    }

    @Override
    protected @NonNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NonNull BlockState mirror(@NonNull BlockState state, @NonNull Mirror mirror) {
        return state
            .setValue(FACING, mirror.mirror(state.getValue(FACING)))
            .setValue(TILT, state.getValue(TILT).invert());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, TILT);
    }

    protected boolean isPathfindable(@NonNull BlockState state, @NonNull PathComputationType type) {
        return false;
    }

    public static final Supplier<MapCodec<SlopedRailingBlock>> CODEC = BoxDeco.BLOCK_TYPES.register(
        "sloped_railing",
        () -> BlockBehaviour.simpleCodec(SlopedRailingBlock::new)
    );

    @Override
    protected @NonNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC.get();
    }

    public enum Part implements StringRepresentable {
        LOWER("lower"),
        MIDDLE("middle"),
        UPPER("upper");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public enum Tilt implements StringRepresentable {
        LEFT_UP("left_up"),
        RIGHT_UP("right_up");

        private final String name;

        Tilt(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public @NonNull String getSerializedName() {
            return this.name;
        }

        public Tilt invert() {
            return this == LEFT_UP ? RIGHT_UP : LEFT_UP;
        }
    }

    private static VoxelShape mirrorSlope(VoxelShape shape) {
        VoxelShape[] result = { Shapes.empty() };
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
                result[0] = Shapes.or(result[0], Shapes.box(1 - x2, y1, z1, 1 - x1, y2, z2))
        );
        return result[0];
    }

    static {
        // Base shapes for rotateHorizontal: railing at z=0-3 (north face), slope along X.
        // RIGHT_UP: high at +X (east), low at -X (west).
        VoxelShape lowerRU = Shapes.or(
                Block.box(12, 0, 0, 16, 24, 3),
                Block.box(8, 0, 0, 12, 20, 3),
                Block.box(0, 0, 0, 8, 17.5, 3)
        );
        VoxelShape middleRU = Shapes.or(
                Block.box(12, 8, 0, 16, 26, 3),
                Block.box(8, 4, 0, 12, 22, 3),
                Block.box(4, 0, 0, 8, 18, 3),
                Block.box(0, -4, 0, 4, 14, 3)
        );
        VoxelShape upperRU = Shapes.or(
                Block.box(6, 0, 0, 16, 17.5, 3),
                Block.box(3, -2, 0, 6, 15, 3),
                Block.box(0, -4, 0, 3, 12, 3)
        );

        VoxelShape lowerLU = mirrorSlope(lowerRU);
        VoxelShape middleLU = mirrorSlope(middleRU);
        VoxelShape upperLU = mirrorSlope(upperRU);

        SHAPES[Part.LOWER.ordinal()][Tilt.RIGHT_UP.ordinal()] = Shapes.rotateHorizontal(lowerRU);
        SHAPES[Part.MIDDLE.ordinal()][Tilt.RIGHT_UP.ordinal()] = Shapes.rotateHorizontal(middleRU);
        SHAPES[Part.UPPER.ordinal()][Tilt.RIGHT_UP.ordinal()] = Shapes.rotateHorizontal(upperRU);
        SHAPES[Part.LOWER.ordinal()][Tilt.LEFT_UP.ordinal()] = Shapes.rotateHorizontal(lowerLU);
        SHAPES[Part.MIDDLE.ordinal()][Tilt.LEFT_UP.ordinal()] = Shapes.rotateHorizontal(middleLU);
        SHAPES[Part.UPPER.ordinal()][Tilt.LEFT_UP.ordinal()] = Shapes.rotateHorizontal(upperLU);
    }
}
