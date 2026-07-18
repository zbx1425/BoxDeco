package cn.zbx1425.boxdeco.block;

import cn.zbx1425.boxdeco.BoxDeco;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.function.Supplier;

public class ThinVerticalBarrierBlock extends HorizontalDirectionalBlock {

    private static final VoxelShape SHAPE;
    private static final Map<Direction, VoxelShape> SHAPES;
    private static final Map<Direction, VoxelShape> SHAPES_ACT;

    public ThinVerticalBarrierBlock(Properties properties) {
        super(properties);
    }

    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level,
                                           @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return getInteractionShape(state, level, pos);
    }

    @Override
    protected @NonNull VoxelShape getInteractionShape(BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos) {
        return SHAPES_ACT.get(state.getValue(FACING));
    }

    @Override
    protected @NonNull VoxelShape getCollisionShape(BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    protected boolean isPathfindable(@NonNull BlockState state, @NonNull PathComputationType type) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(@NonNull BlockState state) {
        return true;
    }

    protected @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    protected float getShadeBrightness(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos) {
        return 1.0F;
    }

    public static final Supplier<MapCodec<ThinVerticalBarrierBlock>> CODEC = BoxDeco.BLOCK_TYPES.register(
        "thin_vertical_barrier",
        () -> BlockBehaviour.simpleCodec(ThinVerticalBarrierBlock::new)
    );

    @Override
    protected @NonNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC.get();
    }

    static {
        SHAPE = Block.box(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 3.0F);
        SHAPES = Shapes.rotateHorizontal(SHAPE);
        VoxelShape actShape = Block.box(0, 0, 0.01, 16, 16, 8);
        SHAPES_ACT = Shapes.rotateHorizontal(actShape);
    }
}
