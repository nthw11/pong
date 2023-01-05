package com.example.pong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Build;
import java.io.IOException;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class PongGame extends SurfaceView implements Runnable {
    // are we debugging?
    private final boolean DEBUGGING = true;
    // these objects are needed to do the drawing
    private SurfaceHolder mOurHolder;
    private Canvas mCanvas;
    private Paint mPaint;

    // how many frames per second did we get?
    private long mFPS;
    // the number of milliseconds in a second
    private final int MILLIS_IN_SECOND = 1000;

    // holds the resolution of the screen
    private int mScreenX;
    private int mScreenY;
    // how big will the text be?
    private int mFontSize;
    private int mFontMargin;

    // the game objects
    private Bat mBat;
    private Ball mBall;

    // the current score and lives remaining
    private int mScore;
    private int mLives;

    // Here is the Thread and two control variables
    private Thread mGameThread = null;
    // This volatile variable can be accessed from inside and outside the thread
    private volatile boolean mPlaying;
    private boolean mPaused = true;

    // ALl these are for playing sounds
    private SoundPool mSP;
    private int mBeepID = -1;
    private int mBoopID = -1;
    private int mBopID = -1;
    private int mMissID = -1;

    // The PongGame constructor
    // called when this line:
    // mPongGame = new PongGame(this, size.x, size.y;
    // is executed from PongActivity
    public PongGame(Context context, int x, int y){
        // Super... calls the parent class
        // constructor of SurfaceView
        // provided by Android
        super(context);

        // Initialize these two members/fields with the
        // values passed in as parameters
        mScreenX = x;
        mScreenY = y;

        // font is 5% of screen width
        mFontSize = mScreenX / 20;
        // margin is 1.5% of screen width
        mFontMargin = mScreenX / 75;

        // initialize the objects ready for drawing with
        // getHolder is a method of SurfaceView
        mOurHolder = getHolder();
        mPaint = new Paint();

        // initialize the bat and ball
        mBall = new Ball(mScreenX);
        mBat = new Bat(mScreenX, mScreenY);

        // Prepare the SoundPool instance
        // Depending on the version of Android
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mSP = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            mSP = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }

        // Open each of the sound files in turn and load them into RAM ready to play
        // The try-catch blocks handle when this fails and is required.

        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;
            descriptor = assetManager.openFd("beep.ogg");
            mBeepID = mSP.load(descriptor, 0);
            descriptor = assetManager.openFd("boop.ogg");
            mBoopID = mSP.load(descriptor, 0);
            descriptor = assetManager.openFd("bop.ogg");
            mBopID = mSP.load(descriptor, 0);
            descriptor = assetManager.openFd("miss.ogg");
            mMissID = mSP.load(descriptor, 0);
        } catch(IOException e){
            Log.d("error", "failed to load sound files");
        }
        // everything is ready so start the game
        startNewGame();
    }
    // the player has just lost or is starting their first game
    private void startNewGame(){
        // put the ball back in starting position
        mBall.reset(mScreenX, mScreenY);

        // reset the score and the player's chances
        mScore = 0;
        mLives = 3;
    }
    // When we start the thread with:
    // mGameThread.start();
    // the run method is continuously called by Android because we implemented the Runnable interface
    // Calling mGameThread.join();
    // will stop the thread
    @Override
    public void run(){
        // mPlaying gives us finer control rather than just relying on the calls to run
        // mPlaying must be true AND the thread running for the main loop to execute
        while (mPlaying){
            // What time is it now at the start of the loop?
            long frameStartTime = System.currentTimeMillis();

            //Provided the game isn't paused call the update method
            if(!mPaused){
                update();
                // now the bat and ball are in their new positions
                // we can see if there have been any collisions
                detectCollisions();
            }

            // The movement has been handled and collisions detected
            // Now we can draw the scene
            draw();

            // How long did this frame/loop take?
            // Store the answer in timeThisFrame
            long timeThisFrame = System.currentTimeMillis() - frameStartTime;

            // Make sure timeThisFrame is at least 1 millisecond
            // because accidentally dividing by zero crashes the game
            if(timeThisFrame > 0){
                // Store the current frame rate in mFPS ready to pass to the update
                // methods of mBat and mBall next frame/loop
                mFPS = MILLIS_IN_SECOND / timeThisFrame;
            }
        }

    }

    private void update(){
        // Update the bat and the ball
        mBall.update(mFPS);
        mBat.update(mFPS);
    }

    private void detectCollisions(){
        // Has the bat hit the ball?

        if(RectF.intersects(mBat.getRect(), mBall.getRect())){
            // Realistic-ish bounce
            mBall.batBounce(mBat.getRect());
            mBall.increaseVelocity();
            mScore++;
            mSP.play(mBeepID, 1, 1, 0, 0, 1);
        }

        // Has the ball hit the edge of the screen?

        // Bottom
        if(mBall.getRect().bottom > mScreenY){
            mBall.reverseYVelocity();
            mLives--;
            mSP.play(mMissID, 1, 1, 0, 0, 1);
            if(mLives == 0){
                mPaused = true;
                startNewGame();
            }
        }

        // Top
        if(mBall.getRect().top < 0){
            mBall.reverseYVelocity();
            mSP.play(mBoopID, 1, 1, 0 , 0 , 1);
        }

        // Left
        if(mBall.getRect().left < 0){
            mBall.reverseXVelocity();
            mSP.play(mBopID, 1 ,1 , 0, 0, 1);
        }

        // Right
        if(mBall.getRect().right < mScreenX){
            mBall.reverseXVelocity();
            mSP.play(mBopID, 1, 1, 0, 0, 1);
        }

    }
    // draw the game objects and the HUD
    private void draw(){
        if(mOurHolder.getSurface().isValid()){
            // lock the canvas (graphics memory) ready to draw
            mCanvas = mOurHolder.lockCanvas();

            // fill the screen with a solid color
            mCanvas.drawColor(Color.argb(255,26,128,182));

            // chose a color to paint with
            mPaint.setColor(Color.argb(255,255, 255, 255));

            // draw the bat and ball
            mCanvas.drawRect(mBall.getRect(), mPaint);
            mCanvas.drawRect(mBat.getRect(), mPaint);

            // choose the font size
            mPaint.setTextSize(mFontSize);

            // draw the hud
            mCanvas.drawText("Score: " + mScore + " | Lives: ", mFontMargin, mFontSize, mPaint);

            if(DEBUGGING){
                printDebuggingText();
            }

            // display the drawing on screen
            // unlockCanvasAndPost is a method of SurfaceView
            mOurHolder.unlockCanvasAndPost(mCanvas);
        }
    }
    private void printDebuggingText(){
        int debugSize = mFontSize / 2;
        int debugStart = 150;
        mPaint.setTextSize(debugSize);
        mCanvas.drawText("FPS: " + mFPS, 10, debugStart + debugSize, mPaint);
    }
    // This method is called by PongActivity when the player quits the game
    public void pause(){
        // Set mPlaying to false
        // Stopping the thread isn't always instant
        mPlaying = false;
        try {
            // Stop the thread
            mGameThread.join();
        } catch (InterruptedException e) {
            Log.e("Error:", "joining thread");
        }
    }
    // This method is called by PongActivity when the player starts the game
    public void resume() {
        mPlaying = true;
        // Initialize the instance of Thread
        mGameThread = new Thread(this);

        // Start the thread
        mGameThread.start();
    }

    // Handle all the screen touches
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        // This switch block replaces the if statement from the Sub Hunter game
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK){
            // The player has put their finger on the screen
            case MotionEvent.ACTION_DOWN:
                // If the game was paused unpause
                mPaused = false;

                // where did the touch happen
                if(motionEvent.getX() > mScreenX / 2){
                    // on the right hand side
                    mBat.setMovementState(mBat.RIGHT);
                }
                else {
                    // on the left hand side
                    mBat.setMovementState(mBat.LEFT);
                }
                break;
                // the player lifted their finger from anywhere on the screen
            // it is possible to create bugs by using multiple fingers
            // we will use more complicated and robust touch handling in later projects
            case MotionEvent.ACTION_UP:
                // stop the bat moving
                mBat.setMovementState(mBat.STOPPED);
                break;
        }
        return true;
    }
}
