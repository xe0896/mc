package com.voxel;

import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import java.nio.IntBuffer;

public class Window {
    public long window;
    public int width;
    public int height;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
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

        window = glfwCreateWindow(width, height, title, NULL, NULL);
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
    }

    public void setTitle(String title) {
        glfwSetWindowTitle(window, title);
    }
}
