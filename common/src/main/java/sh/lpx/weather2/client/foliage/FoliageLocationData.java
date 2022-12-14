package sh.lpx.weather2.client.foliage;

import sh.lpx.weather2.client.render.foliage.Foliage;

import java.util.ArrayList;
import java.util.List;

public class FoliageLocationData {

    public FoliageReplacerBase foliageReplacer;
    public List<Foliage> listFoliage = new ArrayList<>();

    public FoliageLocationData(FoliageReplacerBase foliageReplacer) {
        this.foliageReplacer = foliageReplacer;
    }
}
