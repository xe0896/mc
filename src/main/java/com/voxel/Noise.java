package com.voxel;
import java.util.Random;

public class Noise {
    private final int[] permutation = new int[512];
    private static final int[][] vectors = {{-1, 1}, {1, -1}, {1, 1}, {-1, -1}};

    public Noise(long seed) {
        int[] p = new int[256];
        for(int k = 0; k < 256; k++) p[k] = k;

        Random rand = new Random(seed);
        for(int k = 255; k > 0; k--) {
            int r = rand.nextInt(k + 1);
            int tmp = p[k];
            p[k] = p[r];
            p[r] = tmp;
        }

        // We want to make this permutation array double the size so 
        // it handles wrap around, just a copy around it
        for(int k = 0; k < 512; k++) permutation[k] = p[k & 255];
    }

    // Lerp itself would be just a straight line which is not smooth, we use
    // fade before based on some smooth quintic to make it smooth
    private float lerp(float a, float b, float t) {
        return a + t * (b - a); // Linear interpolation, along t 
    }

    private float fade(float t) {
        return t*t*t*(t*(t*6-15)+10);
    }

    private float grad(int i, int j, float dx, float dz) {
        int hash = permutation[permutation[i & 255] + (j & 255)];
        
        int idx = hash & 3; // same as mod 4 to index our 4 vector list
        int[] v = vectors[idx];

        return v[0] * dx + v[1] * dz;
    }

    public int height(int wx, float SCALE, int wz, int AMPLITUDE, int BASE) {
        float v = noise(SCALE * wx, SCALE * wz);
        v = (v + 1) * 0.5f; // -1 -> 1 becomes 0 -> 1
        v = (float) Math.pow(v, 3); // Genuine peaks would survive but small peaks would be more flat
        return Math.max((int)(v * AMPLITUDE + BASE), 1);
    }

    public float noise(float nx, float nz) {
        int i = (int) Math.floor(nx);
        int j = (int) Math.floor(nz);
        float fx = nx - i; // Remove the integer part and only keep fractional
        float fz = nz - j;

        float d0 = grad(i, j, fx, fz);
        float d1 = grad(i + 1, j, fx - 1, fz);
        float d2 = grad(i, j + 1, fx, fz - 1);
        float d3 = grad(i + 1, j + 1, fx - 1, fz - 1);

        float u = fade(fx);
        float v = fade(fz);
    
        // Lerp along x twice since we need two horziontal lines to find the x
        float n0 = lerp(d0, d1, u);
        float n1 = lerp(d2, d3, u);
        
        // Then z once on the lerped x to get the correct value
        return lerp(n0, n1, v);
    }
}
