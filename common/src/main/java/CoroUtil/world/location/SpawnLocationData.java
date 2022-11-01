package CoroUtil.world.location;

import CoroUtil.util.BlockCoord;

import java.util.UUID;

public class SpawnLocationData {

    public BlockCoord coords;
    public String type;
    public UUID entityUUID;

    public SpawnLocationData(BlockCoord parCoords, String parType) {
        coords = parCoords;
        type = parType;
    }
}
