package com.voxel;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector3f;
import com.voxel.enums.BlockType;
public class Player {
    public Camera camera;
    private World world;

    private static final float GRAVITY = 9.8f;
    private static final float PLAYER_HEIGHT = 2;
    public float velocityY = 0;

    public boolean fly = false;
    public boolean spaceWasDown = false;
    public double doubleTapFlyTime = glfwGetTime();
    public double jumpTime = glfwGetTime();

    public float jumpCooldown = 1.5f;
    public float jumpSpeed = 5;
    public float doubleTapFlyCd = 0.3f;

    public Player(int shaderId, World world) {
        //this.position = position;
        this.world = world;
        this.camera = new Camera(shaderId);
    }

    public Vector3f position() {
        return new Vector3f(camera.cameraPos.x, camera.cameraPos.y - PLAYER_HEIGHT, camera.cameraPos.z);
    }

    private boolean solidAt(float wx, float wy, float wz) {
        int x = (int) Math.floor(wx);
        int y = (int) Math.floor(wy);
        int z = (int) Math.floor(wz);

        BlockType type = world.getBlock(x, y, z);
        return type != BlockType.AIR;
    }

    private boolean isGrounded() {
        float feet = camera.cameraPos.y - PLAYER_HEIGHT;
        return solidAt(camera.cameraPos.x, feet - 0.05f, camera.cameraPos.z);
    }

    // Fixing an error state, different to isGrounded() as that is just a check
    // if there is an actual block underneath
    private boolean collides() {
        Vector3f cameraPos = camera.cameraPos;
        float feet = camera.cameraPos.y - PLAYER_HEIGHT;
        return solidAt(cameraPos.x, feet, cameraPos.z) || solidAt(cameraPos.x, cameraPos.y, cameraPos.z);
    }

    public void apply(float deltaTime, long window, float time) {
        // Several collides() calls as we want to use the new positions as we go along
        boolean isDown = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;
        
        if(isDown && !spaceWasDown) {
            if(glfwGetTime() - doubleTapFlyCd < doubleTapFlyTime) {
                fly = !fly;
            }
            doubleTapFlyTime = glfwGetTime();
        }
        
        spaceWasDown = isDown; // Tracking isDown

        Vector3f delta = camera.keyInput(deltaTime, window, fly, isDown);

        float oldX = camera.cameraPos.x;
        camera.cameraPos.x += delta.x;
        if(collides()) camera.cameraPos.x = oldX;

        float oldZ = camera.cameraPos.z;
        camera.cameraPos.z += delta.z;
        if(collides()) camera.cameraPos.z = oldZ;

        if(isGrounded() && !fly && isDown) {
            if(glfwGetTime() - jumpCooldown > jumpTime) {
                velocityY = jumpSpeed;
                jumpTime = glfwGetTime();
            }
        }

        // Apply gravity, double deltaTime as we integrate twice, one to update velocity; another for position
        if(!fly) {
            if(!isGrounded()) {
                if(position().y > -10) {
                    velocityY -= GRAVITY * deltaTime;
                } else {
                    velocityY = 0;
                    camera.cameraPos.y = 200;
                }
            }
            camera.cameraPos.y += velocityY * deltaTime;
        }

        
        if(collides() && !fly) {
            int by = (int) Math.floor(position().y);
            camera.cameraPos.y = by + 1 + PLAYER_HEIGHT;
            velocityY = 0;
        }

        float oldY = camera.cameraPos.y;
        camera.cameraPos.y += delta.y;
        if(collides()) camera.cameraPos.y = oldY;

        camera.moveCamera(time);
    }

    public void windowToPlayer(Window window) {
        Vector3f p = position();
        String title = String.format("x: %.1f  y: %.1f  z: %.1f", p.x, p.y, p.z);
        window.setTitle(title);
    }
}
