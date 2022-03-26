package ItemBridgeX.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.distribution.BufferedItemBridge.*;
import mindustry.world.blocks.distribution.ItemBridge.*;
import mindustry.world.blocks.distribution.Junction.*;

import static mindustry.Vars.*;

// Code mainly from MI2
public class Renderer{
    static QuadTree<Tile> tiles;

    public static void load(){
        tiles = new QuadTree<>(Tmp.r1.set(0, 0, world.unitWidth(), world.unitHeight()));

        world.tiles.eachTile(tile -> tiles.insert(tile));
    }

    public static void draw(){
        Rect bounds = Core.camera.bounds(Tmp.r1).grow(tilesize);

        Draw.z(Layer.power + 1f);

        tiles.intersect(bounds, tile -> {
            Building building = tile.build;
            if(building == null || !tile.isCenter()) return;

            if(building instanceof ItemBridgeBuild bridge){
                drawBridgeItem(bridge);
            }else if(building instanceof JunctionBuild junction){
                drawHiddenItem(junction);
            }
        });

        Draw.reset();
    }

    private static void getBuffer(ItemBuffer itemBuffer, Item[] returnItems, float[] returnTimes){
        long[] buffer = Reflect.get(itemBuffer, "buffer");
        int index = Reflect.get(itemBuffer, "index");

        for(int i = 0; i < index; i++){
            long l = buffer[i];
            returnItems[i] = content.item(TimeItem.item(l));
            returnTimes[i] = TimeItem.time(l);
        }
    }

    public static void drawBridgeItem(ItemBridgeBuild bridge){
        // Draw each item the bridge have
        Draw.color(Color.white, 0.8f);
        int amount = 0;
        for(int iid = 0; iid < bridge.items.length(); iid++){
            if(bridge.items.get(iid) > 0){
                for(int itemid = 1; itemid <= bridge.items.get(iid); itemid ++){
                    Draw.rect(content.item(iid).uiIcon, bridge.x, bridge.y - tilesize/2f + 1f + 0.6f * (float)amount, 4f, 4f);
                    amount++;
                }
            }
        }

        if(bridge instanceof BufferedItemBridgeBuild bufferedBridge){
            drawHiddenItem(bufferedBridge);
        }
    }

    public static void drawHiddenItem(BufferedItemBridgeBuild bridge){
        ItemBuffer buffer = Reflect.get(bridge, "buffer");

        BufferedItemBridge block = (BufferedItemBridge)bridge.block;
        int capacity = block.bufferCapacity;

        Item[] bufferItems = new Item[capacity];
        float[] bufferTimes = new float[capacity];

        getBuffer(buffer, bufferItems, bufferTimes);

        Tile other = world.tile(bridge.link);

        float begx, begy, endx, endy;
        if(!block.linkValid(bridge.tile, other)){
            begx = bridge.x - tilesize / 2f;
            begy = bridge.y - tilesize / 2f;
            endx = bridge.x + tilesize / 2f;
            endy = bridge.y - tilesize / 2f;
        }else{
            int i = bridge.tile.absoluteRelativeTo(other.x, other.y);
            float ex = other.worldx() - bridge.x - Geometry.d4(i).x * tilesize / 2f,
            ey = other.worldy() - bridge.y - Geometry.d4(i).y * tilesize / 2f;
            float warmup = state.isEditor() ? 1f : bridge.warmup;
            ex *= warmup;
            ey *= warmup;

            begx = bridge.x + Geometry.d4(i).x * tilesize / 2f;
            begy = bridge.y + Geometry.d4(i).y * tilesize / 2f;
            endx = bridge.x + ex;
            endy = bridge.y + ey;
        }

        for(int i = 0; i < capacity; i++){
            if(bufferItems[i] != null){
                float f = Math.min(((Time.time - bufferTimes[i]) * bridge.timeScale / block.speed) * capacity, capacity - i - 1) / (float)capacity;
                Draw.rect(bufferItems[i].uiIcon,
                begx + (endx - begx) * f,
                begy + (endy - begy) * f,
                4f, 4f);
            }
        }
    }

    private static void getDirectionalBuffer(DirectionalItemBuffer buffer, Item[][] items, float[][] times){
        for(int i = 0; i < 4; i++){
            for(int ii = 0; ii < buffer.indexes.length; ii++){
                long l = buffer.buffers[i][ii];
                items[i][ii] = content.item(BufferItem.item(l));
                times[i][ii] = BufferItem.time(l);
            }
        }
    }

    public static void drawHiddenItem(JunctionBuild junction){
        DirectionalItemBuffer buffer = junction.buffer;
        Junction block = (Junction)junction.block;
        int capacity = block.capacity;

        Item[][] items = new Item[4][capacity];
        float[][] times = new float[4][capacity];
        getDirectionalBuffer(buffer, items, times);

        float begx, begy, endx, endy;
        for(int i = 0; i < 4; i++){
            endx = junction.x + Geometry.d4(i).x * tilesize / 2f + Geometry.d4(Math.floorMod(i + 1, 4)).x * tilesize / 4f;
            endy = junction.y + Geometry.d4(i).y * tilesize / 2f + Geometry.d4(Math.floorMod(i + 1, 4)).y * tilesize / 4f;
            begx = junction.x - Geometry.d4(i).x * tilesize / 4f + Geometry.d4(Math.floorMod(i + 1, 4)).x * tilesize / 4f;
            begy = junction.y - Geometry.d4(i).y * tilesize / 4f + Geometry.d4(Math.floorMod(i + 1, 4)).y * tilesize / 4f;

            if(buffer.indexes[i] > 0){
                for(int idi = 0; idi < buffer.indexes[i]; idi++){
                    if(items[i][idi] != null){
                        float f = Math.min(((Time.time - times[i][idi]) * junction.timeScale / block.speed) * capacity, capacity - idi - 1) / (float)capacity;
                        Draw.rect(items[i][idi].uiIcon,
                        begx + (endx - begx) * f,
                        begy + (endy - begy) * f,
                        4f, 4f);
                    }
                }
            }
        }
    }
}
