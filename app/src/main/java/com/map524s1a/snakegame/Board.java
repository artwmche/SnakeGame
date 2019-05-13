package com.map524s1a.snakegame;

import java.util.ArrayList;

public class Board{

    private ArrayList<BoardUnit> units = new ArrayList<BoardUnit>();

    public Board(){
        for(int i = 0 ; i < 100 ; i++){
            for(int j = 0 ; j < 100; j++){
                BoardUnit aUnit = new BoardUnit(i,j);
                getUnits().add(aUnit);
            }
        }
    }


    public ArrayList<BoardUnit> getUnits() {
        return units;
    }

    public void setUnits(ArrayList<BoardUnit> units) {
        this.units = units;
    }
}
