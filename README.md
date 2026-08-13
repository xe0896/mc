# Voxel Engine

A Minecraft-style voxel engine built from scratch in Java + LWJGL (OpenGL).
The point isn't the game — it's the **data structures** that make an infinite
block world possible.

## Run it

```bash
cd ~/Documents/voxel-engine
mvn compile exec:exec
```

First run downloads LWJGL (~a few MB). A sky-blue window appears. `ESC` closes it.

> macOS note: the window MUST launch via `exec:exec`, not `exec:java`.
> GLFW has to run on the process's first thread (`-XstartOnFirstThread`),
> which only a fresh JVM can do. The pom is already set up for this.

## The build order (each step is runnable)

| # | Milestone | What you learn | Key data structure |
|---|-----------|----------------|--------------------|
| 1 | **Window + game loop** ✅ | how a game is structured | the render/tick loop |
| 2 | Draw one cube | VAOs, shaders, the GPU | vertex/index buffers |
| 3 | Camera you can fly | matrices, projection | 3D math |
| 4 | A `Chunk` of 16³ blocks | flyweight blocks | `short[]` id array + registry |
| 5 | Only draw visible faces | why voxel games are fast | face culling |
| 6 | Many chunks around player | "infinite" world | `HashMap<ChunkPos, Chunk>` |
| 7 | Terrain generation | procedural worlds | Perlin/Simplex noise |
| 8 | Light spreading | flood fill | **BFS** with a queue |
| 9 | Place / break blocks | interaction | raycast into the grid |
| 10 | Save / load world | persistence | serialization + region files |

## Architecture (where the DS live)

- **Block** — one shared object per type (flyweight). Blocks in the world are
  stored as `short` ids, never as objects. Millions of blocks stay cheap.
- **Chunk** — a 16×16×16 (or ×256) box of block ids in a flat array.
  `index = x + y*16 + z*256`.
- **World** — a `HashMap<ChunkPos, Chunk>`. Load chunks near the player,
  unload far ones. This is what makes the world "infinite".
- **Game loop** — logic ticks at a fixed rate; rendering runs as fast as the GPU allows.

## Package layout (planned)

```
com.voxel
├─ Main.java          game loop + window
├─ render/            shaders, meshes, camera, GPU stuff
├─ world/
│  ├─ Block.java      flyweight block types
│  ├─ Chunk.java      block-id array
│  ├─ World.java      HashMap of chunks
│  └─ ChunkPos.java   map key (record)
└─ gen/               noise-based terrain generation
```
