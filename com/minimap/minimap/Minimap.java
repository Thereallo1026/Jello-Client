package com.minimap.minimap;

import net.minecraft.client.*;
import java.awt.*;
import java.util.*;
import net.minecraft.client.shader.*;

import com.mentalfrostbyte.jello.main.Jello;
import com.minimap.*;
import net.minecraft.block.state.*;
import net.minecraft.init.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.*;
import java.io.*;
import java.awt.image.*;
import net.minecraft.world.chunk.*;
import optifine.Reflector;
import net.minecraft.util.*;
import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.world.*;
import com.minimap.settings.*;
import com.minimap.animation.*;
import java.nio.*;
import net.minecraft.client.settings.*;
import com.minimap.interfaces.*;
import org.lwjgl.opengl.*;
import org.lwjgl.input.*;
import net.minecraft.client.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.*;
import net.minecraft.scoreboard.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.*;
import net.minecraft.entity.item.*;

public class Minimap
{
    public Interface screen;
    public static final int frame = 9;
    public static int loadingSide;
    public static int loadedSide;
    public static int updateRadius;
    public static boolean enlargedMap;
    public static Minecraft mc;
    private int loadedCaving;
    private int loadingCaving;
    public static final Color radarPlayers;
    public static final Color radarShadow;
    public static ArrayList<Entity> loadedPlayers;
    public static ArrayList<Entity> loadedLiving;
    public static ArrayList<Entity> loadedHostile;
    public static ArrayList<Entity> loadedItems;
    public static ArrayList<Entity> loadedEntities;
    public static int blocksLoaded;
    public int loadingMapX;
    public int loadingMapZ;
    public int loadedMapX;
    public int loadedMapZ;
    public double minimapZoom;
    public int minimapWidth;
    public int minimapHeight;
    public MapLoader loader;
    private static String worldID;
    public static String customWorldID;
    public MinimapChunk[][] currentBlocks;
    public MinimapChunk[][] loadingBlocks;
    public static boolean frameIsUpdating;
    public static boolean frameUpdateNeeded;
    public static int bufferSizeToUpdate;
    public static float frameUpdatePartialTicks;
    public static boolean updatePause;
    Integer previousTransparentBlock;
    int underRed;
    int underGreen;
    int underBlue;
    float divider;
    int sun;
    public float postBrightness;
    int blockY;
    int blockColor;
    public static HashMap<String, Integer> textureColours;
    public static HashMap<Integer, Integer> blockColours;
    public static boolean clearBlockColours;
    boolean isglowing;
    public int[] lastBlockY;
    public static float sunBrightness;
    private byte[] bytes;
    private byte drawYState;
    private static boolean toResetImage;
    public static double zoom;
    public static final int[] minimapSizes;
    public static final int[] bufferSizes;
    public static final int[] FBOMinimapSizes;
    public static final int[] FBOBufferSizes;
    public static boolean triedFBO;
    public static boolean loadedFBO;
    public static DynamicTexture mapTexture;
    public static Framebuffer scalingFrameBuffer;
    public static Framebuffer rotationFrameBuffer;
    public static int mapUpdateX;
    public static int mapUpdateZ;
    
    public static int getLoadSide() {
        return Minimap.enlargedMap ? 31 : Minimap.FBOMinimapSizes[XaeroMinimap.getSettings().getMinimapSize()];
    }
    
    public static int getUpdateRadius() {
        return (int)Math.ceil(Minimap.loadingSide / 2.0 / XaeroMinimap.getSettings().zooms[XaeroMinimap.getSettings().zoom]);
    }
    
    /*private static String getWorld() {
        if (Minimap.mc.getIntegratedServer() != null) {
            return Minimap.mc.getIntegratedServer().getFolderName() + "_" + Minimap.mc.theWorld.provider.getSaveFolder();
        }
        if (Minimap.mc.getCurrentServerData() != null) {
            String serverIP = Minimap.mc.getCurrentServerData().serverIP;
            if (serverIP.contains(":")) {
                serverIP = serverIP.substring(0, serverIP.indexOf(":"));
            }
            return "Multiplayer_" + serverIP.replaceAll(":", "�") + "_" + Minimap.mc.theWorld.provider.getSaveFolder();
        }
        return "Unknown_null";
    }*/
    
    public static String getCurrentWorldID() {
        if (Minimap.customWorldID == null) {
            return Minimap.worldID;
        }
        return Minimap.customWorldID;
    }
    
    
    public static String getAutoWorldID() {
        return Minimap.worldID;
    }
    
    
    public static void updateWaypoints() {
       // addWorld(Minimap.worldID = getWorld());
      //  Minimap.waypoints = getWaypoints();
    }
    
    public Minimap(final Interface i) {
        this.loadedCaving = -1;
        this.loadingCaving = -1;
        this.loadingMapX = 0;
        this.loadingMapZ = 0;
        this.loadedMapX = 0;
        this.loadedMapZ = 0;
        this.minimapZoom = 1.0;
        this.minimapWidth = 0;
        this.minimapHeight = 0;
        this.loader = new MapLoader();
        this.currentBlocks = new MinimapChunk[16][16];
        this.loadingBlocks = new MinimapChunk[16][16];
        this.previousTransparentBlock = null;
        this.divider = 1.0f;
        this.blockY = 0;
        this.blockColor = 0;
        this.isglowing = false;
        this.lastBlockY = new int[16];
        this.drawYState = 0;
        this.screen = i;
        new Thread(this.loader).start();
    }
    
    public static int loadBlockColourFromTexture(final IBlockState state, final Block b, final BlockPos pos, final boolean convert) {
        final int stateId = state.toString().hashCode();
        Integer c = Minimap.blockColours.get(stateId);
        int red = 0;
        int green = 0;
        int blue = 0;
        if (c == null) {
            final TextureAtlasSprite texture = Minecraft.getMinecraft().getBlockRendererDispatcher().func_175023_a().func_178122_a(state);
            String name = null;
            try {
                name = texture.getIconName() + ".png";
                if (b instanceof BlockGrass) {
                    name = "minecraft:blocks/grass_top.png";
                }
                else if (b == Blocks.red_mushroom_block) {
                    name = "minecraft:blocks/mushroom_block_skin_red.png";
                }
                else if (b == Blocks.brown_mushroom_block) {
                    name = "minecraft:blocks/mushroom_block_skin_brown.png";
                }
                else if (b instanceof BlockOre && b != Blocks.quartz_ore) {
                    name = "minecraft:blocks/stone.png";
                }
                if (convert) {
                    name = name.replaceAll("_side", "_top").replaceAll("_front.png", "_top.png");
                }
                c = -1;
                String[] args = name.split(":");
                if (args.length < 2) {
                    args = new String[] { "minecraft", args[0] };
                }
                final Integer cachedColour = Minimap.textureColours.get(name);
                if (cachedColour == null) {
                    final ResourceLocation location = new ResourceLocation(args[0], "textures/" + args[1]);
                    final IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
                    final InputStream input = resource.getInputStream();
                    final BufferedImage img = TextureUtil.func_177053_a(input);
                    red = 0;
                    green = 0;
                    blue = 0;
                    int total = 64;
                    final int tw = img.getWidth();
                    final int diff = tw / 8;
                    for (int i = 0; i < 8; ++i) {
                        for (int j = 0; j < 8; ++j) {
                            final int rgb = img.getRGB(i * diff, j * diff);
                            if (rgb == 0) {
                                --total;
                            }
                            else {
                                red += (rgb >> 16 & 0xFF);
                                green += (rgb >> 8 & 0xFF);
                                blue += (rgb & 0xFF);
                            }
                        }
                    }
                    input.close();
                    if (total == 0) {
                        total = 1;
                    }
                    red /= total;
                    green /= total;
                    blue /= total;
                    c = (0xFF000000 | red << 16 | green << 8 | blue);
                    Minimap.textureColours.put(name, c);
                }
                else {
                    c = cachedColour;
                }
            }
            catch (FileNotFoundException e) {
                if (convert) {
                    return loadBlockColourFromTexture(state, b, pos, false);
                }
                c = b.getMapColor(state).colorValue;
                if (name != null) {
                    Minimap.textureColours.put(name, c);
                }
                System.out.println("Block file not found: " + b.getLocalizedName());
            }
            catch (Exception e2) {
                c = b.getMapColor(state).colorValue;
                if (name != null) {
                    Minimap.textureColours.put(name, c);
                }
                System.out.println("Block " + b.getLocalizedName() + " has no texture, using material colour.");
            }
            if (c != null) {
                Minimap.blockColours.put(stateId, c);
            }
        }
        final int grassColor = b.colorMultiplier((IBlockAccess)Minecraft.getMinecraft().theWorld, pos);
        if (grassColor != 16777215) {
            final float rMultiplier = (c >> 16 & 0xFF) / 255.0f;
            final float gMultiplier = (c >> 8 & 0xFF) / 255.0f;
            final float bMultiplier = (c & 0xFF) / 255.0f;
            red = (int)((grassColor >> 16 & 0xFF) * rMultiplier);
            green = (int)((grassColor >> 8 & 0xFF) * gMultiplier);
            blue = (int)((grassColor & 0xFF) * bMultiplier);
            c = (0xFF000000 | red << 16 | green << 8 | blue);
        }
        return c;
    }
    
    public boolean applyTransparentBlock(final Chunk bchunk, final Block b, final IBlockState state, final BlockPos globalPos, final BlockPos pos) {
        int red = 0;
        int green = 0;
        int blue = 0;
        int intensity = 1;
        boolean skip = false;
        if (b instanceof BlockLiquid && b.getLightOpacity() != 255 && b.getLightOpacity() != 0) {
            switch (XaeroMinimap.getSettings().blockColours) {
                case 1: {
                    red = 0;
                    green = 100;
                    blue = 225;
                    break;
                }
                case 0: {
                    final int waterColor = loadBlockColourFromTexture(state, b, globalPos, true);
                    red = (waterColor >> 16 & 0xFF);
                    green = (waterColor >> 8 & 0xFF);
                    blue = (waterColor & 0xFF);
                    break;
                }
            }
            intensity = 2;
            skip = true;
        }
        else if (XaeroMinimap.getSettings().blockColours == 0 && (b.getBlockLayer() == EnumWorldBlockLayer.TRANSLUCENT || b instanceof BlockGlass)) {
            final int glassColor = loadBlockColourFromTexture(state, b, globalPos, true);
            red = (glassColor >> 16 & 0xFF);
            green = (glassColor >> 8 & 0xFF);
            blue = (glassColor & 0xFF);
            skip = true;
        }
        else if (XaeroMinimap.getSettings().blockColours == 1 && b instanceof BlockStainedGlass) {
            final int i5 = bchunk.getBlockMetadata(pos);
            final int color = b.getMapColor(b.getStateFromMeta(i5)).colorValue;
            red = (color >> 16 & 0xFF);
            green = (color >> 8 & 0xFF);
            blue = (color & 0xFF);
            skip = true;
        }
        if (skip) {
            if (this.previousTransparentBlock == null) {
                this.postBrightness = this.getBlockBrightness(bchunk, new BlockPos(pos.getX(), Math.min(pos.getY() + 1, 255), pos.getZ()), 5.0f, this.sun, true);
            }
            final int rgb = red << 16 | green << 8 | blue;
            if (this.previousTransparentBlock == null || this.previousTransparentBlock != rgb) {
                this.previousTransparentBlock = rgb;
                final float overlayIntensity = intensity * this.getBlockBrightness(bchunk, new BlockPos(pos.getX(), Math.min(pos.getY() + 1, 255), pos.getZ()), 5.0f, this.sun, false);
                if (this.isGlowing(b)) {
                    final int[] colours = this.getBrightestColour(red, green, blue);
                    red = colours[0];
                    green = colours[1];
                    blue = colours[2];
                }
                this.divider += overlayIntensity;
                this.underRed += (int)(red * overlayIntensity);
                this.underGreen += (int)(green * overlayIntensity);
                this.underBlue += (int)(blue * overlayIntensity);
            }
            this.sun -= b.getLightOpacity();
            if (this.sun < 0) {
                this.sun = 0;
            }
        }
        return skip;
    }
    
    public Block findBlock(final Chunk bchunk, final int insideX, final int insideZ, final int highY, final int lowY) {
        boolean underair = false;
        for (int i = highY; i >= lowY; --i) {
            final Block got = bchunk.getBlock(insideX, i, insideZ);
            if (!(got instanceof BlockAir) && (underair || this.loadingCaving == -1)) {
                if (got.getRenderType() != -1) {
                    if (got != Blocks.torch) {
                        if (got != Blocks.tallgrass) {
                            if (!XaeroMinimap.getSettings().showFlowers) {
                                if (got instanceof BlockFlower || got instanceof BlockDoublePlant) {
                                    continue;
                                }
                                if (got instanceof BlockDeadBush) {
                                    continue;
                                }
                            }
                            if (!XaeroMinimap.getSettings().displayRedstone) {
                                if (got == Blocks.redstone_torch || got == Blocks.redstone_wire || got instanceof BlockRedstoneRepeater) {
                                    continue;
                                }
                                if (got instanceof BlockRedstoneComparator) {
                                    continue;
                                }
                            }
                            this.blockY = i;
                            int color = 0;
                            final BlockPos pos = new BlockPos(insideX, this.blockY, insideZ);
                            final BlockPos globalPos = this.getGlobalBlockPos(bchunk.xPosition, bchunk.zPosition, insideX, this.blockY, insideZ);
                            IBlockState state = bchunk.getBlockState(pos);
                            if (XaeroMinimap.getSettings().blockColours == 0) {
                            	state = got.getActualState(state, mc.theWorld, globalPos);
                            }
                            if (!this.applyTransparentBlock(bchunk, got, state, globalPos, pos)) {
                                if (XaeroMinimap.getSettings().blockColours == 1) {
                                    final MapColor minimapColor = got.getMapColor(state);
                                    color = minimapColor.colorValue;
                                }
                                else {
                                    color = loadBlockColourFromTexture(state, got, globalPos, true);
                                }
                                if (color != 0) {
                                    this.blockColor = color;
                                    return got;
                                }
                            }
                        }
                    }
                }
            }
            else if (got instanceof BlockAir) {
                underair = true;
            }
        }
        return null;
    }
    
    public BlockPos getGlobalBlockPos(final int chunkX, final int chunkZ, final int x, final int y, final int z) {
        return new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
    }
    
    public float getBlockBrightness(final Chunk c, final BlockPos pos, final float min, final int sun, final boolean dayLight) {
        if (!XaeroMinimap.getSettings().lighting) {
            return (min + sun) / (15.0f + min);
        }
        return (min + Math.max((dayLight ? Minimap.sunBrightness : 1.0f) * c.getLightFor(EnumSkyBlock.SKY, pos), c.getLightFor(EnumSkyBlock.BLOCK, pos))) / (15.0f + min);
    }
    
    public int[] getBrightestColour(int r, int g, int b) {
        final int max = Math.max(r, Math.max(g, b));
        if (max == 0) {
            return new int[] { r, g, b };
        }
        r = 255 * r / max;
        g = 255 * g / max;
        b = 255 * b / max;
        return new int[] { r, g, b };
    }
    
    public boolean isGlowing(final Block b) {
        return b.getLightValue() >= 0.5;
    }
    
    public void loadBlockColor(final int par1, final int par2, final Chunk bchunk, final int chunkX, final int chunkZ) {
        final int insideX = par1 & 0xF;
        final int insideZ = par2 & 0xF;
        final int playerY = (int)Minimap.mc.thePlayer.posY;
        final int height = bchunk.getHeight(insideX, insideZ);
        final int highY = (this.loadingCaving != -1) ? this.loadingCaving : (height + 3);
        int lowY = (this.loadingCaving != -1) ? (playerY - 30) : 0;
        if (lowY < 0) {
            lowY = 0;
        }
        this.blockY = 0;
        this.blockColor = 0;
        this.underRed = 0;
        this.underGreen = 0;
        this.underBlue = 0;
        this.divider = 1.0f;
        this.sun = 15;
        this.previousTransparentBlock = null;
        this.postBrightness = 1.0f;
        this.isglowing = false;
        final Block block = this.findBlock(bchunk, insideX, insideZ, highY, lowY);
        this.isglowing = (block != null && !(block instanceof BlockOre) && this.isGlowing(block));
        float brightness = 1.0f;
        if (!XaeroMinimap.getSettings().lighting && this.loadingCaving != -1) {
            brightness *= (float)Math.min(this.blockY / height, 1.0);
        }
        else {
            final BlockPos pos = new BlockPos(insideX, Math.min(this.blockY + 1, 255), insideZ);
            brightness = this.getBlockBrightness(bchunk, pos, 5.0f, this.sun, this.previousTransparentBlock == null);
        }
        double secondaryB = 1.0;
        if (this.lastBlockY[insideX] <= 0) {
            this.lastBlockY[insideX] = this.blockY;
            final Chunk prevChunk = Minimap.mc.theWorld.getChunkFromChunkCoords(this.loadingMapX + chunkX, this.loadingMapZ + chunkZ - 1);
            if (prevChunk != null && prevChunk.isLoaded()) {
                this.lastBlockY[insideX] = prevChunk.getHeight(insideX, 15) - 1;
            }
        }
        if (this.blockY < this.lastBlockY[insideX]) {
            secondaryB -= 0.15;
        }
        if (this.blockY > this.lastBlockY[insideX]) {
            secondaryB += 0.15;
        }
        brightness *= (float)secondaryB;
        this.lastBlockY[insideX] = this.blockY;
        if (this.blockColor == 0) {
            this.blockColor = 1;
        }
        int l = this.blockColor >> 16 & 0xFF;
        int i1 = this.blockColor >> 8 & 0xFF;
        int j1 = this.blockColor & 0xFF;
        if (this.isglowing) {
            final int[] colours = this.getBrightestColour(l, i1, j1);
            l = colours[0];
            i1 = colours[1];
            j1 = colours[2];
        }
        l = (int)((l * brightness + this.underRed) / this.divider * this.postBrightness);
        if (l > 255) {
            l = 255;
        }
        i1 = (int)((i1 * brightness + this.underGreen) / this.divider * this.postBrightness);
        if (i1 > 255) {
            i1 = 255;
        }
        j1 = (int)((j1 * brightness + this.underBlue) / this.divider * this.postBrightness);
        if (j1 > 255) {
            j1 = 255;
        }
        this.blockColor = (0xFF000000 | l << 16 | i1 << 8 | j1);
        if (chunkX < 0 || chunkX >= Minimap.loadingSide || chunkZ < 0 || chunkZ >= Minimap.loadingSide) {
            return;
        }
        MinimapChunk[] chunkmap = this.loadingBlocks[chunkX];
        if (chunkmap == null) {
            chunkmap = new MinimapChunk[Minimap.loadingSide];
            this.loadingBlocks[chunkX] = chunkmap;
        }
        MinimapChunk chunk = chunkmap[chunkZ];
        if (chunk == null) {
            chunk = new MinimapChunk(bchunk.xPosition, bchunk.zPosition);
            chunkmap[chunkZ] = chunk;
        }
        chunk.colors[insideX][insideZ] = this.blockColor;
    }
    
    public int getMapCoord(final int side, final double coord) {
        return (myFloor(coord) >> 4) - side / 2;
    }
    
    public int getLoadedBlockColor(final int par1, final int par2) {
        final int cX = (par1 >> 4) - this.loadedMapX;
        final int cZ = (par2 >> 4) - this.loadedMapZ;
        if (cX < 0 || cX >= Minimap.loadedSide || cZ < 0 || cZ >= Minimap.loadedSide) {
            return 1;
        }
        final MinimapChunk current = this.currentBlocks[cX][cZ];
        if (current != null) {
            return this.chunkOverlay(current.colors[par1 & 0xF][par2 & 0xF], current);
        }
        return 1;
    }
    
    public MinimapChunk[] getLoadedYChunks(final int par1) {
        final int cX = (par1 >> 4) - this.loadedMapX;
        if (cX < 0 || cX >= Minimap.loadedSide) {
            return null;
        }
        return this.currentBlocks[cX];
    }
    
    public int getLoadedBlockColor(final MinimapChunk[] yChunks, final int par1, final int par2) {
        final int cZ = (par2 >> 4) - this.loadedMapZ;
        if (cZ < 0 || cZ >= Minimap.loadedSide) {
            return 1;
        }
        final MinimapChunk current = yChunks[cZ];
        if (current != null) {
            return this.chunkOverlay(current.colors[par1 & 0xF][par2 & 0xF], current);
        }
        return 1;
    }
    
    public int gridOverlay(final int color) {
        final int grid = ModSettings.COLORS[XaeroMinimap.getSettings().chunkGrid];
        final int r = ((color >> 16 & 0xFF) * 3 + (grid >> 16 & 0xFF)) / 4;
        final int g = ((color >> 8 & 0xFF) * 3 + (grid >> 8 & 0xFF)) / 4;
        final int b = ((color & 0xFF) * 3 + (grid & 0xFF)) / 4;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
    
    public int slimeOverlay(final int color) {
        final int r = ((color >> 16 & 0xFF) + 82) / 2;
        final int g = ((color >> 8 & 0xFF) + 241) / 2;
        final int b = ((color & 0xFF) + 64) / 2;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
    
    public int chunkOverlay(final int color, final MinimapChunk c) {
        if (XaeroMinimap.getSettings().getSlimeChunks() && c.slimeChunk) {
            return this.slimeOverlay(color);
        }
        if (XaeroMinimap.getSettings().chunkGrid > -1 && c.chunkGrid) {
            return this.gridOverlay(color);
        }
        return color;
    }
    
    public static double getRenderAngle() {
        if (XaeroMinimap.getSettings().getLockNorth()) {
            return 90.0;
        }
        return getActualAngle();
    }
    
    public static double getActualAngle() {
        double rotation = Minimap.mc.thePlayer.rotationYaw;
        if (rotation < 0.0 || rotation > 360.0) {
            rotation %= 360.0;
        }
        double angle = 270.0 - rotation;
        if (angle < 0.0 || angle > 360.0) {
            angle %= 360.0;
        }
        return angle;
    }
    
    public double getZoom() {
        return this.minimapZoom;
    }
    
    public void updateZoom() {
        double target = XaeroMinimap.getSettings().zooms[XaeroMinimap.getSettings().zoom] * ((this.loadedCaving != -1) ? 3.0f : 1.0f);
        if (target > XaeroMinimap.getSettings().zooms[XaeroMinimap.getSettings().zooms.length - 1]) {
            target = XaeroMinimap.getSettings().zooms[XaeroMinimap.getSettings().zooms.length - 1];
        }
        double off = target - this.minimapZoom;
        if (off > 0.01 || off < -0.01) {
            off = (float)Animation.animate(off, 0.8);
        }
        else {
            off = 0.0;
        }
        this.minimapZoom = target - off;
    }
    
    public static double getEntityX(final Entity e, final float partial) {
        return e.lastTickPosX + (e.posX - e.lastTickPosX) * partial;
    }
    
    public static double getEntityZ(final Entity e, final float partial) {
        return e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partial;
    }
    
    public static void resetImage() {
        Minimap.toResetImage = true;
    }
    
    public static int myFloor(double d) {
        if (d < 0.0) {
            --d;
        }
        return (int)d;
    }
    
    public int getMinimapWidth() {
    	//Jello.addChatMessage(""+(Minimap.enlargedMap ? 448 : Minimap.minimapSizes[XaeroMinimap.getSettings().getMinimapSize()]));
    	if(true)
    		return 149;
    	//System.out.println(Minimap.minimapSizes[XaeroMinimap.getSettings().getMinimapSize()]);
        return Minimap.enlargedMap ? 448 : Minimap.minimapSizes[XaeroMinimap.getSettings().getMinimapSize()];
    }
    
    public int getBufferSize() {
        return Minimap.enlargedMap ? 512 : Minimap.bufferSizes[XaeroMinimap.getSettings().getMinimapSize()];
    }
    
    public int getFBOBufferSize() {
        return Minimap.enlargedMap ? 512 : Minimap.FBOBufferSizes[XaeroMinimap.getSettings().getMinimapSize()];
    }
    
    public static boolean usingFBO() {
        return Minimap.loadedFBO && !XaeroMinimap.getSettings().mapSafeMode;
    }
    
    public void updateMapFrame(final int bufferSize, final float partial) {
    	
        final EntityPlayer p = (EntityPlayer)Minimap.mc.thePlayer;
        final long before = System.currentTimeMillis();
        if (Minimap.toResetImage || usingFBO()) {
            this.bytes = new byte[bufferSize * bufferSize * 3];
            Minimap.toResetImage = false;
        }
        final boolean motionBlur = Minecraft.getDebugFps() >= 35;
        final int increaseY = motionBlur ? 2 : 1;
        int mapW = 0;
        int mapH = 0;
        int halfW = 0;
        int halfH = 0;
        if (!usingFBO()) {
            mapW = (mapH = this.getMinimapWidth());
            halfW = (mapW + 1) / 2;
            halfH = (mapH + 1) / 2;
        }
        final double halfWZoomed = halfW / Minimap.zoom;
        final double halfHZoomed = halfH / Minimap.zoom;
        if (usingFBO()) {
            final int chunkAmount = Minimap.loadedSide;
            final int actualSize = getLoadSide();
            Minimap.mapUpdateX = this.getMapCoord(actualSize, getEntityX((Entity)Minimap.mc.thePlayer, partial));
            Minimap.mapUpdateZ = this.getMapCoord(actualSize, getEntityZ((Entity)Minimap.mc.thePlayer, partial));
            final int chunkOffsetX = Minimap.mapUpdateX - this.loadedMapX;
            final int chunkOffsetZ = Minimap.mapUpdateZ - this.loadedMapZ;
            mapW = (mapH = Math.min(bufferSize, actualSize * 16));
            final double corner = Minimap.enlargedMap ? 0.0 : ((double)(int)(actualSize * (Math.sqrt(2.0) - 1.0) / Math.sqrt(2.0)));
            final int cornerZoomed = (int)(corner + actualSize * Math.sqrt(0.5) * (1.0 - 1.0 / Minimap.zoom) - 1.0);
            final int thing = actualSize - 1;
            for (int chunkX = 0; chunkX < chunkAmount - chunkOffsetX; ++chunkX) {
                final int transformedX = chunkX + chunkOffsetX;
                if (transformedX >= 0) {
                    if (transformedX < chunkAmount) {
                        final MinimapChunk[] yChunks = this.currentBlocks[transformedX];
                        if (yChunks != null) {
                            final int drawX = 16 * chunkX;
                            for (int chunkZ = 0; chunkZ < yChunks.length - chunkOffsetZ; ++chunkZ) {
                                if (chunkX + chunkZ >= cornerZoomed && thing - chunkZ + chunkX >= cornerZoomed && thing - chunkX + chunkZ >= cornerZoomed) {
                                    if (2 * thing - chunkX - chunkZ >= cornerZoomed) {
                                        final int transformedZ = chunkZ + chunkOffsetZ;
                                        if (transformedZ >= 0) {
                                            if (transformedZ < chunkAmount) {
                                                final MinimapChunk c = yChunks[transformedZ];
                                                if (c != null) {
                                                    final int drawY = 16 * chunkZ;
                                                    for (int insideX = 0; insideX < 16; ++insideX) {
                                                        final int[] blocks = c.colors[insideX];
                                                        final int pixelX = drawX + insideX;
                                                        if (pixelX <= mapW) {
                                                            if (pixelX >= 0) {
                                                                for (int insideZ = 0; insideZ < 16; ++insideZ) {
                                                                    final int pixelY = drawY + insideZ;
                                                                    if (pixelY >= 0) {
                                                                        if (pixelY <= mapH) {
                                                                        	//System.out.println(blocks[insideZ]);
                                                                            this.putColor(this.bytes, pixelX, pixelY, blocks[insideZ] == -16777216 ? 0xff8cafff : this.chunkOverlay(blocks[insideZ], c), bufferSize);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            byte currentState = this.drawYState;
            final double angle = Math.toRadians(getRenderAngle());
            final double ps = Math.sin(3.141592653589793 - angle);
            final double pc = Math.cos(3.141592653589793 - angle);
            final double playerX = getEntityX((Entity)Minimap.mc.thePlayer, partial);
            final double playerZ = getEntityZ((Entity)Minimap.mc.thePlayer, partial);
            for (int currentX = 0; currentX <= mapW + 1; ++currentX) {
                final double currentXZoomed = currentX / Minimap.zoom;
                final double offx = currentXZoomed - halfWZoomed;
                final double psx = ps * offx;
                final double pcx = pc * offx;
                for (int currentY = motionBlur ? currentState : 0; currentY <= mapH + 1; currentY += increaseY) {
                    final double offy = currentY / Minimap.zoom - halfHZoomed;
                    this.putColor(this.bytes, currentX, currentY, this.getLoadedBlockColor(myFloor(playerX + psx + pc * offy), myFloor(playerZ + ps * offy - pcx)), bufferSize);
                }
                currentState = (byte)((currentState != 1) ? 1 : 0);
            }
            this.renderEntityListSafeMode(p, Minimap.loadedEntities, pc, ps, mapW, bufferSize, halfW, halfH, playerX, playerZ, partial);
            this.renderEntityListSafeMode(p, Minimap.loadedItems, pc, ps, mapW, bufferSize, halfW, halfH, playerX, playerZ, partial);
            this.renderEntityListSafeMode(p, Minimap.loadedHostile, pc, ps, mapW, bufferSize, halfW, halfH, playerX, playerZ, partial);
            this.renderEntityListSafeMode(p, Minimap.loadedLiving, pc, ps, mapW, bufferSize, halfW, halfH, playerX, playerZ, partial);
            this.renderEntityListSafeMode(p, Minimap.loadedPlayers, pc, ps, mapW, bufferSize, halfW, halfH, playerX, playerZ, partial);
            this.renderEntityDotSafeMode(p, (Entity)p, pc, ps, mapW, bufferSize, halfW, halfH, playerX, playerZ, partial);
            this.drawYState = (byte)((this.drawYState != 1) ? 1 : 0);
            final ByteBuffer buffer = Minimap.mapTexture.getBuffer(bufferSize);
            buffer.put(this.bytes);
            buffer.flip();
        }
    }
    
    public void renderEntityListSafeMode(final EntityPlayer p, final ArrayList<Entity> list, final double pc, final double ps, final int mapW, final int bufferSize, final int halfW, final int halfH, final double playerX, final double playerZ, final float partial) {
        for (int i = 0; i < list.size(); ++i) {
            final Entity e = list.get(i);
            if (p != e) {
                if (!this.renderEntityDotSafeMode(p, e, pc, ps, mapW, bufferSize, halfW, halfH, playerX, playerZ, partial)) {}
            }
        }
    }
    
    public boolean renderEntityDotSafeMode(final EntityPlayer p, final Entity e, final double pc, final double ps, final int mapW, final int bufferSize, final int halfW, final int halfH, final double playerX, final double playerZ, final float partial) {
        if (!shouldRenderEntity(e)) {
            return false;
        }
        final double offx = getEntityX(e, partial) - playerX;
        final double offz = getEntityZ(e, partial) - playerZ;
        final double offh = p.posY - e.posY;
        final double Z = pc * offx + ps * offz;
        final double X = ps * offx - pc * offz;
        final int drawX = myFloor(halfW + X * Minimap.zoom);
        final int drawY = myFloor(halfH + Z * Minimap.zoom);
        XaeroMinimap.getSettings();
        final int color = getEntityColour(p, e, offh);
        for (int a = drawX - 2; a < drawX + 4; ++a) {
            if (a >= 0) {
                if (a <= mapW) {
                    for (int b = drawY - 2; b < drawY + 4; ++b) {
                        if (b >= 0) {
                            if (b <= mapW) {
                                if (((a != drawX - 2 && a != drawX + 3) || (b != drawY - 2 && b != drawY + 3)) && (a != drawX + 2 || b != drawY - 2) && (a != drawX + 3 || b != drawY - 1) && (a != drawX - 2 || b != drawY + 2)) {
                                    if (a != drawX - 1 || b != drawY + 3) {
                                        if (a == drawX + 3 || b == drawY + 3 || (a == drawX + 2 && b == drawY + 2)) {
                                            this.putColor(this.bytes, a, b, 0, bufferSize);
                                        }
                                        else {
                                            this.putColor(this.bytes, a, b, color, bufferSize);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
    
    private int getCaving() {
        if (!XaeroMinimap.settings.getCaveMaps()) {
            return -1;
        }
        final int x = myFloor(Minimap.mc.thePlayer.posX);
        final int y = Math.max((int)Minimap.mc.thePlayer.posY + 1, 0);
        final int z = myFloor(Minimap.mc.thePlayer.posZ);
        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        final int insideX = x & 0xF;
        final int insideZ = z & 0xF;
        final Chunk bchunk = Minimap.mc.theWorld.getChunkFromChunkCoords(chunkX, chunkZ);
        final int top = bchunk.getHeight(insideX, insideZ);
        if (y < top) {
            for (int i = y; i < top; ++i) {
                final Block got = bchunk.getBlock(insideX, i, insideZ);
                if (got.getMaterial().isOpaque()) {
                    return Math.min(y + 3, i);
                }
            }
        }
        return -1;
    }
    
    private void putColor(final byte[] bytes, final int x, final int y, final int color, final int size) {
        int pixel = (y * size + x) * 3;
        bytes[pixel] = (byte)(color >> 16 & 0xFF);
        bytes[++pixel] = (byte)(color >> 8 & 0xFF);
        bytes[++pixel] = (byte)(color & 0xFF);
    }
    
    public static void loadFrameBuffer() {
        if (!GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            System.out.println("FBO not supported! Using minimap safe mode.");
        }
        else {
            if (!Minecraft.getMinecraft().gameSettings.fboEnable) {
                Minecraft.getMinecraft().gameSettings.setOptionValue(GameSettings.Options.FBO_ENABLE, 0);
                System.out.println("FBO is supported but off. Turning it on.");
            }
            Minimap.scalingFrameBuffer = new Framebuffer(512, 512, false);
            Minimap.rotationFrameBuffer = new Framebuffer(512, 512, false);
            Minimap.loadedFBO = (Minimap.scalingFrameBuffer.framebufferObject != -1);
        }
        Minimap.triedFBO = true;
    }
    
    public void renderFrameToFBO(final int bufferSize, final int viewW, final float sizeFix, final float partial, final boolean retryIfError) {
        Minimap.updatePause = true;
        final int chunkAmount = getLoadSide();
        final int mapW = chunkAmount * 16;
        final double playerX = getEntityX((Entity)Minimap.mc.thePlayer, partial);
        final double playerZ = getEntityZ((Entity)Minimap.mc.thePlayer, partial);
        final int xFloored = myFloor(playerX);
        final int zFloored = myFloor(playerZ);
        int offsetX = xFloored & 0xF;
        int offsetZ = zFloored & 0xF;
        final int mapX = this.getMapCoord(chunkAmount, playerX);
        final int mapZ = this.getMapCoord(chunkAmount, playerZ);
        final boolean zooming = (int)Minimap.zoom != Minimap.zoom;
        final ByteBuffer buffer = Minimap.mapTexture.getBuffer(bufferSize);
        if (mapX != Minimap.mapUpdateX || mapZ != Minimap.mapUpdateZ || zooming || !retryIfError) {
            if (!Minimap.frameIsUpdating) {
                Minimap.frameIsUpdating = true;
                this.updateMapFrame(bufferSize, partial);
                Minimap.frameIsUpdating = false;
                buffer.put(this.bytes);
                buffer.flip();
                Minimap.bufferSizeToUpdate = -1;
                Minimap.frameUpdateNeeded = false;
            }
            else {
                offsetX += 16 * (mapX - Minimap.mapUpdateX);
                offsetZ += 16 * (mapZ - Minimap.mapUpdateZ);
            }
        }
        Minimap.scalingFrameBuffer.bindFramebuffer(true);
        GL11.glClear(16640);
        GL11.glEnable(3553);
        RenderHelper.disableStandardItemLighting();
        try {
            bindTextureBuffer(buffer, bufferSize, bufferSize, Minimap.mapTexture.getGlTextureId());
        }
        catch (Exception e) {
            if (retryIfError) {
                System.out.println("Error when binding texture buffer. Retrying...");
                this.renderFrameToFBO(bufferSize, viewW, sizeFix, partial, false);
            }
            else {
                System.out.println("Error after retrying... :( Please report to Xaero96 on MinecraftForum of PlanetMinecraft!");
            }
        }
        long before = System.currentTimeMillis();
        if (!zooming) {
            GL11.glTexParameteri(3553, 10240, 9728);
        }
        else {
            GL11.glTexParameteri(3553, 10240, 9729);
        }
        GlStateManager.clear(256);
        GlStateManager.matrixMode(5889);
        GL11.glPushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0, 512.0, 512.0, 0.0, 1000.0, 3000.0);
        GlStateManager.matrixMode(5888);
        GL11.glPushMatrix();
        GlStateManager.loadIdentity();
        before = System.currentTimeMillis();
        double xInsidePixel = getEntityX((Entity)Minimap.mc.thePlayer, partial) - xFloored;
        if (xInsidePixel < 0.0) {
            ++xInsidePixel;
        }
        double zInsidePixel = getEntityZ((Entity)Minimap.mc.thePlayer, partial) - zFloored;
        if (zInsidePixel < 0.0) {
            ++zInsidePixel;
        }
        zInsidePixel = 1.0 - zInsidePixel;
        final float halfW = mapW / 2.0f;
        final float halfWView = viewW / 2.0f;
        final float angle = (float)(90.0 - getRenderAngle());
        GlStateManager.translate(256.0f, 256.0f, -2000.0f);
        //Jello.addChatMessage(Minimap.zoom+"");
        //Minimap.zoom = 1;
        GlStateManager.scale(Minimap.zoom, Minimap.zoom, 1.0);
        drawMyTexturedModalRect(-halfW - offsetX + 8.0f, -halfW - offsetZ + 7.0f, 0, 0, mapW + offsetX, mapW + offsetZ, bufferSize);
        Minimap.scalingFrameBuffer.unbindFramebuffer();
        Minimap.rotationFrameBuffer.bindFramebuffer(false);
        GL11.glClear(16640);
        Minimap.scalingFrameBuffer.bindFramebufferTexture();
        GlStateManager.loadIdentity();
        if (XaeroMinimap.getSettings().getAntiAliasing()) {
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10241, 9729);
        }
        else {
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glTexParameteri(3553, 10241, 9728);
        }
        GlStateManager.translate(halfWView + 0.5f, 511.5f - halfWView, -2000.0f);
        if (!XaeroMinimap.getSettings().getLockNorth()) {
            GL11.glRotatef(angle, 0.0f, 0.0f, 1.0f);
        }
        GL11.glPushMatrix();
        GlStateManager.translate(-xInsidePixel * Minimap.zoom, -zInsidePixel * Minimap.zoom, 0.0);
        GlStateManager.disableBlend();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, XaeroMinimap.getSettings().minimapOpacity / 100.0f);
        drawMyTexturedModalRect(-256.0f, -256.0f, 0, 0, 512.0f, 512.0f, 512.0f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
        before = System.currentTimeMillis();
        //Minimap.mc.getTextureManager().bindTexture(InterfaceHandler.guiTextures);
        if (XaeroMinimap.getSettings().getSmoothDots()) {
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10241, 9729);
        }
        else {
            GL11.glTexParameteri(3553, 10240, 9728);
            GL11.glTexParameteri(3553, 10241, 9728);
        }
        GlStateManager.enableBlend();
        GL14.glBlendFuncSeparate(770, 771, 1, 771);
        final EntityPlayer p = (EntityPlayer)Minimap.mc.thePlayer;
        /*renderEntityListToFBO(p, Minimap.loadedEntities, angle, playerX, playerZ, partial);
        renderEntityListToFBO(p, Minimap.loadedItems, angle, playerX, playerZ, partial);
        renderEntityListToFBO(p, Minimap.loadedLiving, angle, playerX, playerZ, partial);
        renderEntityListToFBO(p, Minimap.loadedHostile, angle, playerX, playerZ, partial);
        renderEntityListToFBO(p, Minimap.loadedPlayers, angle, playerX, playerZ, partial);*/
        //Minimap.mc.getTextureManager().bindTexture(InterfaceHandler.guiTextures);
        //renderEntityDotToFBO(p, (Entity)p, angle, playerX, playerZ, partial);
        GL11.glTexParameteri(3553, 10240, 9728);
        GL11.glTexParameteri(3553, 10241, 9728);
        Minimap.rotationFrameBuffer.unbindFramebuffer();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
        Minimap.updatePause = false;
        GlStateManager.matrixMode(5889);
        GL11.glPopMatrix();
        GlStateManager.matrixMode(5888);
        GL11.glPopMatrix();
    }
    
    public static void renderEntityListToFBO(final EntityPlayer p, final ArrayList<Entity> list, final float angle, final double playerX, final double playerZ, final float partial) {
        for (int i = 0; i < list.size(); ++i) {
            final Entity e = list.get(i);
            if (p != e) {
                if (Keyboard.isKeyDown(15) && e instanceof AbstractClientPlayer) {
                    renderPlayerHeadToFBO(p, (AbstractClientPlayer)e, angle, playerX, playerZ, partial);
                }
                else {
                    renderEntityDotToFBO(p, e, angle, playerX, playerZ, partial);
                }
            }
        }
    }
    
    public static void renderPlayerHeadToFBO(final EntityPlayer mainPlayer, final AbstractClientPlayer e, final float angle, final double playerX, final double playerZ, final float partial) {
        if (!shouldRenderEntity((Entity)e)) {
            return;
        }
        final double offx = getEntityX((Entity)e, partial) - playerX;
        final double offz = playerZ - getEntityZ((Entity)e, partial);
        final double offh = mainPlayer.posY - e.posY;
        GL11.glPushMatrix();
        GlStateManager.translate(offx * Minimap.zoom, offz * Minimap.zoom, 0.0);
        if (!XaeroMinimap.getSettings().getLockNorth()) {
            GL11.glRotatef(-angle, 0.0f, 0.0f, 1.0f);
        }
        GlStateManager.scale(2.0, 2.0, 2.0);
        GlStateManager.translate(-4.0f, -4.0f, 0.0f);
        final double brightness = getEntityBrightness(offh);
        GL11.glColor3d(brightness, brightness, brightness);
        final boolean flag1 = e != null && e.func_175148_a(EnumPlayerModelParts.CAPE) && (e.getGameProfile().getName().equals("Dinnerbone") || e.getGameProfile().getName().equals("Grumm"));
        Minecraft.getMinecraft().getTextureManager().bindTexture(e.getLocationSkin());
        final int l2 = 8 + (flag1 ? 8 : 0);
        final int i3 = 8 * (flag1 ? -1 : 1);
        Gui.drawScaledCustomSizeModalRect(0, 0, 8.0f, l2 + i3, 8, -i3, 8, 8, 64.0f, 64.0f);
        if (e != null && e.func_175148_a(EnumPlayerModelParts.HAT)) {
            final int j3 = 8 + (flag1 ? 8 : 0);
            final int k3 = 8 * (flag1 ? -1 : 1);
            Gui.drawScaledCustomSizeModalRect(0, 0, 40.0f, j3 + k3, 8, -k3, 8, 8, 64.0f, 64.0f);
        }
        GL11.glPopMatrix();
    }
    
    public static void renderEntityDotToFBO(final EntityPlayer p, final Entity e, final float angle, final double playerX, final double playerZ, final float partial) {
        try {
            if (!shouldRenderEntity(e)) {
                return;
            }
            final double offx = getEntityX(e, partial) - playerX;
            final double offz = playerZ - getEntityZ(e, partial);
            final double offh = p.posY - e.posY;
            GL11.glPushMatrix();
            GlStateManager.translate(offx * Minimap.zoom, offz * Minimap.zoom, 0.0);
            if (!XaeroMinimap.getSettings().getLockNorth()) {
                GL11.glRotatef(-angle, 0.0f, 0.0f, 1.0f);
            }
            GlStateManager.translate(-0.5f, -0.5f, 0.0f);
            final int color = getEntityColour(p, e, offh);
            final float f = (color >> 16 & 0xFF) / 255.0f;
            final float f2 = (color >> 8 & 0xFF) / 255.0f;
            final float f3 = (color & 0xFF) / 255.0f;
            GL11.glColor4f(f, f2, f3, 1.0f);
            GL11.glScalef(XaeroMinimap.getSettings().dotsScale, XaeroMinimap.getSettings().dotsScale, 1.0f);
            drawMyTexturedModalRect(-2.0f, -3.0f, XaeroMinimap.getSettings().getSmoothDots() ? 2 : 10, XaeroMinimap.getSettings().getSmoothDots() ? 89 : 78, 6.0f, 6.0f, 256.0f);
            GL11.glPopMatrix();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static void drawMyTexturedModalRect(final float x, final float y, final int textureX, final int textureY, final float width, final float height, final float factor) {
        final float f2;
        final float f = f2 = 1.0f / factor;
        final Tessellator tessellator = Tessellator.getInstance();
        final WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.startDrawingQuads();
        worldrenderer.addVertexWithUV((double)(x + 0.0f), (double)(y + height), 0.0, (double)((textureX + 0) * f), (double)((textureY + height) * f2));
        worldrenderer.addVertexWithUV((double)(x + width), (double)(y + height), 0.0, (double)((textureX + width) * f), (double)((textureY + height) * f2));
        worldrenderer.addVertexWithUV((double)(x + width), (double)(y + 0.0f), 0.0, (double)((textureX + width) * f), (double)((textureY + 0) * f2));
        worldrenderer.addVertexWithUV((double)(x + 0.0f), (double)(y + 0.0f), 0.0, (double)((textureX + 0) * f), (double)((textureY + 0) * f2));
        tessellator.draw();
    }
    
    public static void bindTextureBuffer(final ByteBuffer image, final int width, final int height, final int par0) {
        GL11.glBindTexture(3553, par0);
        GL11.glTexImage2D(3553, 0, 6407, width, height, 0, 6407, 5121, image);
    }
    
    public static boolean shouldRenderEntity(final Entity e) {
        return !e.isSneaking() && !e.isInvisible();
    }
    
    public static int getEntityColour(final EntityPlayer p, final Entity e, final double offh) {
        int color = ModSettings.COLORS[XaeroMinimap.getSettings().otherColor];
        if (e instanceof EntityPlayer) {
            if (XaeroMinimap.getSettings().otherTeamColor != -1 && p.getTeam() != ((EntityLivingBase)e).getTeam()) {
                color = ModSettings.COLORS[XaeroMinimap.getSettings().otherTeamColor];
            }
            else if (XaeroMinimap.getSettings().playersColor != -1) {
                color = ModSettings.COLORS[XaeroMinimap.getSettings().playersColor];
            }
            else {
                final ScorePlayerTeam team = (ScorePlayerTeam)((EntityPlayer)e).getTeam();
                if (team != null && team.getColorPrefix() != null && team.getColorPrefix().length() > 1) {
                    color = Minecraft.getMinecraft().fontRendererObj.func_175064_b(team.getColorPrefix().charAt(1));
                }
                else {
                    color = Minimap.radarPlayers.hashCode();
                }
            }
        }
        else if (e instanceof EntityMob || e instanceof IMob) {
            color = ModSettings.COLORS[XaeroMinimap.getSettings().hostileColor];
        }
        else if (e instanceof EntityLiving) {
            color = ModSettings.COLORS[XaeroMinimap.getSettings().mobsColor];
        }
        else if (e instanceof EntityItem) {
            color = ModSettings.COLORS[XaeroMinimap.getSettings().itemsColor];
        }
        int l = color >> 16 & 0xFF;
        int i1 = color >> 8 & 0xFF;
        int j1 = color & 0xFF;
        final double brightness = getEntityBrightness(offh);
        if (brightness < 1.0) {
            l *= (int)brightness;
            if (l > 255) {
                l = 255;
            }
            i1 *= (int)brightness;
            if (i1 > 255) {
                i1 = 255;
            }
            j1 *= (int)brightness;
            if (j1 > 255) {
                j1 = 255;
            }
            color = (0xFF000000 | l << 16 | i1 << 8 | j1);
        }
        return color;
    }
    
    public static double getEntityBrightness(final double offh) {
        double level = 20.0 - offh;
        if (level < 0.0) {
            level = 0.0;
        }
        double brightness = 1.0;
        if (level <= 10.0 && XaeroMinimap.getSettings().showEntityHeight) {
            brightness = level / 20.0;
        }
        return brightness;
    }
    
    static {
        Minimap.loadingSide = 16;
        Minimap.loadedSide = 16;
        Minimap.updateRadius = 16;
        Minimap.enlargedMap = false;
        Minimap.mc = Minecraft.getMinecraft();
        radarPlayers = new Color(255, 255, 255);
        radarShadow = new Color(0, 0, 0);
        Minimap.loadedPlayers = new ArrayList<Entity>();
        Minimap.loadedLiving = new ArrayList<Entity>();
        Minimap.loadedHostile = new ArrayList<Entity>();
        Minimap.loadedItems = new ArrayList<Entity>();
        Minimap.loadedEntities = new ArrayList<Entity>();
        Minimap.blocksLoaded = 0;
        Minimap.worldID = null;
        Minimap.customWorldID = null;
        Minimap.frameIsUpdating = false;
        Minimap.frameUpdateNeeded = false;
        Minimap.bufferSizeToUpdate = -1;
        Minimap.frameUpdatePartialTicks = 1.0f;
        Minimap.updatePause = false;
        Minimap.textureColours = new HashMap<String, Integer>();
        Minimap.blockColours = new HashMap<Integer, Integer>();
        Minimap.clearBlockColours = false;
        Minimap.toResetImage = true;
        Minimap.zoom = 1.0;
        minimapSizes = new int[] { 112, 168, 224, 336 };
        bufferSizes = new int[] { 128, 256, 256, 512 };
        FBOMinimapSizes = new int[] { 11, 17, 21, 31 };
        FBOBufferSizes = new int[] { 256, 512, 512, 512 };
        Minimap.triedFBO = false;
        Minimap.loadedFBO = false;
        Minimap.mapTexture = new DynamicTexture(InterfaceHandler.mapTextures);
    }
    
    public class MapLoader implements Runnable
    {
        @Override
        public void run() {
            int updateChunkX = 0;
            int updateChunkZ = 0;
            while (true) {
                final long before = System.currentTimeMillis();
                boolean sleep = true;
                try {
                    if (XaeroMinimap.getSettings() == null || !XaeroMinimap.getSettings().getMinimap() || Minimap.mc.thePlayer == null || Minimap.mc.theWorld == null) {
                        Thread.sleep(100L);
                    }
                    else {
                        final long before2 = System.currentTimeMillis();
                        if (updateChunkX == 0 && updateChunkZ == 0) {
                            if (Minimap.clearBlockColours) {
                                Minimap.clearBlockColours = false;
                                if (!Minimap.blockColours.isEmpty()) {
                                    Minimap.blockColours.clear();
                                    Minimap.textureColours.clear();
                                    System.out.println("Minimap block colour cache cleaned.");
                                }
                            }
                            Minimap.sunBrightness = Minimap.mc.theWorld.getSunBrightness(1.0f);
                            Minimap.loadingSide = Minimap.getLoadSide();
                            Minimap.updateRadius = Minimap.getUpdateRadius();
                            Minimap.this.loadingMapX = Minimap.this.getMapCoord(Minimap.loadingSide, Minimap.mc.thePlayer.posX);
                            Minimap.this.loadingMapZ = Minimap.this.getMapCoord(Minimap.loadingSide, Minimap.mc.thePlayer.posZ);
                            Minimap.this.loadingCaving = Minimap.this.getCaving();
                            Minimap.this.loadingBlocks = new MinimapChunk[Minimap.loadingSide][Minimap.loadingSide];
                        }
                        sleep = this.updateChunk(updateChunkX, updateChunkZ);
                        if (updateChunkX == Minimap.loadingSide - 1 && updateChunkZ == Minimap.loadingSide - 1) {
                            Minimap.this.currentBlocks = Minimap.this.loadingBlocks;
                            Minimap.loadedSide = Minimap.loadingSide;
                            Minimap.this.loadedMapX = Minimap.this.loadingMapX;
                            Minimap.this.loadedMapZ = Minimap.this.loadingMapZ;
                            Minimap.this.loadedCaving = Minimap.this.loadingCaving;
                            Minimap.frameUpdateNeeded = Minimap.usingFBO();
                        }
                        updateChunkZ = (updateChunkZ + 1) % Minimap.loadingSide;
                        if (updateChunkZ == 0) {
                            updateChunkX = (updateChunkX + 1) % Minimap.loadingSide;
                            Minimap.this.lastBlockY = new int[16];
                            final EntityPlayer p = (EntityPlayer)Minimap.mc.thePlayer;
                            final ArrayList<Entity> loadingPlayers = new ArrayList<Entity>();
                            final ArrayList<Entity> loadingHostile = new ArrayList<Entity>();
                            final ArrayList<Entity> loadingLiving = new ArrayList<Entity>();
                            final ArrayList<Entity> loadingItems = new ArrayList<Entity>();
                            final ArrayList<Entity> loadingEntities = new ArrayList<Entity>();
                            for (int i = 0; i < mc.theWorld.loadedEntityList.size(); ++i) {
                                try {
                                    final Entity e = (Entity) mc.theWorld.loadedEntityList.get(i);
                                    int type = 0;
                                    if (e instanceof EntityPlayer) {
                                        if (e != p && (!XaeroMinimap.getSettings().getShowPlayers() || (!XaeroMinimap.settings.getShowOtherTeam() && p.getTeam() != ((EntityLivingBase)e).getTeam()))) {
                                            continue;
                                        }
                                        type = 1;
                                    }
                                    else if (e instanceof EntityMob || e instanceof IMob) {
                                        if (!XaeroMinimap.getSettings().getShowHostile()) {
                                            continue;
                                        }
                                        type = 2;
                                    }
                                    else if (e instanceof EntityLiving) {
                                        if (!XaeroMinimap.getSettings().getShowMobs()) {
                                            continue;
                                        }
                                        type = 3;
                                    }
                                    else if (e instanceof EntityItem) {
                                        if (!XaeroMinimap.getSettings().getShowItems()) {
                                            continue;
                                        }
                                        type = 4;
                                    }
                                    else if (!XaeroMinimap.getSettings().getShowOther()) {
                                        continue;
                                    }
                                    final double offx = e.posX - p.posX;
                                    final double offy = e.posZ - p.posZ;
                                    final double offh = p.posY - e.posY;
                                    final double offheight2 = offh * offh;
                                    final double offx2 = offx * offx;
                                    final double offy2 = offy * offy;
                                    final double maxDistance = 31250.0 / (Minimap.this.getZoom() * Minimap.this.getZoom());
                                    if (offx2 <= maxDistance && offy2 <= maxDistance && offheight2 <= 400.0) {
                                        ArrayList<Entity> typeList = loadingEntities;
                                        switch (type) {
                                            case 1: {
                                                typeList = loadingPlayers;
                                                break;
                                            }
                                            case 2: {
                                                typeList = loadingHostile;
                                                break;
                                            }
                                            case 3: {
                                                typeList = loadingLiving;
                                                break;
                                            }
                                            case 4: {
                                                typeList = loadingItems;
                                                break;
                                            }
                                        }
                                        typeList.add(e);
                                        if (XaeroMinimap.getSettings().entityAmount != 0 && typeList.size() >= 100 * XaeroMinimap.getSettings().entityAmount) {
                                            break;
                                        }
                                    }
                                }
                                catch (Exception e4) {}
                            }
                            Minimap.loadedPlayers = loadingPlayers;
                            Minimap.loadedHostile = loadingHostile;
                            Minimap.loadedLiving = loadingLiving;
                            Minimap.loadedItems = loadingItems;
                            Minimap.loadedEntities = loadingEntities;
                        }
                        if (!Minimap.frameIsUpdating && Minimap.frameUpdateNeeded) {
                            Minimap.frameUpdateNeeded = false;
                            Minimap.frameIsUpdating = true;
                            Minimap.bufferSizeToUpdate = Minimap.this.getFBOBufferSize();
                            Minimap.this.updateMapFrame(Minimap.bufferSizeToUpdate, Minimap.frameUpdatePartialTicks);
                            Minimap.frameIsUpdating = false;
                        }
                        if (!Minimap.updatePause && Minimap.bufferSizeToUpdate != -1) {
                            final ByteBuffer buffer = Minimap.mapTexture.getBuffer(Minimap.bufferSizeToUpdate);
                            buffer.put(Minimap.this.bytes);
                            buffer.flip();
                            Minimap.bufferSizeToUpdate = -1;
                        }
                    }
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                    Minimap.frameIsUpdating = false;
                }
                final int passed = (int)(System.currentTimeMillis() - before);
                try {
                    if (sleep && passed <= 5) {
                        Thread.sleep(5 - passed);
                    }
                    else {
                        Thread.sleep(1L);
                    }
                }
                catch (InterruptedException e3) {
                    e3.printStackTrace();
                }
            }
        }
        
        public boolean updateChunk(final int x, final int z) {
            final int chunkX = Minimap.this.loadingMapX + x;
            final int chunkZ = Minimap.this.loadingMapZ + z;
            final int halfSide = Minimap.loadingSide / 2;
            final int fromCenterX = x - halfSide;
            final int fromCenterZ = z - halfSide;
            final int xOld = chunkX - Minimap.this.loadedMapX;
            final int zOld = chunkZ - Minimap.this.loadedMapZ;
            MinimapChunk current = null;
            if (Minimap.this.currentBlocks != null && xOld > -1 && xOld < Minimap.this.currentBlocks.length && zOld > -1 && zOld < Minimap.this.currentBlocks.length) {
                current = Minimap.this.currentBlocks[xOld][zOld];
            }
            final Chunk bchunk = Minimap.mc.theWorld.getChunkFromChunkCoords(chunkX, chunkZ);
            if ((int)Minimap.zoom == Minimap.zoom && (!bchunk.isLoaded() || ((fromCenterX > Minimap.updateRadius || fromCenterZ > Minimap.updateRadius || fromCenterX < -Minimap.updateRadius || fromCenterZ < -Minimap.updateRadius) && current != null))) {
                if (current != null) {
                    Minimap.this.loadingBlocks[x][z] = current;
                    for (int i = 0; i < 16; ++i) {
                        Minimap.this.lastBlockY[i] = current.lastHeights[i];
                    }
                }
                else {
                    Minimap.this.lastBlockY = new int[16];
                }
                return false;
            }
            final int x2 = chunkX * 16;
            final int z2 = chunkZ * 16;
            for (int blockX = x2; blockX < x2 + 16; ++blockX) {
                for (int blockZ = z2; blockZ < z2 + 16; ++blockZ) {
                    Minimap.this.loadBlockColor(blockX, blockZ, bchunk, x, z);
                    if ((blockZ & 0xF) == 0xF) {
                        Minimap.this.loadingBlocks[x][z].lastHeights[blockX & 0xF] = Minimap.this.lastBlockY[blockX & 0xF];
                    }
                }
            }
            return true;
        }
    }
}
