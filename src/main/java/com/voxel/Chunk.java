package com.voxel;

import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.voxel.enums.BlockType;

public class Chunk {
    public static final int W = 16;
    public static final int H = 64;
    public static final int D = 16;
    private static final BlockType[] values = BlockType.values();
    public final int chunkX;
    public final int chunkZ;
    public final byte[] chunk;
    public Mesh mesh;

    private static final float[][] FACES = {
        {
            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f 
        },
        {
             0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
        },
        {
            -0.5f, -0.5f, -0.5f,   
            -0.5f, -0.5f,  0.5f,   
            -0.5f,  0.5f,  0.5f,   
            -0.5f, -0.5f, -0.5f,   
            -0.5f,  0.5f,  0.5f,   
            -0.5f,  0.5f, -0.5f,   
        },
        {
             0.5f, -0.5f,  0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
        },
        {
            -0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
        },
        {
            -0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
        }
    };

    private static final int[][] NORMALS = {
        {0, 0, 1}, 
        {0, 0, -1}, 
        {-1, 0, 0},
        {1, 0, 0},
        {0, 1, 0},
        {0, -1, 0}
    };

    private static final float[][] FACE_COLORS = { {1,0,0}, {0,1,0}, {0,0,1}, {1,1,0}, {0,1,1}, {1,0,1} };

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        // Default value is 0000000 so AIR
        this.chunk = new byte[W*H*D];
    }
    
    private int index(int x, int y, int z) {
        int idx = (x) + (y * W) + (z * W * H);
        return idx;
    }

    public void flat() {
        for(int x = 0; x < W; x++) {
            for(int y = 0; y < 6; y++) {
                for(int z = 0; z < D; z++) {
                    this.setBlock(x,y,z, BlockType.STONE);
                }
            }
        }
    }

    public void render(int shaderId) {
        glBindVertexArray(mesh.VAO);
        // glDrawArrays(GL_TRIANGLES, 0, 3); // Says to draw 3 veritices, independent from a vertex having 3 positions

        Vector3f pos = new Vector3f(chunkX * W, 0, chunkZ * D);
        Matrix4f translate = new Matrix4f().translate(pos); // identity matrix then apply translation
                    
        Shader.setMatrix4(shaderId, translate);
        glDrawArrays(GL_TRIANGLES, 0, mesh.vertexCount);
    }

    public void buildMesh(World world) {
        float[] vertices = cull(world);
        this.mesh = new Mesh(vertices, null);
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



    public float[] cull(World world) {
        
        List<Float> survivors = new ArrayList<>();
    
        for(int x = 0; x < W; x++) {
            for(int y = 0; y < H; y++) {
                for(int z = 0; z < W; z++) {
                    if(getBlock(x, y, z) == BlockType.AIR) continue;
                    for(int f = 0; f < 6; f++) {
                        int[] n = NORMALS[f];
                        if(world.getBlock(chunkX*W + x + n[0], y + n[1], chunkZ*D + z + n[2]) != BlockType.AIR) continue;

                        float[] verts = FACES[f];
                        float[] colors = FACE_COLORS[f];

                        // 6 verticies for one face
                        for(int v = 0; v < 6; v++) {
                            survivors.add(verts[v*3] + x);
                            survivors.add(verts[v*3+1] + y);
                            survivors.add(verts[v*3+2] + z);
                            survivors.add(colors[0]); survivors.add(colors[1]); survivors.add(colors[2]);
                        }
                    }
                }
            }
        }
        return toFloatArray(survivors);
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
