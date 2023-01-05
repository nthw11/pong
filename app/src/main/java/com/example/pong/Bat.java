package com.example.pong;

import android.graphics.RectF;

public class Bat {

    // These are the member variables (fields)
    // they all have the m prefix
    // they are all private because direct access is not required
    private RectF mRect;
    private float mLength;
    private float mXCoord;
    private float mBatSpeed;
    private int mScreenX;

    // These variables are public and final
    // they can be directly accessed by the instance (in PongGame)
    // because they are part of the same package but cannot be changed
    final int STOPPED = 0;
    final int LEFT = 1;
    final int RIGHT = 2;

    // keeps trac of if and how the bat is moving
    // starting with STOPPED condition
    private int mBatMoving = STOPPED;

    public Bat(int sx, int sy){

        // Bat needs to know the screen horiz resolution outside of this method
        mScreenX = sx;

        // Configure the size of the bat based on the screen resolution
        // One eighth of the screen width
        mLength = mScreenX / 8;
        // One fortieth of the screen height
        float height = sy / 40;

        // Configure the starting location of the bat
        // roughly the middle horizontally
        mXCoord = mScreenX / 2;

        // The height of the bat off the bottom of the screen
        float mYCoord = sy - height;

        // initialize mRect based on the size and position
        mRect = new RectF(mXCoord, mYCoord, mYCoord + mLength, mYCoord + height);

        // Configure the speed of the bat
        // this code means the bat can cover the width of the screen in 1 second
        mBatSpeed = mScreenX;
    }

    // Return a reference to the mRect object
    RectF getRect(){
        return mRect;
    }

    // Update the movement state passed in by the onTouchEvent method
    void setMovementState(int state){
        mBatMoving = state;
    }

    // Update the bat - called each frame/loop
    void update(long fps){

        // Move the bat based on the mBatMoving variable and the speed of the prev frame
        if(mBatMoving == LEFT){
            mXCoord = mXCoord - mBatSpeed / fps;
        }
        if(mBatMoving == RIGHT){
            mXCoord = mXCoord + mBatSpeed / fps;
        }

        // Stop the bat from going off screen
        if(mXCoord < 0){
            mXCoord = 0;
        }
        else if(mXCoord + mLength > mScreenX){
            mXCoord = mScreenX - mLength;
        }

        // Update mRect based on the results from the previous code in update
        mRect.left = mXCoord;
        mRect.right = mXCoord + mLength;
    }
}
