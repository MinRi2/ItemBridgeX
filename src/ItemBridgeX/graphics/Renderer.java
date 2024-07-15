package ItemBridgeX.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.distribution.BufferedItemBridge.*;
import mindustry.world.blocks.distribution.DirectionBridge.*;
import mindustry.world.blocks.distribution.ItemBridge.*;
import mindustry.world.blocks.distribution.Junction.*;

import java.lang.reflect.*;
import java.util.*;

import static arc.math.geom.Geometry.*;
import static mindustry.Vars.*;

public class Renderer{
    private static Field itemBridgeBufferField, bufferField, indexField;

    private static Item[] resultItems = new Item[8];
    private static float[] resultTimes = new float[8];

    private static Seq<Block> bridgeBlocks;
    private static Seq<Block> junctionBlocks;
    private static Seq<Block> directionBridgeBlocks;

    public static void init(){
        try{
            itemBridgeBufferField = BufferedItemBridgeBuild.class.getDeclaredField("buffer");
            bufferField = ItemBuffer.class.getDeclaredField("buffer");
            indexField = ItemBuffer.class.getDeclaredField("index");

            itemBridgeBufferField.setAccessible(true);
            bufferField.setAccessible(true);
            indexField.setAccessible(true);
        }catch(NoSuchFieldException e){
            throw new RuntimeException(e);
        }

        bridgeBlocks = Vars.content.blocks().select(b -> b instanceof ItemBridge);
        junctionBlocks = Vars.content.blocks().select(b -> b instanceof Junction);
        directionBridgeBlocks = content.blocks().select(b -> b instanceof DirectionBridge);
    }

    public static void draw(){
        Rect bounds = Core.camera.bounds(Tmp.r1).grow(tilesize);

        Draw.z(Layer.power + 1f);

        for(TeamData data : state.teams.present){
            for(Block block : bridgeBlocks){
                for(Building building : data.getBuildings(block)){
                    if(!bounds.contains(building.x, building.y)) continue;

                    drawItems(building);
                    drawBridgeItem((ItemBridgeBuild)building);
                }
            }

            for(Block block : junctionBlocks){
                for(Building building : data.getBuildings(block)){
                    if(!bounds.contains(building.x, building.y)) continue;

                    drawHiddenItem((JunctionBuild)building);
                }
            }

            for(Block block : directionBridgeBlocks){
                for(Building building : data.getBuildings(block)){
                    if(!bounds.contains(building.x, building.y)) continue;

                    drawItems(building);
                }
            }
        }

        Draw.reset();
    }

    // 缩放太大 消耗变大
//    public static void draw0(){
//        Rect bounds = Core.camera.bounds(Tmp.r1).grow(tilesize);
//
//        Draw.z(Layer.power + 1f);
//
//        for(TeamData data : state.teams.present){
//            QuadTree<Building> tree = data.buildingTree;
//
//            if(tree == null) continue;
//
//            tree.intersect(bounds, building -> {
//                if(building instanceof ItemBridgeBuild bridge){
//                    drawBridgeItem(bridge);
//                }else if(building instanceof JunctionBuild junction){
//                    drawHiddenItem(junction);
//                }else if(building instanceof DirectionBridgeBuild directionBridge){
//                    drawItems(directionBridge);
//                }
//            });
//        }
//
//        Draw.reset();
//    }

    private static void ensureCapacity(int requireCapacity){
        if(requireCapacity > resultItems.length) {
            Item[] newItems = new Item[requireCapacity];
            System.arraycopy(resultItems, 0, newItems, 0, resultTimes.length);
            resultItems = newItems;
        }

        if(requireCapacity > resultTimes.length){
            float[] newTimes = new float[requireCapacity];
            System.arraycopy(resultTimes, 0, newTimes, 0, resultTimes.length);
            resultTimes = newTimes;
        }
    }

    private static void getBuffer(ItemBuffer itemBuffer){
        long[] buffer = Reflect.get(itemBuffer, bufferField);
        int index = Reflect.get(itemBuffer, indexField);

        int capacity = buffer.length;

        Arrays.fill(resultItems, null);
        Arrays.fill(resultTimes, 0);

        ensureCapacity(capacity);

        for(int i = 0; i < index; i++){
            long l = buffer[i];

            resultItems[i] = content.item(TimeItem.item(l));
            resultTimes[i] = TimeItem.time(l);
        }
    }

    private static void getDirectionalBuffer(DirectionalItemBuffer buffer){
        Arrays.fill(resultItems, null);
        Arrays.fill(resultTimes, 0);

        int rotations = buffer.buffers.length;
        int capacity = buffer.buffers[0].length;
        int totalCapacity = rotations * capacity;

        ensureCapacity(totalCapacity);

        for(int i = 0; i < rotations; i++){
            for(int j = 0; j < buffer.indexes[i]; j++){
                long l = buffer.buffers[i][j];
                int index = i * capacity + j;

                resultItems[index] = content.item(BufferItem.item(l));
                resultTimes[index] = (BufferItem.time(l));
            }
        }
    }

    public static void drawItems(Building building){
        if(building.items == null) return;

        final int[] count = {0};
        building.items.each((item, amount) -> {
            for(int i = 0; i < amount; i++){
                Draw.rect(item.uiIcon, building.x, building.y - tilesize / 2f + 1f + 0.6f * count[0], 4f, 4f);
                count[0]++;
            }
        });
    }

    public static void drawBridgeItem(ItemBridgeBuild bridge){
        // Draw each item the bridge have.
        Draw.color(Color.white, 0.8f);

        drawItems(bridge);

        if(bridge instanceof BufferedItemBridgeBuild bufferedBridge){
            drawHiddenItem(bufferedBridge);
        }
    }

    public static void drawHiddenItem(BufferedItemBridgeBuild bridge){
        ItemBuffer buffer = Reflect.get(bridge, itemBridgeBufferField);

        BufferedItemBridge block = (BufferedItemBridge)bridge.block;
        int capacity = block.bufferCapacity;

        getBuffer(buffer);

        // Times empty check?
        if(!Structs.contains(resultItems, Objects::nonNull)){
            return;
        }

        Tile other = world.tile(bridge.link);

        float x = bridge.x, y = bridge.y;
        float begX, begY, endX, endY;
        if(!block.linkValid(bridge.tile, other)){
            begX = x - tilesize / 2f;
            begY = y - tilesize / 2f;
            endX = x + tilesize / 2f;
            endY = y - tilesize / 2f;
        }else{
            int bridgeOtherRot = bridge.relativeTo(other);
            int otherBridgeRot = Math.floorMod(bridgeOtherRot + 2, 4);

            float progress = state.isEditor() ? 1f : bridge.warmup;

            begX = x + d4x(bridgeOtherRot) * tilesize / 2f;
            begY = y + d4y(bridgeOtherRot) * tilesize / 2f;
            endX = other.worldx() + d4x(otherBridgeRot) * tilesize / 2f;
            endY = other.worldy() + d4y(otherBridgeRot) * tilesize / 2f;

            endX = Mathf.lerp(begX, endX, progress);
            endY = Mathf.lerp(begY, endY, progress);
        }

        for(int i = 0; i < capacity; i++){
            Item item = resultItems[i];
            float time = resultTimes[i];

            if(item == null) continue;

            float progress = Math.min(((Time.time - time) * bridge.timeScale() / block.speed) * capacity, capacity - i - 1) / (float)capacity;
            float itemX = Mathf.lerp(begX, endX, progress);
            float itemY = Mathf.lerp(begY, endY, progress);

            Draw.rect(item.uiIcon, itemX, itemY, 4f, 4f);
        }
    }

    public static void drawHiddenItem(JunctionBuild junction){
        DirectionalItemBuffer buffer = junction.buffer;
        Junction block = (Junction)junction.block;

        int rotations = buffer.indexes.length;
        int capacity = block.capacity;

        getDirectionalBuffer(buffer);

        // Times empty check?
        if(!Structs.contains(resultItems, Objects::nonNull)){
            return;
        }

        for(int from = 0; from < rotations; from++){
            if(buffer.indexes[from] <= 0) continue;

            int to = Math.floorMod(from + 1, 4);

            float begX = junction.x - d4x(from) * tilesize / 4f + d4x(to) * tilesize / 4f;
            float begY = junction.y - d4y(from) * tilesize / 4f + d4y(to) * tilesize / 4f;
            float endX = junction.x + d4x(from) * tilesize / 2f + d4x(to) * tilesize / 4f;
            float endY = junction.y + d4y(from) * tilesize / 2f + d4y(to) * tilesize / 4f;

            for(int j = 0; j < capacity; j++){
                int index = from * capacity + j;
                Item item = resultItems[index];
                float time = resultTimes[index];

                if(item == null) continue;

                float progress = Math.min(((Time.time - time) * junction.timeScale() / block.speed) * capacity, capacity - j - 1) / (float)capacity;
                float itemX = Mathf.lerp(begX, endX, progress);
                float itemY = Mathf.lerp(begY, endY, progress);
                Draw.rect(item.uiIcon, itemX, itemY, 4f, 4f);
            }
        }
    }
}
