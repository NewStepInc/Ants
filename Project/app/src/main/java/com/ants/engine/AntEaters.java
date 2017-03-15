package com.ants.engine;

import android.graphics.Canvas;

import com.ants.engine.base.AntEater;

import java.util.ArrayList;
import java.util.List;

public class AntEaters {
    public List<AntEater> listAntEaters = new ArrayList<>();

    public AntEaters(AntsEngine antsEngine) {
        float movement = AntsEngine.LEVEL_ANTEATER_MOVEMENT[antsEngine.mLevel - 1];
        for (int i = 0; i < AntsEngine.LEVEL_ANTEATER_COUNT[antsEngine.mLevel - 1]; i++) {
            AntEater antEater = new AntEater(antsEngine, movement, i);
            listAntEaters.add(antEater);
        }

    }

    public void draw(Canvas canvas) {
        for (int i = 0; i < listAntEaters.size(); i ++)
            listAntEaters.get(i).draw(canvas);
    }

    public void updateFrame() {
        for (int i = 0; i < listAntEaters.size(); i ++)
            listAntEaters.get(i).updateFrame();
    }
}
