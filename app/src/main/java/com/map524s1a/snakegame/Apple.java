package com.map524s1a.snakegame;

import java.util.ArrayList;
import java.util.Random;

public class Apple {

    private int posX;
    private int posY;

    public Apple(int posX,int posY){
        this.setPosX(posX);
        this.setPosY(posY);
    }

    public boolean randomPlace(Board aBoard,Snake aSnake){
        //board - snakebody
        ArrayList<BoardUnit> unitList = aBoard.getUnits();
        ArrayList<SnakeBody> bodyList = aSnake.getBody();
        for (SnakeBody unit: bodyList) {
            unitList.remove(unit);
        }
        System.out.println(unitList.size());
        Random rand = new Random();
        if(unitList.size()>0) {
            int nextCoord = rand.nextInt(unitList.size());

            BoardUnit aUnit = unitList.get(nextCoord);
            this.posX = aUnit.getPosX();
            this.posY = aUnit.getPosY();
            return false;
        }
        else{
            return true;
        }

    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }
}
