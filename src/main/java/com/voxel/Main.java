package com.voxel;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.joml.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;


import com.voxel.Shader;

public class Main {
    // The window "handle" GLFW gives us a long id, not an object. Flyweight-ish:
    // the real window lives in native memory, we just hold a pointer to it

    private Window window;
    private Shader shader;
    private Mesh mesh;

    public void run() {
        init(1280, 720);
        loop();

        // Free the window and GLFW when we're done.
        glfwDestroyWindow(window.window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init(int width, int height) {
        this.window = new Window(width, height, "Voxel");
        glfwMakeContextCurrent(window.window);
        // Wires LWJGL's OpenGL bindings to the current context. Must come after
        // glfwMakeContextCurrent. Nothing OpenGL works before this line.
        GL.createCapabilities();
        glfwSwapInterval(1); // v-sync: cap to monitor refresh rate

        float vertices[] = {
            // positions         // colors
            -0.5f, -0.5f, 0.5f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.5f, 0.0f, 1.0f, 0.0f,
             0.5f,  0.5f, 0.5f, 0.0f, 0.0f, 1.0f,
            -0.5f, 0.5f, 0.5f, 1.0f, 0.0f, 0.0f,

            -0.5f, -0.5f, -0.5f, 1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
             0.5f,  0.5f, -0.5f, 0.0f, 0.0f, 1.0f,
            -0.5f, 0.5f, -0.5f, 1.0f, 0.0f, 0.0f
        };

        int indices[] = {  // note that we start from 0!
            0, 1, 3,   // first triangle
            1, 2, 3,    // second triangle
            
            4, 5, 7,
            7, 6, 5,

            4, 7, 0,
            0, 3, 7,

            5, 1, 6,
            6, 2, 1,

            4, 5, 0,
            0, 1, 5,

            7, 6, 3,
            3, 2, 6
        };

        // location = 0 is like a conveyer belt saying I live here
        // gl_Position is the final vertex position, the code below is saying
        // output as stored but we need to convert to vec4 to get homogenous coordinates
        // for easier transformations
        String vertexShaderSource = """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                layout (location = 1) in vec3 aColor;

                out vec3 outColor;
                uniform mat4 model;
                uniform mat4 view;
                uniform mat4 projection;

                void main()
                {
                    gl_Position = projection * view * model * vec4(aPos, 1.0);
                    outColor = aColor;
                }
                """;

        // Orange fragment shader
        String fragmentShaderSource = """
                #version 330 core
                out vec4 FragColor;

                in vec3 outColor;

                void main() {
                    FragColor = vec4(outColor, 1.0);
                }
                """;

        this.shader = new Shader(vertexShaderSource, fragmentShaderSource, this.window);
        this.mesh = new Mesh(vertices, indices);

        glfwShowWindow(window.window);
    }

    private void loop() {
        // The "clear color" — the colour the screen is wiped to each frame.
        // Sky blue, so it already feels a little Minecraft-y.
        glClearColor(0.45f, 0.62f, 0.92f, 1.0f);
        // glPolygonMode(GL_FRONT_AND_BACK, GL_LINE); wireframe mode
        glEnable(GL_DEPTH_TEST);

        // Game loop
        while (!glfwWindowShouldClose(window.window)) {
            // Game logic goes here later (ticks, physics, chunks).
            // for now just wipe the screen to the clear color.
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glUseProgram(shader.shaderId);
             // The time is the angle
            Matrix4f transform = new Matrix4f().rotate((float) glfwGetTime(),0.5f, 1, 0);
            try(var stack = stackPush()) {
                FloatBuffer buf = stack.mallocFloat(16);
                transform.get(buf);
                glUniformMatrix4fv(shader.model, false, buf);
            }

            glBindVertexArray(mesh.VAO);
            // glDrawArrays(GL_TRIANGLES, 0, 3); // Says to draw 3 veritices, independent from a vertex having 3 positions
            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);

        
            // Swap the buffer we drew to onto the screen (double buffering)
            glfwSwapBuffers(window.window);

            // Handle window/keyboard/mouse events
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        // The reason why we create an instance of Main is because the function run() calls init() and
        // loop(), which also use fields meaning if we was to make run() static then we would get a compile
        // error as we cannot use fields from a static context
        new Main().run();
    }
}
