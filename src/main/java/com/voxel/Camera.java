package com.voxel;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;

import java.time.LocalDate;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.voxel.Shader;

public class Camera {

    private final float radius = 10.0f;
    public final float cameraSpeed = 100.0f;
    public float lastX = 400;
    public float lastY = 300;
    public float sensitivity = 0.1f;

    public Vector3f cameraPos;
    public Vector3f target;
    public Vector3f up;

    public float yaw = -90;
    public float pitch;
    public boolean firstMouse = true;

    public boolean fly = false;
    public boolean spaceWasDown = false;
    public double time = glfwGetTime();
    
    private int shaderId;
    
    public Camera(int shaderId) {
        this.shaderId = shaderId;
        this.cameraPos = new Vector3f(0, 15, 3);
        this.target = new Vector3f(0, 0, -1); // looking at origin
        this.up = new Vector3f(0, 1, 0); // where is up? normally its y-direction (direction vector)
    }

    public void rotateCamera(float time) {
        float camX = (float) Math.sin(time) * radius;
        float camZ = (float) Math.cos(time) * radius;

        cameraPos = new Vector3f(camX, 0.0f, camZ);

        Matrix4f cameraTransform = new Matrix4f().lookAt(cameraPos, target, up);

        Shader.setMatrix4(glGetUniformLocation(shaderId, "view"), cameraTransform);
    }

    public void moveCamera(float time) {
        // view = glm::lookAt(cameraPos, cameraPos + cameraFront, cameraUp);
        // We want the target to not be fixed, cameraPos converts the direction to a point to look
        Vector3f center = new Vector3f(cameraPos).add(target);
        Matrix4f cameraTransform = new Matrix4f().lookAt(cameraPos, center, up);
        Shader.setMatrix4(glGetUniformLocation(shaderId, "view"), cameraTransform);
    }

    public void keyInput(float deltaTime, long window) {
         // FMA is doing camera.cameraPos += (deltaTime * camera.cameraSpeed) * camera.target;
        // this = this + a * b;

        Vector3f cross = new Vector3f(target).cross(new Vector3f(up));
        Vector3f forward = new Vector3f(target.x, 0, target.z).normalize();


        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            cameraPos.fma(deltaTime * cameraSpeed, (fly ? target : forward));
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            cameraPos.fma(-(deltaTime * cameraSpeed), (fly ? target : forward));
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            Vector3f c = new Vector3f(cross);
            c.normalize();
            cameraPos.fma(deltaTime * cameraSpeed, c);
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            Vector3f c = new Vector3f(cross);
            c.normalize();
            cameraPos.fma(-(deltaTime * cameraSpeed), c);
        }

        boolean isDown = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;

        
        if(isDown && !spaceWasDown) {
            if(glfwGetTime() - 0.3f < time) {
                fly = !fly;
            }
            time = glfwGetTime();
        }
        spaceWasDown = isDown; // Tracking isDown
        
    }

    public void input(long _window) {
        glfwSetCursorPosCallback(_window, (window, xPos, yPos) -> {
            if (firstMouse) {
                lastX = (float) xPos;
                lastY = (float) yPos;
                firstMouse = false;
            }
            float xoffset = (float) xPos - lastX;
            float yoffset =  lastY - (float) yPos;

            lastX = (float) xPos;
            lastY = (float) yPos;

            xoffset *=  sensitivity;  
            yoffset *=  sensitivity;

            pitch += yoffset;
            yaw += xoffset;

            if( pitch > 89.0f) {
                 pitch = 89.0f;
            }
            if( pitch < -89.0f) {
                 pitch = -89.0f;
            }

            Vector3f newTarget = new Vector3f();
            float length = (float) Math.cos(Math.toRadians( pitch));

            newTarget.x = (float) Math.cos(Math.toRadians( yaw)) * length;
            newTarget.y = (float) Math.sin(Math.toRadians( pitch));
            newTarget.z = (float) Math.sin(Math.toRadians( yaw)) * length;
            newTarget.normalize();
            target = newTarget;
        });
    }
}
