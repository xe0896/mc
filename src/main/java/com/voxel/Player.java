package com.voxel;

import com.voxel.Camera;
import org.joml.Vector3f;

public class Player {
    public Camera camera;
    private Vector3f position;
    private Vector3f velocity;

    private static final float GRAVITY = 9.8f;
    private static final float PLAYER_HEIGHT = 0.9f;
    public int velocityY = 0;

    public Player(int shaderId) {
        //this.position = position;
        this.camera = new Camera(shaderId);
    }

    public Vector3f position() {
        return new Vector3f(camera.cameraPos.x, camera.cameraPos.y - PLAYER_HEIGHT, camera.cameraPos.z);
    }

    public void apply(float deltaTime, float time) {
        // Apply gravity, double deltaTime as we integrate twice, one to update velocity; another for position
        if(position().y >=  10) {
            velocityY -= GRAVITY * deltaTime;
            camera.cameraPos.y += velocityY * deltaTime;
        }
        camera.moveCamera(time);
    }

    public void windowToPlayer(Window window) {
        Vector3f p = position();
        String title = String.format("x: %.1f  y: %.1f  z: %.1f", p.x, p.y, p.z);
        window.setTitle(title);
    }
}
