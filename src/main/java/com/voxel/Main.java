package com.voxel;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.joml.Vector3f;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import java.util.List;
import java.util.ArrayList;

import com.voxel.Shader;
import com.voxel.enums.BlockType;
import com.voxel.Chunk;
import com.voxel.Camera;
import com.voxel.World; 

public class Main {
    // The window "handle" GLFW gives us a long id, not an object. Flyweight-ish:
    // the real window lives in native memory, we just hold a pointer to it

    private Window window;
    private Shader shader;
    private Camera camera;
    private World world;

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

        /*
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
        */

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

        this.world = new World(5);
        this.shader = new Shader(vertexShaderSource, fragmentShaderSource, this.window);
        this.camera = new Camera(shader.shaderId);

        glfwShowWindow(window.window);
    }

    private void loop() {
        // The "clear color" — the colour the screen is wiped to each frame.
        // Sky blue, so it already feels a little Minecraft-y.
        glClearColor(0.45f, 0.62f, 0.92f, 1.0f);
        // glPolygonMode(GL_FRONT_AND_BACK, GL_LINE); wireframe mode
        glEnable(GL_DEPTH_TEST);

        float deltaTime = 0.0f;
        float lastFrame = 0.0f;

        glfwSetCursorPosCallback(window.window, (window, xPos, yPos) -> {
            if (camera.firstMouse) {
                camera.lastX = (float) xPos;
                camera.lastY = (float) yPos;
                camera.firstMouse = false;
            }
            float xoffset = (float) xPos - camera.lastX;
            float yoffset = camera.lastY - (float) yPos;

            camera.lastX = (float) xPos;
            camera.lastY = (float) yPos;

            xoffset *= camera.sensitivity;  
            yoffset *= camera.sensitivity;

            camera.pitch += yoffset;
            camera.yaw += xoffset;

            if(camera.pitch > 89.0f) {
                camera.pitch = 89.0f;
            }
            if(camera.pitch < -89.0f) {
                camera.pitch = -89.0f;
            }

            Vector3f newTarget = new Vector3f();
            float length = (float) Math.cos(Math.toRadians(camera.pitch));

            newTarget.x = (float) Math.cos(Math.toRadians(camera.yaw)) * length;
            newTarget.y = (float) Math.sin(Math.toRadians(camera.pitch));
            newTarget.z = (float) Math.sin(Math.toRadians(camera.yaw)) * length;
            newTarget.normalize();
            camera.target = newTarget;
        });

        glfwSetInputMode(window.window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        
        // Game loop
        while (!glfwWindowShouldClose(window.window)) {
            // Game logic goes here later (ticks, physics, chunks).
            // for now just wipe the screen to the clear color.
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glUseProgram(shader.shaderId);

             // The time is the angle
            float time = (float) glfwGetTime();

            // deltaTime would be the time for one frame, so for a high FPS monitor like 144fps vs 60fps
            // 1/60 = 0.0167 and 1/144 = 0.00694 then cameraPos doing * 5 would make each 0.0835 and 0.0347 
            // respectively then for a 60fps monitor it would do this 60 times a second so 0.0835 x 60 = 5
            // and for a 144fps monitor it would do this 144 times a second so 0.0347x144 = 5, so its all equal
            deltaTime = time - lastFrame;
            lastFrame = time;

            // FMA is doing camera.cameraPos += (deltaTime * camera.cameraSpeed) * camera.target;
            // this = this + a * b;
            if (glfwGetKey(window.window, GLFW_KEY_W) == GLFW_PRESS) {
                camera.cameraPos.fma(deltaTime * camera.cameraSpeed, camera.target);
            }
            if (glfwGetKey(window.window, GLFW_KEY_S) == GLFW_PRESS) {
                camera.cameraPos.fma(-(deltaTime * camera.cameraSpeed), camera.target);
            }
            if (glfwGetKey(window.window, GLFW_KEY_D) == GLFW_PRESS) {
                Vector3f target = new Vector3f(camera.target);
                Vector3f up = new Vector3f(camera.up);
                Vector3f cross = target.cross(up);
                cross.normalize();
                camera.cameraPos.fma(deltaTime * camera.cameraSpeed, cross);
            }

            if (glfwGetKey(window.window, GLFW_KEY_A) == GLFW_PRESS) {
                Vector3f target = new Vector3f(camera.target);
                Vector3f up = new Vector3f(camera.up);
                Vector3f cross = target.cross(up);
                cross.normalize();
                camera.cameraPos.fma(-(deltaTime * camera.cameraSpeed), cross);
            }

            world.render(shader.model);

            /*
            for(int x = 0; x < chunk.W; x++) {
                for(int y = 0; y < chunk.H; y++) {
                    for(int z = 0; z < chunk.W; z++) {
                        if(chunk.getBlock(x, y, z) == BlockType.STONE) {
                            Vector3f position = new Vector3f(x, y, z);
                            //Matrix4f model = new Matrix4f().translate(position).rotate(time, 0.5f, 1, 0);
                            Matrix4f model = new Matrix4f().translate(position);
                            Shader.setMatrix4(shader.model, model);
                            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
                        }
                    }
                }
            }
            */

            camera.moveCamera(time);
        
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
