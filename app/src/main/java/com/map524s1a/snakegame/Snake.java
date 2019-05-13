package com.map524s1a.snakegame;

import java.util.ArrayList;

public class Snake {

    //head is the end of the arrayList, tail is the beginning of the arrayList
    private ArrayList<SnakeBody> body;
    private int directionX;
    private int directionY;

    public Snake(int posX,int poxY) {
        setBody(new ArrayList<SnakeBody>());
        for(int i = 0 ; i < 3 ;i++){
            getBody().add(new SnakeBody(posX+i, poxY));
        }
        setDirectionX(1);
        setDirectionY(0);
    }


    public ArrayList<SnakeBody> getBody() {
        return body;
    }

    public void setBody(ArrayList<SnakeBody> body) {
        this.body = body;
    }

    public int getDirectionX() {
        return directionX;
    }

    public void setDirectionX(int directionX) {
        this.directionX = directionX;
    }

    public int getDirectionY() {
        return directionY;
    }

    public void setDirectionY(int directionY) {
        this.directionY = directionY;
    }

    public void growBody(){
        SnakeBody unit = body.get(0);
        body.add(0,unit);
    }
}
