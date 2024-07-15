package ItemBridgeX;

import ItemBridgeX.graphics.*;
import arc.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

public class ItemBridgeX extends Mod{

    public ItemBridgeX(){
        Events.on(EventType.ClientLoadEvent.class, e -> Renderer.init());
        Events.run(Trigger.draw, Renderer::draw);
    }
}
