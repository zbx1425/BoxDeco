package cn.zbx1425.boxdeco.block;

import cn.zbx1425.boxdeco.BoxDeco;
import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.function.Supplier;

public class RailingBlock extends HorizontalDirectionalBlock {

    public static final EnumProperty<StairsShape> SHAPE;

    private static final VoxelShape SHAPE_CLIP_OUTER;
    private static final VoxelShape SHAPE_CLIP_STRAIGHT;
    private static final VoxelShape SHAPE_CLIP_INNER;
    private static final Map<Direction, VoxelShape> SHAPES_CLIP_OUTER;
    private static final Map<Direction, VoxelShape> SHAPES_CLIP_STRAIGHT;
    private static final Map<Direction, VoxelShape> SHAPES_CLIP_INNER;
    private static final VoxelShape SHAPE_ACT_OUTER;
    private static final VoxelShape SHAPE_ACT_STRAIGHT;
    private static final VoxelShape SHAPE_ACT_INNER;
    private static final Map<Direction, VoxelShape> SHAPES_ACT_OUTER;
    private static final Map<Direction, VoxelShape> SHAPES_ACT_STRAIGHT;
    private static final Map<Direction, VoxelShape> SHAPES_ACT_INNER;

    public RailingBlock(Properties properties) {
        super(properties);
    }

    protected boolean useShapeForLightOcclusion(@NonNull BlockState state) {
        return true;
    }

    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level,
                                           @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return getInteractionShape(state, level, pos);
    }

    @Override
    protected @NonNull VoxelShape getCollisionShape(BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        Map<Direction, VoxelShape> shapes = switch (state.getValue(SHAPE)) {
            case STRAIGHT -> SHAPES_CLIP_STRAIGHT;
            case OUTER_LEFT, OUTER_RIGHT -> SHAPES_CLIP_OUTER;
            case INNER_RIGHT, INNER_LEFT -> SHAPES_CLIP_INNER;
        };
        Direction refDirection = switch (state.getValue(SHAPE)) {
            case STRAIGHT, OUTER_LEFT, INNER_RIGHT -> facing;
            case OUTER_RIGHT -> facing.getClockWise();
            case INNER_LEFT -> facing.getCounterClockWise();
        };
        return shapes.get(refDirection);
    }

    @Override
    protected @NonNull VoxelShape getInteractionShape(BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Map<Direction, VoxelShape> shapes = switch (state.getValue(SHAPE)) {
            case STRAIGHT -> SHAPES_ACT_STRAIGHT;
            case OUTER_LEFT, OUTER_RIGHT -> SHAPES_ACT_OUTER;
            case INNER_RIGHT, INNER_LEFT -> SHAPES_ACT_INNER;
        };
        Direction refDirection = switch (state.getValue(SHAPE)) {
            case STRAIGHT, OUTER_LEFT, INNER_RIGHT -> facing;
            case OUTER_RIGHT -> facing.getClockWise();
            case INNER_LEFT -> facing.getCounterClockWise();
        };
        return shapes.get(refDirection);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        return state.setValue(SHAPE, getStairsShape(state, context.getLevel(), pos));
    }

    protected @NonNull BlockState updateShape(@NonNull BlockState state, @NonNull LevelReader level, @NonNull ScheduledTickAccess ticks,
                                              @NonNull BlockPos pos, Direction directionToNeighbour, @NonNull BlockPos neighbourPos,
                                              @NonNull BlockState neighbourState, @NonNull RandomSource random) {
        return directionToNeighbour.getAxis().isHorizontal()
            ? state.setValue(SHAPE, getStairsShape(state, level, pos))
            : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    private static StairsShape getStairsShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState behindState = level.getBlockState(pos.relative(facing));
        if (isRailing(behindState)) {
            Direction behindFacing = behindState.getValue(FACING);
            if (behindFacing.getAxis() != state.getValue(FACING).getAxis() && canTakeShape(state, level, pos, behindFacing.getOpposite())) {
                if (behindFacing == facing.getCounterClockWise()) {
                    return StairsShape.OUTER_LEFT;
                }
                return StairsShape.OUTER_RIGHT;
            }
        }
        BlockState frontState = level.getBlockState(pos.relative(facing.getOpposite()));
        if (isRailing(frontState)) {
            Direction frontFacing = frontState.getValue(FACING);
            if (frontFacing.getAxis() != state.getValue(FACING).getAxis() && canTakeShape(state, level, pos, frontFacing)) {
                if (frontFacing == facing.getCounterClockWise()) {
                    return StairsShape.INNER_LEFT;
                }
                return StairsShape.INNER_RIGHT;
            }
        }
        return StairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction neighbour) {
        BlockState neighborState = level.getBlockState(pos.relative(neighbour));
        return !isRailing(neighborState) || neighborState.getValue(FACING) != state.getValue(FACING);
    }

    public static boolean isRailing(BlockState state) {
        return state.getBlock() instanceof RailingBlock || state.getBlock() instanceof SlopedRailingBlock;
    }

    protected @NonNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    protected @NonNull BlockState mirror(BlockState state, Mirror mirror) {
        Direction direction = state.getValue(FACING);
        StairsShape shape = state.getValue(SHAPE);
        switch (mirror) {
            case LEFT_RIGHT:
                if (direction.getAxis() == Direction.Axis.Z) {
                    switch (shape) {
                        case OUTER_LEFT -> {
                            return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                        }
                        case OUTER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                        }
                        case INNER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                        }
                        case INNER_LEFT -> {
                            return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                        }
                        default -> {
                            return state.rotate(Rotation.CLOCKWISE_180);
                        }
                    }
                }
                break;
            case FRONT_BACK:
                if (direction.getAxis() == Direction.Axis.X) {
                    switch (shape) {
                        case STRAIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180);
                        }
                        case OUTER_LEFT -> {
                            return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                        }
                        case OUTER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                        }
                        case INNER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                        }
                        case INNER_LEFT -> {
                            return state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                        }
                    }
                }
        }

        return super.mirror(state, mirror);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    protected boolean isPathfindable(@NonNull BlockState state, @NonNull PathComputationType type) {
        return false;
    }

    public static final Supplier<MapCodec<RailingBlock>> CODEC = BoxDeco.BLOCK_TYPES.register(
        "railing",
        () -> BlockBehaviour.simpleCodec(RailingBlock::new)
    );

    @Override
    protected @NonNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC.get();
    }

    static {
        SHAPE = BlockStateProperties.STAIRS_SHAPE;
        SHAPE_CLIP_OUTER = Block.box(0.0F, 0.0F, 0.01F, 3.0F, 24.0F, 3.0F);
        SHAPE_CLIP_STRAIGHT = Block.box(0.0F, 0.0F, 0.01F, 16.0F, 24.0F, 3.0F);
        SHAPE_CLIP_INNER = Shapes.or(SHAPE_CLIP_STRAIGHT, Shapes.rotate(SHAPE_CLIP_STRAIGHT, OctahedralGroup.BLOCK_ROT_Y_90));
        SHAPES_CLIP_OUTER = Shapes.rotateHorizontal(SHAPE_CLIP_OUTER);
        SHAPES_CLIP_STRAIGHT = Shapes.rotateHorizontal(SHAPE_CLIP_STRAIGHT);
        SHAPES_CLIP_INNER = Shapes.rotateHorizontal(SHAPE_CLIP_INNER);
        SHAPE_ACT_OUTER = Block.box(0.0F, 0.0F, 0.01F, 8.0F, 16.0F, 8.0F);
        SHAPE_ACT_STRAIGHT = Block.box(0.0F, 0.0F, 0.01F, 16.0F, 16.0F, 8.0F);
        SHAPE_ACT_INNER = Shapes.or(SHAPE_ACT_STRAIGHT, Shapes.rotate(SHAPE_ACT_STRAIGHT, OctahedralGroup.BLOCK_ROT_Y_90));
        SHAPES_ACT_OUTER = Shapes.rotateHorizontal(SHAPE_ACT_OUTER);
        SHAPES_ACT_STRAIGHT = Shapes.rotateHorizontal(SHAPE_ACT_STRAIGHT);
        SHAPES_ACT_INNER = Shapes.rotateHorizontal(SHAPE_ACT_INNER);
    }
}
