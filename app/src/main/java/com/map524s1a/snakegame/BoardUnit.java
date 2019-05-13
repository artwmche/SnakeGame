package com.map524s1a.snakegame;

public class BoardUnit {
    private int posX;
    private int posY;

    public BoardUnit(int posX,int posY){
        this.posX = posX;
        this.posY = posY;
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

    @Override
    public boolean equals(Object object)
    {
        boolean sameObject = false;

        if (object != null && object instanceof SnakeBody)
        {
            sameObject = this.getPosX() == ((SnakeBody) object).getPosX() && this.getPosY() == ((SnakeBody) object).getPosY();
        }

        return sameObject;
    }
}
