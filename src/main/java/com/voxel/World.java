package com.voxel;

import com.voxel.enums.BlockType;
import static org.lwjgl.stb.STBImage.*;

import java.nio.IntBuffer;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalDate;
import com.voxel.Noise;

public class World {
    private Chunk[][] chunks;
    private final Noise noise;
    private final int N;

    public World(int N) {
        this.N = N;
        this.noise = new Noise(LocalDate.now().toEpochDay());
        this.chunks = new Chunk[N][N];

        ByteBuffer buf;
        IntBuffer width;
        IntBuffer height;
        IntBuffer channels_in_files;

        try (var stack = stackPush()) {
            // mallocInt says how many integers we want
            width = stack.mallocInt(1);
            height = stack.mallocInt(1);
            channels_in_files = stack.mallocInt(32);
            buf = stbi_load("src/main/resources/grass_block.jpg", width, height, channels_in_files, 3);
        }

        System.out.println(buf);

        Block block = new Block(buf, width.get(), height.get(), channels_in_files.get());
        
        for(int cx = 0; cx < N; cx++) {
            for(int cz = 0; cz < N; cz++) {
                Chunk c = new Chunk(cx, cz, noise);
                //c.flat();
                c.generate();
                chunks[cx][cz] = c;
            }
        }

        for(int cx = 0; cx < N; cx++) {
            for(int cz = 0; cz < N; cz++) {
                Chunk c = chunks[cx][cz];
                c.buildMesh(this, block);
            }
        }
    }

    public void render(int shaderId) {
        for(int cx = 0; cx < chunks.length; cx++) {
            for(int cz = 0; cz < chunks.length; cz++) {
                Chunk c = chunks[cx][cz];
                c.render(shaderId);
            }
        }
    }

    public void printVertexCount() {
        int vertexCount = 0;
        for(int i = 0; i < N; i++) {
            for(int j = 0; j < N; j++) {
                Chunk c = chunks[i][j];
                vertexCount += c.mesh.vertexCount;
            }
        }
        System.out.println("Vertex count: " + vertexCount);
    }

    public Chunk getChunk(int cx, int cz) {
        if(cx < 0 || cz < 0 || cx >= N|| cz >= N) return null;
        return chunks[cx][cz];
    }

    // World co-ordinates provided
    public BlockType getBlock(int wx, int wy, int wz) {
        int cx = Math.floorDiv(wx, Chunk.W);
        int cz = Math.floorDiv(wz, Chunk.D);

        Chunk c = this.getChunk(cx, cz);
        if(c == null) return BlockType.AIR; // Outside the chunks so return AIR

        // wx = cx * 16 + localX
        int lx = Math.floorMod(wx, Chunk.W);
        int lz = Math.floorMod(wz, Chunk.D);
        return c.getBlock(lx, wy, lz);
    }
}
