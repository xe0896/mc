package com.voxel;

import java.nio.ByteBuffer;

public record Block(ByteBuffer buf, int width, int height, int nrChannels) {}
