package com.voxel;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import com.voxel.Window;

import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Shader {
    public int shaderId;
    public int model;

    public Shader(String vertexSrc, String fragmentSrc, Window window) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

        glShaderSource(vertexShader, vertexSrc);
        glCompileShader(vertexShader);

        int successVertexShader = glGetShaderi(vertexShader, GL_COMPILE_STATUS);
        if(successVertexShader == 0) {
            System.out.println(glGetShaderInfoLog(vertexShader));
        }

        glShaderSource(fragmentShader, fragmentSrc);
        glCompileShader(fragmentShader);

        int successFragmentShader = glGetShaderi(fragmentShader, GL_COMPILE_STATUS);
        if(successFragmentShader == 0) {
            System.out.println(glGetShaderInfoLog(fragmentShader));
        }

        this.shaderId = glCreateProgram();

        glAttachShader(shaderId, vertexShader);
        glAttachShader(shaderId, fragmentShader);
        glLinkProgram(shaderId);

        // Must be linked before we can get the uniform location
        this.model = glGetUniformLocation(shaderId, "model");
        //this.projection = glGetUniformLocation(shaderId, "projection");

        glUseProgram(shaderId); // Apply the view and projection, must set the shader

        // 45 degrees is the FOV
        // 0.1f is chosen as the near plane as zero would explode due to dividing by 0, 
        // causing us to make the view move the objects a few units back
        Matrix4f projectionTransform = new Matrix4f().perspective((float) Math.toRadians(45), (float) window.width/window.height, 0.1f, 100.0f);
        setMatrix4(glGetUniformLocation(shaderId, "projection"), projectionTransform);

        int successShaderLink = glGetProgrami(shaderId, GL_LINK_STATUS);
        if(successShaderLink == 0) {
            System.out.println(glGetProgramInfoLog(shaderId));
        }

        // We linked now, we done
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public static void setMatrix4(int location, Matrix4f matrix) {
        //int location = glGetUniformLocation(shaderId, name);
        try (var stack = stackPush()) {
            FloatBuffer buf = stack.mallocFloat(16);
            matrix.get(buf);
            glUniformMatrix4fv(location, false, buf);
        }
    }
}
