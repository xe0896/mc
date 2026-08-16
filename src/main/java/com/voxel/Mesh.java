package com.voxel;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Mesh {
    public int VAO;
    public int vertexCount;

    public Mesh(float[] vertices, int[] indices) {
        this.vertexCount = vertices.length / 6;
        this.VAO = glGenVertexArrays();
        glBindVertexArray(this.VAO); // Tracks EBO and VBO

        // Reserves an un-used buffer ID, this is like a reserving spot for the GPU, allowing
        // to store certain information there so that we send it once and the GPU can constantly
        // draw very fast
        int VBO = glGenBuffers();
        int EBO = glGenBuffers();
        // Binds the ID to the GL_ARRAY_BUFFER (only one it is fully owned by this)
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);

        // Whatever is at GL_ARRAY_BUFFER gets vertices inserted to and it is VBO
        /* GL_STREAM_DRAW: the data is set only once and used by the GPU at most a few times.
        GL_STATIC_DRAW: the data is set only once and used many times.
        GL_DYNAMIC_DRAW: the data is changed a lot and used many times. */

        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // Provides the data to VBO
        //glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindVertexArray(VAO); // VAO must read the next two lines to store it

        // 1st param is the location we defined in the location in the GLSL code
        // 2nd param is the number of values per vertex, 3 points so 3
        // The rest is self-explantory, the last one is just the pointer at the beginning of the buffer
        glVertexAttribPointer(0, 3, GL_FLOAT, false, Float.BYTES * 6, 0);
        glEnableVertexAttribArray(0); // location=0
        glVertexAttribPointer(1, 3, GL_FLOAT, false, Float.BYTES * 6, Float.BYTES * 3);
        glEnableVertexAttribArray(1); // location=1

        // Binding to 0 would just unbind VBO to GL_ARRAY_BUFFER, we need to unbind after AttribPointer
        // so that the data stored in GL_ARRAY_BUFFER which is VBO is being read and it has access to it
        // when drawing

        glBindVertexArray(0); // Must unbind first so that the unbinds below are still recorded and binded to the VAO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
}
