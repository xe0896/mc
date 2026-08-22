package com.voxel;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import java.nio.ByteBuffer;

public class Mesh {
    public int VAO;
    public int vertexCount;
    public int indicesCount;

    public Mesh(float[] vertices, int[] indices, Block block) {
        this.vertexCount = vertices.length / 6;
        this.indicesCount = indices.length;
        this.VAO = glGenVertexArrays();

        glBindVertexArray(this.VAO); // Tracks EBO and VBO

        // Create a textureID for the blocks
        int textureID = glGenTextures();

        // Reserves an un-used buffer ID, this is like a reserving spot for the GPU, allowing
        // to store certain information there so that we send it once and the GPU can constantly
        // draw very fast
        int VBO = glGenBuffers();
        int EBO = glGenBuffers();
        // Binds the ID to the GL_ARRAY_BUFFER (only one it is fully owned by this)
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glBindTexture(GL_TEXTURE_2D, textureID); // Currently binded for textures

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);	
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Whatever is at GL_ARRAY_BUFFER gets vertices inserted to and it is VBO
        /* GL_STREAM_DRAW: the data is set only once and used by the GPU at most a few times.
        GL_STATIC_DRAW: the data is set only once and used many times.
        GL_DYNAMIC_DRAW: the data is changed a lot and used many times. */

        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // Provides the data to VBO
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindVertexArray(VAO); // VAO must read the next two lines to store it

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, block.width(), block.height(), 0, GL_RGB, GL_UNSIGNED_BYTE, block.buf());
        glGenerateMipmap(GL_TEXTURE_2D);

        // 1st param is the location we defined in the location in the GLSL code
        // 2nd param is the number of values per vertex, 3 points so 3
        // The rest is self-explantory, the last one is just the pointer at the beginning of the buffer
        glVertexAttribPointer(0, 3, GL_FLOAT, false, Float.BYTES * 6, 0);
        glEnableVertexAttribArray(0); // location=0
        glVertexAttribPointer(1, 3, GL_FLOAT, false, Float.BYTES * 6, Float.BYTES * 3);
        glEnableVertexAttribArray(1); // location=1
        glVertexAttribPointer(2, 2, GL_FLOAT, false, Float.BYTES * 8, Float.BYTES * 6);
        glEnableVertexAttribArray(2); // location=2

        // Binding to 0 would just unbind VBO to GL_ARRAY_BUFFER, we need to unbind after AttribPointer
        // so that the data stored in GL_ARRAY_BUw manFFER which is VBO is being read and it has access to it
        // when drawing

        glBindVertexArray(0); // Must unbind first so that the unbinds below are still recorded and binded to the VAO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
