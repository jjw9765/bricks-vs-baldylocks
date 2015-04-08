//BRICKS VS BALDYLOCKS
//FINAL PROJECT
//JOHN WATSON 5/21/14

package com.watson.john.app;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import java.util.Timer;
import java.util.TimerTask;
import android.view.OrientationEventListener;
import android.hardware.SensorManager;

public class MainActivityView extends View
{
   // constant for accessing the high score in SharedPreference
   private static final String HIGH_SCORE = "HIGH_SCORE";
   private SharedPreferences preferences; // stores the high score

   // variables for managing the game
   private int score; // current score
   private long animationTime; //how long the bricks go from right to left
   private boolean gameIsOver; // whether the game has ended
   private boolean gamePaused; // whether the game has ended
   private boolean dialogDisplayed; // whether the game has ended
   private int highScore; // the game's all time high score
   
   // collections of spots (ImageViews) and Animators 
   private final Queue<ImageView> bricks =
      new ConcurrentLinkedQueue<ImageView>(); 
   private final Queue<Animator> animators = 
      new ConcurrentLinkedQueue<Animator>();
   private ImageView baldy, jumpButton, threeSixtyButton, currentBrick; //Imageviews that are constantly/currently on the screen
   private boolean bottomFloor = true; //Whether or not Baldylocks is on the starting ground
   private boolean isInAir = false; //Whether or not Baldylocks is jumping
   private boolean isInBigAir = false; //Whether or not Baldylocks is doing a 360 jump
   private boolean firstTimer = true; //Whether or not this is the first time the timer is being called
   private int currentImage = 0; //Whether or not the brick going by is a brick or a brick wall
   private int theOrientation; //Orientation for whether or not you are able to play buttons


   private LinearLayout livesLinearLayout; // displays lives remaining
   private RelativeLayout relativeLayout; // displays spots
   private Resources resources; // used to load resources
   private LayoutInflater layoutInflater; // used to inflate GUIs
   private OrientationEventListener currentOrientation;
   private int orientationNum;

   // time in milliseconds for spot and touched spot animations
   private static final int INITIAL_ANIMATION_DURATION = 3000;
   private static final Random random = new Random(); // for random coords
   private static final int BRICK_DELAY = 5000; // delay in milliseconds
   private Handler brickHandler; // adds new bricks to the game

   // sound IDs, constants and variables for the game's sounds
   private static final int JUMP_SOUND_ID = 1;
   private static final int OW_SOUND_ID = 2;
   private static final int SOUND_PRIORITY = 2;
   private static final int SOUND_QUALITY = 100;
   private static final int MAX_STREAMS = 4;
   private SoundPool soundPool; // plays sound effects
   private int volume; // sound effect volume
   private Map<Integer, Integer> soundMap; // maps ID to soundpool
   
   // constructs a new MainActivityView
   public MainActivityView(Context context, SharedPreferences sharedPreferences,
      RelativeLayout parentLayout)
   {
      super(context);
      
      // load the high score
      preferences = sharedPreferences;
      highScore = preferences.getInt(HIGH_SCORE, 0);

      // save Resources for loading external values
      resources = context.getResources();

      // save LayoutInflater
      layoutInflater = (LayoutInflater) context.getSystemService(
         Context.LAYOUT_INFLATER_SERVICE);

      // get references to various GUI components
      relativeLayout = parentLayout;
      livesLinearLayout = (LinearLayout) relativeLayout.findViewById(
         R.id.lifeLinearLayout);

      brickHandler = new Handler(); // used to add bricks when game starts
   } // end SpotOnView constructor

   // called by the Main Activity when it receives a call to onPause
   public void pause()
   {
      gamePaused = true;
      soundPool.release(); // release audio resources
      soundPool = null;
      cancelAnimations(); // cancel all outstanding animations
   } // end method pause

   // cancel animations and remove ImageViews representing spots
   private void cancelAnimations()
   {
      // cancel remaining animations
      for (Animator animator : animators)
         animator.cancel();

      // remove remaining bricks from the screen
      for (ImageView view : bricks)
         relativeLayout.removeView(view);

      brickHandler.removeCallbacks(addSpotRunnable);
      animators.clear();
      bricks.clear();
   } // end method cancelAnimations
   
   // called by the Main Activity when it receives a call to onResume
   public void resume(Context context)
   {
      gamePaused = false;
      initializeSoundEffects(context); // initialize app's SoundPool

      if (!dialogDisplayed)
         resetGame(); // start the game
   } // end method resume

   // start a new game
   public void resetGame()
   {
      bricks.clear(); // empty the List of spots
      animators.clear(); // empty the List of Animators
      livesLinearLayout.removeAllViews(); // clear old lives from screen
      
      animationTime = INITIAL_ANIMATION_DURATION; // init animation length
      score = 0; // reset the score
      gameIsOver = false; // the game is not over

      addBaldy(); // add Baldylocks to the game screen
      addButtons(); // add the two buttons to the game screen

       //Calculates and figures out our current location
       currentOrientation = new OrientationEventListener(this.getContext(), SensorManager.SENSOR_DELAY_NORMAL)
       {
           @Override
           public void onOrientationChanged(int orientation)
           {
               orientationNum = orientation;
           }
       };

       if(currentOrientation.canDetectOrientation())
       {
           currentOrientation.enable();
       }

       //Gives bricks delays when spawned on screen
       brickHandler.postDelayed(addSpotRunnable, BRICK_DELAY);
   } // end method resetGame

   // create the app's SoundPool for playing game audio
   private void initializeSoundEffects(Context context)
   {
      // initialize SoundPool to play the app's three sound effects
      soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC,
         SOUND_QUALITY);

      // set sound effect volume
      AudioManager manager =
         (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

      // create sound map
      soundMap = new HashMap<Integer, Integer>(); // create new HashMap

      // add each sound effect to the SoundPool
      soundMap.put(JUMP_SOUND_ID,
         soundPool.load(context, R.raw.jump, SOUND_PRIORITY));
      soundMap.put(OW_SOUND_ID,
         soundPool.load(context, R.raw.ow, SOUND_PRIORITY));
   } // end method initializeSoundEffect

   // Runnable used to add new spots to the game at the start
   private Runnable addSpotRunnable = new Runnable()
   {
      public void run()
      {
         addNewBrick(); // add a new spot to the game
      } // end method run
   }; // end Runnable

    public void addButtons() {

       //Round to either 0, 90, 180, 270 to figure out current orientation
        theOrientation = 90*Math.round(orientationNum / 90);

        //CREATE JUMP BUTTON
        jumpButton = (ImageView) layoutInflater.inflate(R.layout.untouched, null);
        jumpButton.setLayoutParams(new RelativeLayout.LayoutParams(
                100, 100));
        jumpButton.setImageResource(R.drawable.bluebutton);
        jumpButton.setX(50);
        jumpButton.setY(425);
        jumpButton.setOnClickListener( // listens for spot being clicked
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                       if(isInAir == false){

                       /* if(bottomFloor == true && theOrientation == 0)
                            baldyJump(); // handle touched spot
                        } else if(bottomFloor == false && theOrientation == 180)
                           baldyJump(); // handle touched spot */

                           baldyJump();
                       }

                    } // end method onClick
                } // end OnClickListener
        ); // end call to setOnClickListener
        relativeLayout.addView(jumpButton);


        //CREATE 360 JUMP BUTTON
        threeSixtyButton = (ImageView) layoutInflater.inflate(R.layout.untouched, null);
        threeSixtyButton.setLayoutParams(new RelativeLayout.LayoutParams(
                100, 100));
        threeSixtyButton.setImageResource(R.drawable.darkbluebutton);
        threeSixtyButton.setX(975);
        threeSixtyButton.setY(425);
        threeSixtyButton.setOnClickListener( // listens for spot being clicked
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        if(isInAir == false){


                          //COMMENTED OUT FOR TESTING ON VIRTUAL MACHINE

                          /*
                          if(bottomFloor == true && theOrientation == 0)
                                baldyThreeSixty(); // handle touched spot
                         else if(bottomFloor == false && theOrientation == 180){
                                baldyThreeSixty();
                          */

                            baldyThreeSixty();

                        }
                    } // end method onClick
                } // end OnClickListener
        ); // end call to setOnClickListener
        relativeLayout.addView(threeSixtyButton);
    }

    //Moves the buttons to either the top or bottom of screen depending on which side of screen Baldylocks is on
    public void moveButtons(){
        if(bottomFloor == true){
            jumpButton.setY(425);
            threeSixtyButton.setY(425);
            jumpButton.setY(457);
            threeSixtyButton.setY(457);
        } else {
            jumpButton.setY(32);
            threeSixtyButton.setY(32);
        }
    }

    //Intiially adds baldylocks to the screen
    public void addBaldy() {
        baldy = (ImageView) layoutInflater.inflate(R.layout.untouched, null);
        baldy.setLayoutParams(new RelativeLayout.LayoutParams(
                130, 130));
        baldy.setImageResource(R.drawable.baldylocks);
        baldy.setX(450);
        baldy.setY(297);
        relativeLayout.addView(baldy);
    }

    //Spawns a new Brick, either just a brick or a brick wall
   public void addNewBrick()
   {
      //Give our brick its initial X coordinates
      int x = 1200;
      int x2 = -200;
      int y, y2;

      // create new brick
      final ImageView brick =
         (ImageView) layoutInflater.inflate(R.layout.untouched, null);
      bricks.add(brick); // add the new spot to our list of spots

      //Random integers to spawn either bricks or brick walls
      int randomInt = random.nextInt(2);
      //Random integers to spawn on the top or the bottom
      int randomInt2 = random.nextInt(2);

        //Spawns a brickwall
       if(randomInt == 0) {
           brick.setLayoutParams(new RelativeLayout.LayoutParams(
                   100, 100));
           brick.setImageResource(R.drawable.brickwall);
           currentImage = 2;

           //Spawn on the top wall
           if(randomInt2 == 0){
               y = 130;
               y2 = 130;
           //Spawn on the bottom wall
           } else {
               y = 358;
               y2 = 358;
           }
           //Spawns just a brick
       } else {
           brick.setLayoutParams(new RelativeLayout.LayoutParams(
                   32, 40));
           brick.setImageResource(R.drawable.brick);
           currentImage = 1;

           //Spawn on the top wall
           if(randomInt2 == 0){
               y = 135;
               y2 = 135;
            //Spawn on the bottom wall
           } else {
               y = 410;
               y2 = 410;
           }
       }

      brick.setX(x); // set brick's starting x location
      brick.setY(y); // set brick's starting y location

      currentBrick = brick;

       //Creates a timer to constantly test for collisions between Baldylocks and the current brick
      if(firstTimer == true){
       TimerTask myTask = new TimerTask() {
           @Override
           public void run() {
               if(currentBrick.getX() > 400 && currentBrick.getX() < 500 && isInAir == false){

                   if(currentBrick.getY() < 150 && bottomFloor == false){
                       gameIsOver = true;
                           soundPool.play(OW_SOUND_ID, volume, volume,
                                   SOUND_PRIORITY, 0, 1f);
                   }
                   else if (currentBrick.getY() > 150 && bottomFloor == true)
                   {
                       gameIsOver = true;
                       if (soundPool != null)
                           soundPool.play(OW_SOUND_ID, volume, volume,
                                   SOUND_PRIORITY, 0, 1f);
                   }
               } else if(currentBrick.getX() > 400 && currentBrick.getX() < 500 && isInAir == true && currentImage == 2) {

                   if(isInBigAir == false){
                      gameIsOver = true;
                       if (soundPool != null)
                           soundPool.play(OW_SOUND_ID, volume, volume,
                                   SOUND_PRIORITY, 0, 1f);
                   }
               }
           }
       };
       Timer myTimer = new Timer();

       myTimer.schedule(myTask, 2, 2);
       firstTimer = false;
      }

      relativeLayout.addView(brick); // add spot to the screen

      //Move the bricks from right to left
       brick.animate().x(x2).y(y2)
         .setDuration(animationTime).setListener(
            new AnimatorListenerAdapter()
            {

               @Override
               public void onAnimationStart(Animator animation)
               {
                  animators.add(animation); // save for possible cancel
               } // end method onAnimationStart


               public void onAnimationEnd(Animator animation)
               {
                  animators.remove(animation); // animation done, remove
                  
                  if (!gamePaused && bricks.contains(brick)) // not touched
                  {
                     missedBrick(brick); // lose a life
                  } // end if
               } // end method onAnimationEnd
            } // end AnimatorListenerAdapter
         ); // end call to setListener
   } // end addNewSpot method

   //Normal jump for dodging just bricks
   private void baldyJump(){

       if (soundPool != null)
           soundPool.play(JUMP_SOUND_ID, volume, volume,
                   SOUND_PRIORITY, 0, 1f);
       isInAir = true;
       int y = 0;
       int y2 = 0;
       int x = 482;
       int x2 = 482;

       if(bottomFloor == true){
           y = 328;
           y2 = 250;
       } else {
           y = 133;
           y2 = 211;
       }

       baldy.animate().x(x2).y(y2)
               .setDuration(300).setListener(
               new AnimatorListenerAdapter()
               {
                   @Override
                   public void onAnimationStart(Animator animation)
                   {
                       animators.add(animation); // save for possible cancel
                   } // end method onAnimationStart

                   public void onAnimationEnd(Animator animation)
                   {
                       animators.remove(animation); // animation done, remove
                       if(bottomFloor == true){
                           finishJump1();
                       } else {
                           finishJump2();
                       }
                   } // end method onAnimationEnd
               } // end AnimatorListenerAdapter
       ); // end call to setListener
   }

   //Finishes the normal jump when on the bottom wall
   private void finishJump1(){
       int y = 250;
       int y2 = 328;
       int x = 482;
       int x2 = 482;

       baldy.animate().x(x2).y(y2)
               .setDuration(300).setListener(
               new AnimatorListenerAdapter()
               {
                   @Override
                   public void onAnimationStart(Animator animation)
                   {
                       animators.add(animation); // save for possible cancel
                   } // end method onAnimationStart

                   public void onAnimationEnd(Animator animation)
                   {
                       animators.remove(animation); // animation done, remove
                       isInAir = false;
                   } // end method onAnimationEnd
               } // end AnimatorListenerAdapter
       ); // end call to setListener
   }

    //Finishes the normal jump when on the top wall
    private void finishJump2(){
        int y = 211;
        int y2 = 133;
        int x = 482;
        int x2 = 482;

        baldy.animate().x(x2).y(y2)
                .setDuration(300).setListener(
                new AnimatorListenerAdapter()
                {
                    @Override
                    public void onAnimationStart(Animator animation)
                    {
                        animators.add(animation); // save for possible cancel
                    } // end method onAnimationStart

                    public void onAnimationEnd(Animator animation)
                    {
                        animators.remove(animation); // animation done, remove
                        isInAir = false;
                    } // end method onAnimationEnd
                } // end AnimatorListenerAdapter
        ); // end call to setListener
    }

    //360 Jump, Rotates baldylocks as well as moves him to the other wall
    private void baldyThreeSixty(){

        if (soundPool != null)
            soundPool.play(JUMP_SOUND_ID, volume, volume,
                    SOUND_PRIORITY, 0, 1f);

        isInAir = true;
        isInBigAir = true;
        int y = 0;
        int y2 = 0;
        int x = 482;
        int x2 = 482;
        int rotateNum = 0;

        if(bottomFloor == true){
            y = 328;
            y2 = 133;
           rotateNum = 180;
        } else {
            y = 133;
            y2 = 328;
           rotateNum = 360;
        }


        baldy.animate().x(x2).y(y2).rotation(rotateNum)
                .setDuration(600).setListener(
                new AnimatorListenerAdapter()
                {
                    @Override
                    public void onAnimationStart(Animator animation)
                    {
                        animators.add(animation); // save for possible cancel
                    } // end method onAnimationStart

                    public void onAnimationEnd(Animator animation)
                    {
                        animators.remove(animation); // animation done, remove

                        isInAir = false;
                        isInBigAir = false;
                        if(bottomFloor == true){
                        bottomFloor = false;
                        } else {
                            bottomFloor = true;
                        }
                        moveButtons();
                    } // end method onAnimationEnd
                } // end AnimatorListenerAdapter
        ); // end call to setListener
    }


   //Either spawn another brick or call gameover depending on whether it was dodged or not
   public void missedBrick(ImageView brick)
   {      
      bricks.remove(brick); // remove spot from spots List
      relativeLayout.removeView(brick); // remove spot from screen

      if(gameIsOver == true){
          gameOver();
      } else {
      score++;
      addNewBrick();
      }
   }

   //Game Over, Delete all instances and set scores, call window
   public void gameOver(){

       gameIsOver = true; // the game is over

       // if the last game's score is greater than the high score
       if (score > highScore)
       {
           SharedPreferences.Editor editor = preferences.edit();
           editor.putInt(HIGH_SCORE, score);
           editor.commit(); // store the new high score
           highScore = score;
       }

       //cancelAnimations();

       relativeLayout.removeView(baldy);
       relativeLayout.removeView(jumpButton);
       relativeLayout.removeView(threeSixtyButton);
       bottomFloor = true;
       isInAir = false;

       // display a high score dialog
       Builder dialogBuilder = new Builder(getContext());
       dialogBuilder.setTitle(R.string.game_over);
       dialogBuilder.setMessage(resources.getString(R.string.score) +
               " " + score + "\nHigh Score: " + highScore);
       dialogBuilder.setPositiveButton("Play Again?",
               new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       dialogDisplayed = false;
                       resetGame(); // start a new game
                   } // end method onClick
               } // end DialogInterface
       ); // end call to dialogBuilder.setPositiveButton
       dialogDisplayed = true;
       dialogBuilder.show(); // display the reset game dialog
   }

} // end class SpotOnView
