package com.map524s1a.snakegame;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

/**
 * Created by Hossein on 7/26/2017.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class SnakeView extends SurfaceView implements SurfaceHolder.Callback {



    private static final String TAG = "CannonView"; // for logging errors

    // constants for game play
    public static final int MISS_PENALTY = 2; // seconds deducted on a miss
    public static final int HIT_REWARD = 3; // seconds added on a hit

    // constants for the Cannon
    public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;

    // constants for the Cannonball
    public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
    public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;

    // constants for the Targets
    public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
    public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
    public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
    public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
    public static final double TARGET_PIECES = 9;
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 4;

    // constants for the Blocker
    public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
    public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    public static final double BLOCKER_X_PERCENT = 1.0 / 2;
    public static final double BLOCKER_SPEED_PERCENT = 1.0;

    // text size 1/18 of screen width
    public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

    private SnakeThread snakeThread; // controls the game loop
    private Activity activity = null; // to display Game Over dialog in GUI thread
    private boolean dialogIsDisplayed = false;

    // game objects

    // dimension variables
    private int screenWidth;
    private int screenHeight;

    // variables for the game loop and tracking statistics
    private boolean gameOver; // is the game over?
    private double timeLeft; // time remaining in seconds
    private int shotsFired; // shots the user has fired
    private double totalElapsedTime; // elapsed seconds

    // constants and variables for managing sounds
    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // plays sound effects
    private SparseIntArray soundMap; // maps IDs to SoundPool

    // Paint variables used when drawing each item on the screen
    private Paint textPaint; // Paint used to draw text
    private Paint backgroundPaint; // Paint used to clear the drawing area

    private Snake snake = null;
    private boolean youLose = false;
    private boolean youWin = false;
    private Apple apple = null;
    private boolean appleHit = false;
    private int appleCount = 0;
    private boolean startNewGame = false;
    private int growSnakeCount = 0;
    private MotionEvent e;
    private int boxWidth = 0;
    private int boxHeight = 0;
    private final Handler myHandler = new Handler();
    private static int dialogCount = 0;

    // constructor
    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs); // call superclass constructor
        activity = (Activity) context; // store reference to MainActivity

        // register SurfaceHolder.Callback listener
        getHolder().addCallback(this);

        // configure audio attributes for game audio
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setUsage(AudioAttributes.USAGE_GAME);
        AudioAttributes audioAttributes = attrBuilder.build();

        // initialize SoundPool to play the app's three sound effects
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        // (1) SoundPool.Builder setAudioAttributes (AudioAttributes attributes)
        // Sets the AudioAttributes.
        // For examples, game applications will use attributes built with usage information set to USAGE_GAME.
        // (2) AudioAttributes build ()
        // Combines all of the attributes that have been set and return a new AudioAttributes object.
        builder.setAudioAttributes(audioAttributes);
        // (1) SoundPool build ()
        soundPool = builder.build();

        // create Map of sounds and pre-load sounds
        
        // SparseIntArrays map integers to integers.
        // Unlike a normal array of integers, there can be gaps in the indices.
        // (1) SparseIntArray (int initialCapacity)
        //    Creates a new SparseIntArray containing no mappings that will not require any additional memory allocation to store the specified number of mappings.
        //    If you supply an initial capacity of 0, the sparse array will be initialized with a light-weight representation not requiring any additional array allocations. 
        soundMap = new SparseIntArray(3); // create new SparseIntArray
        soundMap.put(TARGET_SOUND_ID,
                soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID,
                soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID,
                soundPool.load(context, R.raw.blocker_hit, 1));

        textPaint = new Paint();
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);


    }

    // called when the size of the SurfaceView changes,
    // such as when it's first added to the View hierarchy
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w; // store CannonView's width
        screenHeight = h; // store CannonView's height

        // configure text properties
        textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
//        textPaint.setAntiAlias(true); // smoothes the text
    }

    // get width of the game screen
    public int getScreenWidth() {
        return screenWidth;
    }

    // get height of the game screen
    public int getScreenHeight() {
        return screenHeight;
    }

    // plays a sound with the given soundId in soundMap
    public void playSound(int soundId) {
        soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    // reset all the screen elements and start a new game
    public void newGame() {

        //snake game flag
        startNewGame=true;
        dialogCount=0;
        snake=null;
        apple=null;
        appleCount=0;

        // construct a new Cannon


        if (gameOver) {
            System.out.println("I am at game over");// start a new game after the last game ended
            gameOver = false; // the game is not over
            snakeThread = new SnakeThread(getHolder()); // create thread
            snakeThread.start(); // start the game loop thread
        }

//        showSystemBars();
    }

    // hide system bars and app bar
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // show system bars and app bar
    private void showSystemBars() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    // called when surface is first created
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            newGame(); // set up and start a new game
            snakeThread = new SnakeThread(holder); // create thread
            snakeThread.setRunning(true); // start game running
            snakeThread.start(); // start the game loop thread
        }
    }

    // called when surface changes size
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format,
                               int width, int height) { }

    // called when the surface is destroyed
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // ensure that thread terminates properly
        boolean retry = true;
        snakeThread.setRunning(false); // terminate cannonThread

        while (retry) {
            try {
                snakeThread.join(); // wait for cannonThread to finish
                retry = false;
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted", e);
            }
        }
    }

    // Thread subclass to control the game loop
    private class SnakeThread extends Thread {
        private SurfaceHolder surfaceHolder; // for manipulating canvas
        private boolean threadIsRunning = true; // running by default

        // initializes the surface holder
        public SnakeThread(SurfaceHolder holder) {
            setSurfaceHolder(holder);
            setName("CannonThread");
        }

        // changes running state
        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        // controls the game loop
        @Override
        public void run() {
            Canvas canvas = null; // used for drawing
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    // get Canvas for exclusive drawing from this thread
                    canvas = getSurfaceHolder().lockCanvas();

                    // lock the surfaceHolder for drawing
                    synchronized(getSurfaceHolder()) {
                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsedTime += elapsedTimeMS / 1000.0;
                        updatePositions(elapsedTimeMS);
                        if(!gameOver) {// update game state
                            testForCollisions(); // test for GameElement collisions
                            drawGameElements(canvas); // draw using the canvas
                            previousFrameTime = currentTime;
                            Thread.sleep(100);// update previous time
                        }
                    }
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                } finally {
                    // display canvas's contents on the CannonView
                    // and enable other threads to use the Canvas
                    if (canvas != null)
                        getSurfaceHolder().unlockCanvasAndPost(canvas);
                }
            }
        }

        public SurfaceHolder getSurfaceHolder() {
            return surfaceHolder;
        }

        public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }
    }

    // called repeatedly by the CannonThread to update game elements
    private void updatePositions(double elapsedTimeMS) {
        //System.out.println("In update position");
        double interval = elapsedTimeMS / 1000.0; // convert to seconds

        if(snake != null && !gameOver) {
            if(growSnakeCount > 0 ){
                snake.growBody();
                growSnakeCount--;
            }
            moveSnake();
        }
        if(apple != null && appleHit) {
            youWin = apple.randomPlace(new Board(), snake);
            appleHit = false;
            growSnakeCount=3;
            appleCount++;
        }
        /*
        // update cannonball's position if it is on the screen
        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);

        blocker.update(interval); // update the blocker's position

        for (GameElement target : targets)
            target.update(interval); // update the target's position

        timeLeft -= interval; // subtract from time left

        // if the timer reached zero
        if (timeLeft <= 0) {
            timeLeft = 0.0;
            gameOver = true; // the game is over
            cannonThread.setRunning(false); // terminate thread
            showGameOverDialog(R.string.lose); // show the losing dialog
        }

        // if all pieces have been hit
        if (targets.isEmpty()) {
            cannonThread.setRunning(false); // terminate thread
            showGameOverDialog(R.string.win); // show winning dialog
            gameOver = true;
        }
        */
        if(youLose){
            snakeThread.setRunning(false);
            gameOver = true;
            youLose= false;
            //System.out.println("I am at you lose");
            showGameOverDialog(R.string.lose);

        }
        if(youWin){
            snakeThread.setRunning(false);
            gameOver = true;
            youWin= false;
            //System.out.println("I am at you win");
            showGameOverDialog(R.string.win);
        }
    }

    private void moveSnake(){
        if(snake.getDirectionX()>0){
            //System.out.println("I am at +x");
            SnakeBody head = snake.getBody().get(snake.getBody().size()-1);
            SnakeBody body = new SnakeBody(head.getPosX()+1,head.getPosY());
            snake.getBody().add(body);
            snake.getBody().remove(0);
        }
        else if(snake.getDirectionX()<0){
            //System.out.println("I am at -x");
            SnakeBody head = snake.getBody().get(snake.getBody().size()-1);
            SnakeBody body = new SnakeBody(head.getPosX()-1,head.getPosY());
            snake.getBody().add(body);
            snake.getBody().remove(0);

        }else if(snake.getDirectionY()>0){
            //System.out.println("I am at +Y");
            SnakeBody head = snake.getBody().get(snake.getBody().size()-1);
            SnakeBody body = new SnakeBody(head.getPosX(),head.getPosY()+1);
            snake.getBody().add(body);
            snake.getBody().remove(0);

        }else if(snake.getDirectionY()<0){
            //System.out.println("I am at -Y");
            SnakeBody head = snake.getBody().get(snake.getBody().size()-1);
            SnakeBody body = new SnakeBody(head.getPosX(),head.getPosY()-1);
            snake.getBody().add(body);
            snake.getBody().remove(0);

        }
    }
    // display an AlertDialog when the game ends
    private void showGameOverDialog(final int messageId) {
        //System.out.println("In ShowGameOverDialog");
        @SuppressLint("ValidFragment")

        // DialogFragment to display game stats and start new game

        final DialogFragment gameResult =
                new DialogFragment() {
                    // create an AlertDialog and return it
                    @Override
                    public Dialog onCreateDialog(Bundle bundle) {
                        // create dialog displaying String resource for messageId
                        //AlertDialog.Builder builder =
                        //        new AlertDialog.Builder(getActivity());
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(activity);
                        builder.setTitle(getResources().getString(messageId));

                        // display number of shots fired and total time elapsed
                        builder.setMessage(getResources().getString(
                                R.string.results_format, appleCount, totalElapsedTime));
                        builder.setPositiveButton(R.string.reset_game,
                                new DialogInterface.OnClickListener() {
                                    // called when "Reset Game" Button is pressed
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                            dialogIsDisplayed = false;
                                            newGame(); // set up and start a new game
                                            //dismiss();


                                    }
                                }
                        );

                        return builder.create(); // return the AlertDialog
                    }
                };

        // in GUI thread, use FragmentManager to display the DialogFragment
        activity.runOnUiThread(


                new Runnable() {
                    public void run() {
//                        showSystemBars();
                        dialogIsDisplayed = true;
                        gameResult.setCancelable(false);
                        // modal dialog
                        gameResult.show(activity.getFragmentManager(), "results");


                    }
                }
        );

        //gameResult.show(activity.getFragmentManager(), "results");
    }

    // checks if the ball collides with the Blocker or any of the Targets
    // and handles the collisions
    public void testForCollisions() {
        //System.out.println("In testForCollisions");
        // remove any of the targets that the Cannonball
        // collides with
        //get the snake head
        if(snake!=null) {
            SnakeBody head = snake.getBody().get(snake.getBody().size() - 1);
            //test for collision at the body
            if (snake.getBody().indexOf(head) != snake.getBody().size() - 1) {
                youLose = true;
            }
            //test for collision at the border (outside the grid)
            if (head.getPosY() < 0 || head.getPosY() > 100 || head.getPosX() < 0 || head.getPosX() > 100) {
                youLose = true;
            }
            if(apple!=null && apple.getPosX()==head.getPosX()&&apple.getPosY()==head.getPosY()){
                appleHit = true;
                playSound(TARGET_SOUND_ID);
            }
        }
        //test for collision at the apple

        /*

        if (cannon.getCannonball() != null &&
                cannon.getCannonball().isOnScreen()) {
            for (int n = 0; n < targets.size(); n++) {
                if (cannon.getCannonball().collidesWith(targets.get(n))) {
                    targets.get(n).playSound(); // play Target hit sound

                    // add hit rewards time to remaining time
                    timeLeft += targets.get(n).getHitReward();

                    cannon.removeCannonball(); // remove Cannonball from game
                    targets.remove(n); // remove the Target that was hit
                    --n; // ensures that we don't skip testing new target n
                    break;
                }
            }
        }
        else { // remove the Cannonball if it should not be on the screen
            cannon.removeCannonball();
        }

        // check if ball collides with blocker
        if (cannon.getCannonball() != null &&
                cannon.getCannonball().collidesWith(blocker)) {
            blocker.playSound(); // play Blocker hit sound

            // reverse ball direction
            cannon.getCannonball().reverseVelocityX();

            // deduct blocker's miss penalty from remaining time
            timeLeft -= blocker.getMissPenalty();
        }

        */
    }

    // draws the game to the given Canvas
    public void drawGameElements(Canvas canvas) {
        //System.out.println("In drawGameElements");
        // clear the background
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
                backgroundPaint);
        Paint boxPaint = new Paint();
        boxWidth = canvas.getWidth()/100;
        boxHeight = canvas.getHeight() / 100;
        //System.out.print("box width:"+ boxWidth);
        //System.out.print("box height:"+ boxheight);
        //System.out.print("canvas width:"+ canvas.getWidth());
        //System.out.println(" canvas height:"+canvas.getHeight());
        if(startNewGame) {
            snake = new Snake(30, 50);
            apple = new Apple(70, 50);
            startNewGame = false;
        }

        boxPaint.setColor(Color.GRAY);
        for(int i = 0; i < 100; i++){
            for(int j = 0 ; j < 100; j++){
                canvas.drawRect(boxWidth * i, boxHeight * j, (boxWidth*(i+1)-1) , (boxHeight*(j+1)-1),
                        boxPaint);
                //System.out.print("left:"+boxWidth * i);
                //System.out.print(" top:"+boxheight * j);
                //System.out.print(" right:"+ (boxWidth*(i+1)-1));
                //System.out.println(" bottom:"+(boxheight*(j+1)-1));
            }
        }


        Paint snakePaint = new Paint();
        snakePaint.setColor(Color.BLUE);
        for(int i = 0 ; i < snake.getBody().size(); i++){
            SnakeBody body = snake.getBody().get(i);
            canvas.drawRect(boxWidth * body.getPosX(), boxHeight * body.getPosY(), (boxWidth*(body.getPosX()+1)-1) , (boxHeight*(body.getPosY()+1)-1),
                    snakePaint);
        }
        playSound(CANNON_SOUND_ID);

        Paint applePaint = new Paint();
        applePaint.setColor(Color.RED);
        canvas.drawRect(boxWidth * apple.getPosX(), boxHeight * apple.getPosY(), (boxWidth*(apple.getPosX()+1)-1) , (boxHeight*(apple.getPosY()+1)-1),
                applePaint);



        /*
        // display time remaining
        canvas.drawText(getResources().getString(
                R.string.time_remaining_format, timeLeft), 50, 100, textPaint);

        cannon.draw(canvas); // draw the cannon

        // draw the GameElements
        if (cannon.getCannonball() != null &&
                cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);

        blocker.draw(canvas); // draw the blocker

        // draw all of the Targets
        for (GameElement target : targets)
            target.draw(canvas);
       */
    }

    // called when the user touches the screen in this activity
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        this.e = e;
        // get int representing the type of action which caused this event
        int action = e.getAction();
        //System.out.println("I am here "+ action);

        // the user touched the screen or dragged along the screen
        if (action == MotionEvent.ACTION_UP) {
            //System.out.println("I am here");
            // fire the cannonball toward the touch point
            //alignAndFireCannonball(e);
            int x = (int)e.getX()/boxWidth;
            int y = (int)e.getY()/boxHeight;
            //System.out.println("x:"+x+";y:"+y);


            SnakeBody head = snake.getBody().get(snake.getBody().size()-1);
            //System.out.println("x:"+head.getPosX()+";y:"+head.getPosY()+";snake");
            if(Math.abs(snake.getDirectionX())==1){

               //fix this logically
                if(y > head.getPosY()) {
                    snake.setDirectionX(0);
                    snake.setDirectionY(1);
                }
                else if(y < head.getPosY()){
                    snake.setDirectionX(0);
                    snake.setDirectionY(-1);
                }
            }else{
                if(x > head.getPosX()) {
                    snake.setDirectionX(1);
                    snake.setDirectionY(0);
                }
                else if(x < head.getPosX()){
                    snake.setDirectionX(-1);
                    snake.setDirectionY(0);
                }
            }

        }

        return true;
    }

    @Override
    public boolean performClick() {
        //System.out.println("hello");
        super.performClick();
        return true;
    }

    // aligns the barrel and fires a Cannonball if a Cannonball is not
    // already on the screen
    /*
    public void alignAndFireCannonball(MotionEvent event) {
        // get the location of the touch in this view
        Point touchPoint = new Point((int) event.getX(),
                (int) event.getY());

        // compute the touch's distance from center of the screen
        // on the y-axis
        double centerMinusY = (screenHeight / 2 - touchPoint.y);

        double angle = 0; // initialize angle to 0

        // calculate the angle the barrel makes with the horizontal
        angle = Math.atan2(touchPoint.x, centerMinusY);

        // point the barrel at the point where the screen was touched
        cannon.align(angle);

        // fire Cannonball if there is not already a Cannonball on screen
        if (cannon.getCannonball() == null ||
                !cannon.getCannonball().isOnScreen()) {
            cannon.fireCannonball();
            ++shotsFired;
        }
    }
    */
    // stops the game: called by CannonGameFragment's onPause method
    public void stopGame() {
        if (snakeThread != null)
            snakeThread.setRunning(false); // tell thread to terminate
    }

    // release resources: called by CannonGame's onDestroy method
    public void releaseResources() {
        soundPool.release(); // release all resources used by the SoundPool
        soundPool = null;
    }
}
