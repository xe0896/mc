package com.voxel;

import org.joml.Vector3f;
import com.voxel.enums.BlockType;
public class Player {
    public Camera camera;
    private World world;
    private Vector3f position;
    private Vector3f velocity;

    private static final float GRAVITY = 9.8f;
    private static final float PLAYER_HEIGHT = 1.2f;
    private final boolean onGround = false;

    public float velocityY = 0;

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
        return solidAt(camera.cameraPos.x, feet - 1, camera.cameraPos.z);
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
        Vector3f delta = camera.keyInput(deltaTime, window);

        float oldX = camera.cameraPos.x;
        camera.cameraPos.x += delta.x;
        if(collides()) camera.cameraPos.x = oldX;

        float oldZ = camera.cameraPos.z;
        camera.cameraPos.z += delta.z;
        if(collides()) camera.cameraPos.z = oldZ;

        // Apply gravity, double deltaTime as we integrate twice, one to update velocity; another for position
        if(!camera.fly && !isGrounded()) {
            if(position().y >= 10) {
                velocityY -= GRAVITY * deltaTime;
                camera.cameraPos.y += velocityY * deltaTime;
            }
        }
        
        if(collides() && !camera.fly) {
            int by = (int) Math.floor(position().y);
            camera.cameraPos.y = by + 1 + PLAYER_HEIGHT;
            velocityY = 0;
            System.out.println("Not airbourne");
        } else {
            System.out.println("Airbourne");
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
