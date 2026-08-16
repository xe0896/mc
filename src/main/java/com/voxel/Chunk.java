package com.voxel;

import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;

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
    private Vector3f pos;
    public final byte[] chunk;
    public Mesh mesh;

    // Index order must match NORMALS below: 0=+Z, 1=-Z, 2=-X, 3=+X, 4=+Y, 5=-Y
    private static final float[][] FACES = {
        // 0: +Z FRONT (all z = +0.5)
        {
            -0.5f, -0.5f,  0.5f,
             0.5f, -0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f 
        },
        // 1: -Z BACK (all z = -0.5)
        {
             0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
        },
        // 2: -X LEFT (all x = -0.5)
        {
            -0.5f, -0.5f, -0.5f,
            -0.5f, -0.5f,  0.5f,
            -0.5f,  0.5f,  0.5f,
            -0.5f,  0.5f, -0.5f,
        },
        // 3: +X RIGHT (all x = +0.5)
        {
             0.5f, -0.5f,  0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f,  0.5f, -0.5f,
             0.5f,  0.5f,  0.5f,
        },
        // 4: +Y TOP (all y = +0.5)
        {
            -0.5f,  0.5f,  0.5f,
             0.5f,  0.5f,  0.5f,
             0.5f,  0.5f, -0.5f,
            -0.5f,  0.5f, -0.5f,
        },
        // 5: -Y BOTTOM (all y = -0.5)
        {
            -0.5f, -0.5f,  0.5f,
            -0.5f, -0.5f, -0.5f,
             0.5f, -0.5f, -0.5f,
             0.5f, -0.5f,  0.5f,
        }
    };

    // Direction to step to find each face's neighbour. Same index order as FACES.
    private static final int[][] NORMALS = {
        {0, 0, 1},   // 0: +Z FRONT
        {0, 0, -1},  // 1: -Z BACK
        {-1, 0, 0},  // 2: -X LEFT
        {1, 0, 0},   // 3: +X RIGHT
        {0, 1, 0},   // 4: +Y TOP
        {0, -1, 0}   // 5: -Y BOTTOM
    };

    private static final float[][] FACE_COLORS = { {0,0,0}, {0.625f,0.625f,0.625f}, {0,1,0}, {0.4f,0.2f,0} };

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        pos = new Vector3f(chunkX * W, 0, chunkZ * D);
        // Default value is 0000000 so AIR
        this.chunk = new byte[W*H*D];
    }
    
    private int index(int x, int y, int z) {
        int idx = (x) + (y * W) + (z * W * H);
        return idx;
    }

    public void flat() {
        for(int x = 0; x < W; x++) {
            for(int y = 0; y < 10; y++) {
                for(int z = 0; z < D; z++) {
                    if(y == 9) {
                        this.setBlock(x,y,z, BlockType.GRASS);
                    } else if (y < 7) {
                        this.setBlock(x,y,z, BlockType.STONE);
                    } else {
                        this.setBlock(x,y,z, BlockType.DIRT);
                    }
                }
            }
        }
    }

    public void render(int shaderId) {
        glBindVertexArray(mesh.VAO);
        // glDrawArrays(GL_TRIANGLES, 0, 3); // Says to draw 3 veritices, independent from a vertex having 3 positions

        Matrix4f translate = new Matrix4f().translate(pos); // identity matrix then apply translation
                    
        Shader.setMatrix4(shaderId, translate);
        glDrawElements(GL_TRIANGLES, mesh.indicesCount, GL_UNSIGNED_INT, 0);
    }

    public void buildMesh(World world) {
        MeshData data = cull(world);
        this.mesh = new Mesh(data.vertices(), data.indices());
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

        System.out.println(enumInt);

        int idx = this.index(x, y, z);

        this.chunk[idx] = (byte) enumInt;
    }

    public MeshData cull(World world) {
        
        List<Float> survivors = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int faceCount = 0;
    
        for(int x = 0; x < W; x++) {
            for(int y = 0; y < H; y++) {
                for(int z = 0; z < W; z++) {
                    BlockType type = getBlock(x, y, z);
                    if(type == BlockType.AIR) continue;
                    for(int f = 0; f < 6; f++) {
                        int[] n = NORMALS[f];
                        //if(getBlock(x + n[0], y + n[1], z + n[2]) != BlockType.AIR) continue;
                        if(world.getBlock(chunkX*W + x + n[0], y + n[1], chunkZ*D + z + n[2]) != BlockType.AIR) continue;

                        

                        float[] verts = FACES[f];
                        float[] colors = FACE_COLORS[type.ordinal()];

                        // 6 verticies for one face
                        for(int v = 0; v < 4; v++) {
                            // one vertex, 3 for one triangle, 6 for two triangle to make a face
                            survivors.add(verts[v*3] + x);
                            survivors.add(verts[v*3+1] + y);
                            survivors.add(verts[v*3+2] + z);
                            survivors.add(colors[0]); survivors.add(colors[1]); survivors.add(colors[2]);
                        }
                        int base = faceCount * 4;
                        indices.add(base + 0);
                        indices.add(base + 1);
                        indices.add(base + 2);
                        indices.add(base + 0);
                        indices.add(base + 2);
                        indices.add(base + 3);
                        faceCount++;
                    }
                }
            }
        }
        return new MeshData(toFloatArray(survivors), toIntArray(indices));
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
