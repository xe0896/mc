package com.voxel;

import static org.lwjgl.opengl.GL20.glGetUniformLocation;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.voxel.Shader;

public class Camera {

    private final float radius = 10.0f;
    public final float cameraSpeed = 5.0f;
    public float lastX = 400;
    public float lastY = 300;
    public float sensitivity = 0.1f;

    public Vector3f cameraPos;
    public Vector3f target;
    public Vector3f up;

    public float yaw = -90;
    public float pitch;
    public boolean firstMouse = true;
    
    private int shaderId;
    
    public Camera(int shaderId) {
        this.shaderId = shaderId;
        this.cameraPos = new Vector3f(0, 0, 3);
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
}
