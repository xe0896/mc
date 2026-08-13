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

public class Main {

    // The window "handle" GLFW gives us a long id, not an object. Flyweight-ish:
    // the real window lives in native memory, we just hold a pointer to it
    private long window;

    private final int width = 1280;
    private final int height = 720;
    private int VAO;
    private int shaderProgram;

    public void run() {
        init();
        loop();

        // Free the window and GLFW when we're done.
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Print GLFW errors to System.err
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure the window BEFORE creating it.
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // stay hidden until ready
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // macOS needs an explicit modern OpenGL "core profile" context.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Voxel", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // ESC closes the window. This is a callback — GLFW calls our code on a key event.
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true);
            }
        });

        // Center the window on the primary monitor, the idea behind this is that
        // we create a stack that we want to free later on that is why we put
        // it in a try statement, we then allocate some integers that act like pointers
        // so then glfwGetWindowSize can write to them, these value would likely match
        // the width and height we provided but provide the honest truth as it may of been
        // altered due to clamping, we then set the window position to be centered given
        // the width and height of the monitor
        try (var stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            var vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(window,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2);
            }
        }

        glfwMakeContextCurrent(window);
        // Wires LWJGL's OpenGL bindings to the current context. Must come after
        // glfwMakeContextCurrent. Nothing OpenGL works before this line.
        GL.createCapabilities();
        glfwSwapInterval(1); // v-sync: cap to monitor refresh rate

        float vertices[] = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.0f,  0.5f, 0.0f
        };

        // VERTEX SHADER
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);

        // location = 0 is like a conveyer belt saying
        String vertexShaderSource = """
                #version 330 core
                layout (location = 0) in vec3 aPos;

                void main()
                {
                    gl_Position = vec4(aPos, 1.0);
                }
                """;

        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);

        int successVertexShader = glGetShaderi(vertexShader, GL_COMPILE_STATUS);
        if(successVertexShader == 0) {
            System.out.println(glGetShaderInfoLog(vertexShader));
        }

        // FRAGMENT SHADER
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

        // Orange fragment shader
        String fragmentShaderSource = """
                #version 330 core
                out vec4 FragColor;

                void main() {
                    FragColor = vec4(1.0f, 0.5f, 0.2f, 1.0f);
                }
                """;

        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);

        int successFragmentShader = glGetShaderi(fragmentShader, GL_COMPILE_STATUS);
        if(successFragmentShader == 0) {
            System.out.println(glGetShaderInfoLog(fragmentShader));
        }

        // SHADER PROGRAM
        shaderProgram = glCreateProgram();

        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        int successShaderLink = glGetProgrami(shaderProgram, GL_COMPILE_STATUS);
        if(successShaderLink == 0) {
            System.out.println(glGetProgramInfoLog(shaderProgram));
        }

        // We linked now, we done
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        VAO = glGenVertexArrays();
        glBindVertexArray(VAO);

        // Reserves an un-used buffer ID, this is like a reserving spot for the GPU, allowing
        // to store certain information there so that we send it once and the GPU can constantly
        // draw very fast
        int VBO = glGenBuffers();
        // Binds the ID to the GL_ARRAY_BUFFER (only one it is fully owned by this)
        glBindBuffer(GL_ARRAY_BUFFER, VBO);

        // Whatever is at GL_ARRAY_BUFFER gets vertices inserted to and it is VBO
        /* GL_STREAM_DRAW: the data is set only once and used by the GPU at most a few times.
        GL_STATIC_DRAW: the data is set only once and used many times.
        GL_DYNAMIC_DRAW: the data is changed a lot and used many times. */

        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // Provides the data to VBO

        glBindVertexArray(VAO); // VAO must read the next two lines to store it

        // 1st param is the location we defined in the location in the GLSL code
        // 2nd param is the number of values per vertex, 3 points so 3
        // The rest is self-explantory, the last one is just the pointer at the beginning of the buffer
        glVertexAttribPointer(0, 3, GL_FLOAT, false, Float.BYTES * 3, 0);
        glEnableVertexAttribArray(0); // location=0

        // Binding to 0 would just unbind VBO to GL_ARRAY_BUFFER, we need to unbind after AttribPointer
        // so that the data stored in GL_ARRAY_BUFFER which is VBO is being read and it has access to it
        // when drawing

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glfwShowWindow(window);
    }

    private void loop() {
        // The "clear color" — the colour the screen is wiped to each frame.
        // Sky blue, so it already feels a little Minecraft-y.
        glClearColor(0.45f, 0.62f, 0.92f, 1.0f);

        // Game loop
        while (!glfwWindowShouldClose(window)) {
            // Game logic goes here later (ticks, physics, chunks).
            // for now just wipe the screen to the clear color.
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glUseProgram(shaderProgram);
            glBindVertexArray(VAO);
            glDrawArrays(GL_TRIANGLES, 0, 3); // Says to draw 3 veritices, independent from a vertex having 3 positions

            // Swap the buffer we drew to onto the screen (double buffering)
            glfwSwapBuffers(window);

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
