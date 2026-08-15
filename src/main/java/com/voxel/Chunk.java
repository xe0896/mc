package com.voxel;

import com.voxel.enums.BlockType;

public class Chunk {
    public static final int W = 16;
    public static final int H = 64;
    public static final int D = 16;
    private static final BlockType[] values = BlockType.values();
    public final byte[] chunk;

    public Chunk() {
        // Default value is 0000000 so AIR
        this.chunk = new byte[W*H*D];
    }
    
    private int index(int x, int y, int z) {
        int idx = (x) + (y * W) + (z * W * H);
        return idx;
    }

    // Given x,y,z coordinates; we go into our chunk and look at the byte stored at that
    // coordinate, this byte would index to a specific BlockType enum that we want to return
    // cast it to an integer then this would be an entry in that BlockType enum as an int
    // and use .values() to get whatever is stored for this specific index
    public BlockType getBlock(int x, int y, int z) {
        if(x < 0 || y < 0 || z < 0 || x >= W || y >= H || z >= D) return BlockType.AIR;
        int idx = this.chunk[this.index(x, y, z)];
        return values[idx];
    }

    // Given x,y,z coordinates and a specific block; use ordinal() to see what integer this
    // type represents in the BlockType enum, we then get the block that we need to change
    // by the coordinates and change what is stored at idx
    public void setBlock(int x, int y, int z, BlockType type) {
        int enumInt = type.ordinal();
        int idx = this.index(x, y, z);

        this.chunk[idx] = (byte) enumInt;
    }
}
