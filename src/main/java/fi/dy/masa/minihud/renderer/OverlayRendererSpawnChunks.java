package fi.dy.masa.minihud.renderer;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.OverworldDimension;

public class OverlayRendererSpawnChunks extends OverlayRendererBase
{
    protected static boolean needsUpdate = true;

    protected final RendererToggle toggle;

    public static void setNeedsUpdate()
    {
        needsUpdate = true;
    }

    public OverlayRendererSpawnChunks(RendererToggle toggle)
    {
        this.toggle = toggle;
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return this.toggle.getBooleanValue() &&
                (this.toggle == RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_PLAYER ||
                 (mc.world != null && mc.world.dimension instanceof OverworldDimension &&
                  DataStorage.getInstance().isWorldSpawnKnown()));
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        if (needsUpdate)
        {
            return true;
        }

        int ex = (int) Math.floor(entity.x);
        int ez = (int) Math.floor(entity.z);
        int lx = this.lastUpdatePos.getX();
        int lz = this.lastUpdatePos.getZ();

        // Player-following renderer
        if (this.toggle == RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_PLAYER)
        {
            return ex != lx || ez != lz;
        }

        return Math.abs(lx - ex) > 16 || Math.abs(lz - ez) > 16;
    }

    @Override
    public void update(Entity entity, MinecraftClient mc)
    {
        DataStorage data = DataStorage.getInstance();
        BlockPos spawn = this.toggle == RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_PLAYER ? new BlockPos(entity) : data.getWorldSpawn();

        RenderObjectBase renderQuads = this.renderObjects.get(0);
        RenderObjectBase renderLines = this.renderObjects.get(1);
        BUFFER_1.begin(renderQuads.getGlMode(), VertexFormats.POSITION_COLOR);
        BUFFER_2.begin(renderLines.getGlMode(), VertexFormats.POSITION_COLOR);

        final int colorEntity = this.toggle == RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL ?
                Configs.Colors.SPAWN_REAL_ENTITY_OVERLAY_COLOR.getIntegerValue() :
                Configs.Colors.SPAWN_PLAYER_ENTITY_OVERLAY_COLOR.getIntegerValue();
        final int colorLazy = this.toggle == RendererToggle.OVERLAY_SPAWN_CHUNK_OVERLAY_REAL ?
                Configs.Colors.SPAWN_REAL_LAZY_OVERLAY_COLOR.getIntegerValue() :
                Configs.Colors.SPAWN_PLAYER_LAZY_OVERLAY_COLOR.getIntegerValue();

        int rangeH = (mc.options.viewDistance + 1) * 16;
        Pair<BlockPos, BlockPos> corners = this.getSpawnChunkCorners(spawn, 11);
        RenderUtils.renderVerticalWallsOfLinesWithinRange(BUFFER_1, BUFFER_2, corners.getLeft(), corners.getRight(),
                rangeH, 256, 16, 16, entity, colorLazy);

        corners = this.getSpawnChunkCorners(spawn, 10);
        RenderUtils.renderVerticalWallsOfLinesWithinRange(BUFFER_1, BUFFER_2, corners.getLeft(), corners.getRight(),
                rangeH, 256, 16, 16, entity, colorEntity);

        BUFFER_1.end();
        BUFFER_2.end();

        renderQuads.uploadData(BUFFER_1);
        renderLines.uploadData(BUFFER_2);

        needsUpdate = false;
    }

    @Override
    public void allocateGlResources()
    {
        this.allocateBuffer(GL11.GL_QUADS);
        this.allocateBuffer(GL11.GL_LINES);
    }

    protected Pair<BlockPos, BlockPos> getSpawnChunkCorners(BlockPos worldSpawn, int chunkRange)
    {
        int cx = (worldSpawn.getX() >> 4);
        int cz = (worldSpawn.getZ() >> 4);

        BlockPos pos1 = new BlockPos( (cx - chunkRange) << 4      ,   0,  (cz - chunkRange) << 4);
        BlockPos pos2 = new BlockPos(((cx + chunkRange) << 4) + 15, 256, ((cz + chunkRange) << 4) + 15);

        return Pair.of(pos1, pos2);
    }
}
